package com.is1.proyecto.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.javalite.activejdbc.Base;

import com.is1.proyecto.models.Acta;
import com.is1.proyecto.models.AuditoriaActa;
import com.is1.proyecto.models.Nota;
import com.is1.proyecto.models.PeriodoLectivo;

public class ActaService {

    private static final double UMBRAL_APROBACION = 5.0;
    private static final List<String> INSTANCIAS_VALIDAS = List.of(
        "parcial_1", "parcial_2", "tp",
        "recuperatorio_1", "recuperatorio_2", "examen_final"
    );

    // ── CREAR ACTA ───────────────────────────────────────────────

    public Map<String, Object> crearActa(int comisionId, int periodoId,
                                          String instancia, int personaId) {
        Map<String, Object> result = new HashMap<>();

        if (!INSTANCIAS_VALIDAS.contains(instancia)) {
            result.put("success", false);
            result.put("status", 422);
            result.put("message", "La instancia especificada no es válida.");
            return result;
        }

        PeriodoLectivo periodo = PeriodoLectivo.findById(periodoId);
        if (periodo == null || !periodo.isActivo()) {
            result.put("success", false);
            result.put("status", 422);
            result.put("message", "No hay un período académico activo. Contacte al Administrador.");
            return result;
        }

        if (!esTitularDeComision(personaId, comisionId)) {
            result.put("success", false);
            result.put("status", 403);
            result.put("message", "Solo el docente titular puede crear actas.");
            return result;
        }

        if (Acta.count("comision_id = ? AND periodo_id = ? AND instancia = ?",
                comisionId, periodoId, instancia) > 0) {
            result.put("success", false);
            result.put("status", 409);
            result.put("message", "Ya existe un acta para esta instancia en el período activo.");
            return result;
        }

        Base.openTransaction();
        try {
            Acta acta = new Acta();
            acta.setComisionId(comisionId);
            acta.setPeriodoId(periodoId);
            acta.setInstancia(instancia);
            acta.setEstado("abierta");
            acta.saveIt();

            int actaId = ((Number) acta.getLongId()).intValue();

            List<Integer> inscriptos = getEstudiantesParaInstancia(comisionId, periodoId, instancia);

            for (int estId : inscriptos) {
                Nota nota = new Nota();
                nota.setActaId(actaId);
                nota.setEstudianteId(estId);
                nota.setValor(null);
                nota.setAusente(false);
                nota.setCargadaPorDocenteId(personaId);
                nota.saveIt();
            }

            AuditoriaActa.registrar(actaId, "creacion", personaId, null, null);

            Base.commitTransaction();
            result.put("success", true);
            result.put("id", actaId);
            result.put("message", "Acta creada correctamente.");
        } catch (Exception e) {
            Base.rollbackTransaction();
            result.put("success", false);
            result.put("status", 500);
            result.put("message", "Error al crear el acta: " + e.getMessage());
        }
        return result;
    }

    // ── LISTAR ACTAS ─────────────────────────────────────────────

    public List<Map<String, Object>> listarActas(Integer comisionId, Integer periodoId,
                                                   String estado, String instancia,
                                                   int personaId, String rol) {
        StringBuilder where = new StringBuilder("WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (comisionId != null) {
            where.append(" AND a.comision_id = ?");
            params.add(comisionId);
        }
        if (periodoId != null) {
            where.append(" AND a.periodo_id = ?");
            params.add(periodoId);
        }
        if (estado != null && !estado.isEmpty()) {
            where.append(" AND a.estado = ?");
            params.add(estado);
        }
        if (instancia != null && !instancia.isEmpty()) {
            where.append(" AND a.instancia = ?");
            params.add(instancia);
        }

        if ("DOCENTE".equals(rol)) {
            where.append(
                " AND EXISTS (SELECT 1 FROM asignaciones_docentes ad2 " +
                "JOIN docentes d2 ON ad2.docente_id = d2.id " +
                "WHERE ad2.comision_id = a.comision_id AND d2.persona_id = ? AND ad2.activa = 1)"
            );
            params.add(personaId);
        }

        String sql =
            "SELECT a.id, a.comision_id, a.periodo_id, a.instancia, a.estado, " +
            "       a.fecha_cierre, a.created_at, " +
            "       c.nombre AS comision_nombre, " +
            "       m.nombre AS materia_nombre, m.codigo AS materia_codigo, " +
            "       pl.anio AS periodo_anio, pl.cuatrimestre AS periodo_cuat, " +
            "       (SELECT COUNT(*) FROM notas n WHERE n.acta_id = a.id) AS total_notas, " +
            "       (SELECT COUNT(*) FROM notas n WHERE n.acta_id = a.id AND n.valor IS NOT NULL) AS notas_cargadas " +
            "FROM actas a " +
            "JOIN comisiones c ON a.comision_id = c.id " +
            "JOIN materias m ON c.materia_id = m.id " +
            "JOIN periodos_lectivos pl ON a.periodo_id = pl.id " +
            where + " ORDER BY pl.anio DESC, pl.cuatrimestre DESC, m.nombre ASC";

        List<Map> rows = Base.findAll(sql, params.toArray());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map row : rows) {
            result.add(actaRowToMap(row));
        }
        return result;
    }

    // ── DETALLE ACTA ─────────────────────────────────────────────

    public Map<String, Object> getActaDetalle(int actaId, int personaId, List<String> roles) {
        Map<String, Object> result = new HashMap<>();

        List<Map> actaRows = Base.findAll(
            "SELECT a.id, a.comision_id, a.periodo_id, a.instancia, a.estado, " +
            "       a.fecha_cierre, a.cerrada_por_docente_id, a.reabierta_por_admin_id, " +
            "       a.fecha_reapertura, a.created_at, " +
            "       c.nombre AS comision_nombre, " +
            "       m.nombre AS materia_nombre, m.codigo AS materia_codigo, " +
            "       pl.anio AS periodo_anio, pl.cuatrimestre AS periodo_cuat " +
            "FROM actas a " +
            "JOIN comisiones c ON a.comision_id = c.id " +
            "JOIN materias m ON c.materia_id = m.id " +
            "JOIN periodos_lectivos pl ON a.periodo_id = pl.id " +
            "WHERE a.id = ?", actaId
        );

        if (actaRows.isEmpty()) {
            result.put("success", false);
            result.put("status", 404);
            result.put("message", "Acta no encontrada.");
            return result;
        }

        Map actaRow = actaRows.get(0);
        boolean esCerrada = "cerrada".equals(actaRow.get("estado"));
        boolean esEstudiante = roles.contains("ESTUDIANTE") && !roles.contains("DOCENTE")
                                && !roles.contains("ADMINISTRADOR");

        if (esEstudiante && !esCerrada) {
            result.put("success", false);
            result.put("status", 403);
            result.put("message", "Las notas no están disponibles mientras el acta esté abierta.");
            return result;
        }

        String notasSql;
        List<Object> notasParams = new ArrayList<>();

        if (esEstudiante) {
            notasSql =
                "SELECT n.id, n.estudiante_id, n.valor, n.ausente, n.updated_at, " +
                "       p.nombre AS est_nombre, p.apellido AS est_apellido, p.dni AS est_dni " +
                "FROM notas n " +
                "JOIN estudiantes e ON n.estudiante_id = e.id " +
                "JOIN personas p ON e.persona_id = p.id " +
                "WHERE n.acta_id = ? AND e.persona_id = ?";
            notasParams.add(actaId);
            notasParams.add(personaId);
        } else {
            notasSql =
                "SELECT n.id, n.estudiante_id, n.valor, n.ausente, n.updated_at, " +
                "       p.nombre AS est_nombre, p.apellido AS est_apellido, p.dni AS est_dni " +
                "FROM notas n " +
                "JOIN estudiantes e ON n.estudiante_id = e.id " +
                "JOIN personas p ON e.persona_id = p.id " +
                "WHERE n.acta_id = ? ORDER BY p.apellido ASC, p.nombre ASC";
            notasParams.add(actaId);
        }

        List<Map> notaRows = Base.findAll(notasSql, notasParams.toArray());
        List<Map<String, Object>> notas = new ArrayList<>();
        for (Map nr : notaRows) {
            Map<String, Object> nm = new HashMap<>();
            nm.put("id", nr.get("id"));
            nm.put("estudianteId", nr.get("estudiante_id"));
            nm.put("nombreCompleto", nr.get("est_apellido") + ", " + nr.get("est_nombre"));
            nm.put("dni", nr.get("est_dni"));
            Object valorRaw = nr.get("valor");
            nm.put("valor", valorRaw);
            nm.put("valorStr", valorRaw != null ? String.format("%.2f", ((Number) valorRaw).doubleValue()) : "");
            boolean ausente = toBoolean(nr.get("ausente"));
            nm.put("ausente", ausente);
            boolean sinRegistrar = valorRaw == null && !ausente;
            nm.put("sinRegistrar", sinRegistrar);
            nm.put("notaCargada", !sinRegistrar);
            if (valorRaw != null) {
                boolean aprobado = ((Number) valorRaw).doubleValue() >= UMBRAL_APROBACION;
                nm.put("aprobado", aprobado);
                nm.put("reprobado", !aprobado);
            }
            nm.put("updatedAt", nr.get("updated_at"));
            notas.add(nm);
        }

        int comisionId = ((Number) actaRow.get("comision_id")).intValue();
        boolean esTitular = esTitularDeComision(personaId, comisionId);
        boolean esJtp = esJtpDeComision(personaId, comisionId);

        Map<String, Object> acta = actaRowToMap(actaRow);
        acta.put("notas", notas);
        acta.put("esTitular", esTitular);
        acta.put("puedeCargarNotas", (esTitular || esJtp) && !esCerrada);
        acta.put("puedeCerrar", esTitular && !esCerrada);
        acta.put("puedeReabrir", roles.contains("ADMINISTRADOR") && esCerrada);

        result.put("success", true);
        result.put("acta", acta);
        return result;
    }

    // ── GUARDAR LOTE DE NOTAS ────────────────────────────────────

    public Map<String, Object> guardarLoteNotas(int actaId,
                                                  List<Map<String, Object>> notasPayload,
                                                  int personaId) {
        Map<String, Object> result = new HashMap<>();

        Acta acta = Acta.findById(actaId);
        if (acta == null) {
            result.put("success", false);
            result.put("status", 404);
            result.put("message", "Acta no encontrada.");
            return result;
        }

        if (acta.isCerrada()) {
            result.put("success", false);
            result.put("status", 403);
            result.put("message", "El acta está cerrada y no admite modificaciones.");
            return result;
        }

        int comisionId = acta.getComisionId();
        if (!esTitularDeComision(personaId, comisionId) && !esJtpDeComision(personaId, comisionId)) {
            result.put("success", false);
            result.put("status", 403);
            result.put("message", "No tiene permisos para cargar notas en esta acta.");
            return result;
        }

        Map<String, List<String>> errors = new HashMap<>();
        for (int i = 0; i < notasPayload.size(); i++) {
            Map<String, Object> np = notasPayload.get(i);
            List<String> rowErrors = new ArrayList<>();

            Object valorRaw = np.get("valor");
            Object ausenteRaw = np.get("ausente");
            boolean ausente = ausenteRaw != null && Boolean.TRUE.equals(ausenteRaw);

            if (valorRaw != null && ausente) {
                rowErrors.add("No puede registrarse valor numérico y ausente = true simultáneamente.");
            }

            if (valorRaw != null && !ausente) {
                double valor = ((Number) valorRaw).doubleValue();
                if (valor < 0.0 || valor > 10.0) {
                    rowErrors.add("El valor debe ser numérico entre 0.00 y 10.00.");
                }
            }

            Object estIdRaw = np.get("estudiante_id");
            if (estIdRaw == null) {
                rowErrors.add("El campo estudiante_id es obligatorio.");
            } else {
                int estId = ((Number) estIdRaw).intValue();
                if (Nota.count("acta_id = ? AND estudiante_id = ?", actaId, estId) == 0) {
                    rowErrors.add("El estudiante no pertenece a esta acta.");
                }
            }

            if (!rowErrors.isEmpty()) {
                errors.put("notas." + i, rowErrors);
            }
        }

        if (!errors.isEmpty()) {
            result.put("success", false);
            result.put("status", 422);
            result.put("message", "El lote de notas contiene errores. No se guardó ningún cambio.");
            result.put("errors", errors);
            return result;
        }

        Base.openTransaction();
        try {
            Timestamp ahora = new Timestamp(System.currentTimeMillis());
            for (Map<String, Object> np : notasPayload) {
                int estId = ((Number) np.get("estudiante_id")).intValue();
                Object valorRaw = np.get("valor");
                boolean ausente = Boolean.TRUE.equals(np.get("ausente"));
                Double valor = valorRaw != null ? ((Number) valorRaw).doubleValue() : null;

                List<Map> notaRows = Base.findAll(
                    "SELECT id, valor, ausente FROM notas WHERE acta_id = ? AND estudiante_id = ?",
                    actaId, estId
                );

                if (notaRows.isEmpty()) continue;

                Map notaRow = notaRows.get(0);
                Object valorAnterior = notaRow.get("valor");
                boolean ausenteAnterior = toBoolean(notaRow.get("ausente"));

                String accion = (valorAnterior == null && !ausenteAnterior) ? "carga_nota" : "modificacion_nota";
                String detalle = null;
                if ("modificacion_nota".equals(accion)) {
                    detalle = "{\"anterior\":" + (valorAnterior != null ? valorAnterior : "null") +
                              ",\"anterior_ausente\":" + ausenteAnterior +
                              ",\"nuevo\":" + (valor != null ? valor : "null") +
                              ",\"nuevo_ausente\":" + ausente + "}";
                }

                Base.exec(
                    "UPDATE notas SET valor = ?, ausente = ?, ultima_modificacion_por = ?, updated_at = ? " +
                    "WHERE acta_id = ? AND estudiante_id = ?",
                    valor, ausente ? 1 : 0, personaId, ahora, actaId, estId
                );

                AuditoriaActa.registrar(actaId, accion, personaId, detalle, null);
            }

            Base.commitTransaction();
            result.put("success", true);
            result.put("message", "Notas guardadas correctamente.");
        } catch (Exception e) {
            Base.rollbackTransaction();
            result.put("success", false);
            result.put("status", 500);
            result.put("message", "Error al guardar notas: " + e.getMessage());
        }
        return result;
    }

    // ── CERRAR ACTA ──────────────────────────────────────────────

    public Map<String, Object> cerrarActa(int actaId, int personaId) {
        Map<String, Object> result = new HashMap<>();

        Acta acta = Acta.findById(actaId);
        if (acta == null) {
            result.put("success", false);
            result.put("status", 404);
            result.put("message", "Acta no encontrada.");
            return result;
        }

        if (acta.isCerrada()) {
            result.put("success", false);
            result.put("status", 422);
            result.put("message", "El acta ya está cerrada.");
            return result;
        }

        if (!esTitularDeComision(personaId, acta.getComisionId())) {
            result.put("success", false);
            result.put("status", 403);
            result.put("message", "Solo el docente titular puede oficializar el acta.");
            return result;
        }

        Base.openTransaction();
        try {
            Timestamp ahora = new Timestamp(System.currentTimeMillis());

            Base.exec(
                "UPDATE actas SET estado = 'cerrada', fecha_cierre = ?, " +
                "cerrada_por_docente_id = ?, updated_at = ? WHERE id = ?",
                ahora, personaId, ahora, actaId
            );

            // Recalcular condición global de cada estudiante del acta
            List<Map> estudiantesActa = Base.findAll(
                "SELECT DISTINCT n.estudiante_id FROM notas n WHERE n.acta_id = ?", actaId
            );

            for (Map er : estudiantesActa) {
                int estId = ((Number) er.get("estudiante_id")).intValue();
                String condicion = calcularCondicionGlobal(acta.getComisionId(), acta.getPeriodoId(), estId);
                if (!"ACTIVA".equals(condicion)) {
                    Base.exec(
                        "UPDATE inscripciones_comisiones SET estado = ? " +
                        "WHERE comision_id = ? AND estudiante_id = ?",
                        condicion, acta.getComisionId(), estId
                    );
                }
            }

            AuditoriaActa.registrar(actaId, "cierre", personaId, null, null);

            Base.commitTransaction();
            result.put("success", true);
            result.put("message", "Acta cerrada correctamente.");
        } catch (Exception e) {
            Base.rollbackTransaction();
            result.put("success", false);
            result.put("status", 500);
            result.put("message", "Error al cerrar el acta. La operación fue revertida: " + e.getMessage());
        }
        return result;
    }

    // ── REABRIR ACTA ─────────────────────────────────────────────

    public Map<String, Object> reabrirActa(int actaId, int personaId,
                                             List<String> roles, String motivo) {
        Map<String, Object> result = new HashMap<>();

        if (!roles.contains("ADMINISTRADOR")) {
            result.put("success", false);
            result.put("status", 403);
            result.put("message", "Solo el Administrador puede reabrir actas.");
            return result;
        }

        if (motivo == null || motivo.trim().isEmpty()) {
            result.put("success", false);
            result.put("status", 422);
            result.put("message", "El motivo de reapertura es obligatorio.");
            return result;
        }

        Acta acta = Acta.findById(actaId);
        if (acta == null) {
            result.put("success", false);
            result.put("status", 404);
            result.put("message", "Acta no encontrada.");
            return result;
        }

        if (acta.isAbierta()) {
            result.put("success", false);
            result.put("status", 422);
            result.put("message", "El acta ya está abierta.");
            return result;
        }

        Timestamp ahora = new Timestamp(System.currentTimeMillis());

        Base.exec(
            "UPDATE actas SET estado = 'abierta', reabierta_por_admin_id = ?, " +
            "fecha_reapertura = ?, updated_at = ? WHERE id = ?",
            personaId, ahora, ahora, actaId
        );

        List<Map> inscripcionRows = Base.findAll(
            "SELECT ic.id FROM notas n " +
            "JOIN inscripciones_comisiones ic ON ic.estudiante_id = n.estudiante_id " +
            "  AND ic.comision_id = ? " +
            "WHERE n.acta_id = ? AND ic.estado IN ('APROBADO','REPROBADO')",
            acta.getComisionId(), actaId
        );

        for (Map row : inscripcionRows) {
            int icId = ((Number) row.get("id")).intValue();
            Base.exec("UPDATE inscripciones_comisiones SET estado = 'ACTIVA' WHERE id = ?", icId);
        }

        AuditoriaActa.registrar(actaId, "reapertura", personaId, null, motivo.trim());

        result.put("success", true);
        result.put("message", "Acta reabierta correctamente.");
        return result;
    }

    // ── AUDITORÍA ────────────────────────────────────────────────

    public Map<String, Object> getAuditoria(int actaId, int personaId, List<String> roles) {
        Map<String, Object> result = new HashMap<>();

        if (!roles.contains("ADMINISTRADOR")) {
            result.put("success", false);
            result.put("status", 403);
            result.put("message", "Solo el Administrador puede consultar la auditoría.");
            return result;
        }

        List<Map> rows = Base.findAll(
            "SELECT aa.id, aa.accion, aa.detalle, aa.motivo, aa.created_at, " +
            "       p.nombre AS realizado_nombre, p.apellido AS realizado_apellido " +
            "FROM auditoria_acta aa " +
            "JOIN personas p ON aa.realizado_por = p.id " +
            "WHERE aa.acta_id = ? ORDER BY aa.created_at DESC",
            actaId
        );

        List<Map<String, Object>> entries = new ArrayList<>();
        for (Map row : rows) {
            Map<String, Object> e = new HashMap<>();
            e.put("id", row.get("id"));
            e.put("accion", row.get("accion"));
            e.put("detalle", row.get("detalle"));
            e.put("motivo", row.get("motivo"));
            e.put("createdAt", row.get("created_at"));
            e.put("realizadoPor", row.get("realizado_apellido") + ", " + row.get("realizado_nombre"));
            entries.add(e);
        }

        result.put("success", true);
        result.put("auditoria", entries);
        return result;
    }

    // ── HELPERS PARA VISTAS WEB ──────────────────────────────────

    public List<Map<String, Object>> getComisionesParaSelector(int personaId, String rol) {
        String sql;
        if ("ADMINISTRADOR".equals(rol)) {
            sql = "SELECT c.id, c.nombre, m.codigo AS materia_codigo " +
                  "FROM comisiones c JOIN materias m ON c.materia_id = m.id " +
                  "WHERE c.estado = 'ACTIVA' ORDER BY m.nombre ASC";
            List<Map> rows = Base.findAll(sql);
            return mapComisionRows(rows);
        } else {
            sql = "SELECT c.id, c.nombre, m.codigo AS materia_codigo " +
                  "FROM comisiones c " +
                  "JOIN materias m ON c.materia_id = m.id " +
                  "JOIN asignaciones_docentes ad ON ad.comision_id = c.id " +
                  "JOIN docentes d ON ad.docente_id = d.id " +
                  "WHERE d.persona_id = ? AND ad.activa = 1 AND c.estado = 'ACTIVA' " +
                  "ORDER BY m.nombre ASC";
            List<Map> rows = Base.findAll(sql, personaId);
            return mapComisionRows(rows);
        }
    }

    public List<Map<String, Object>> getPeriodosParaSelector() {
        List<Map> rows = Base.findAll(
            "SELECT id, anio, cuatrimestre, activo FROM periodos_lectivos ORDER BY anio DESC, cuatrimestre DESC"
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map row : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", row.get("id"));
            int c = ((Number) row.get("cuatrimestre")).intValue();
            m.put("label", row.get("anio") + " — " + (c == 1 ? "1er Cuatrimestre" : "2do Cuatrimestre"));
            m.put("activo", toBoolean(row.get("activo")));
            result.add(m);
        }
        return result;
    }

    public List<Map<String, Object>> getInstanciasParaSelector() {
        List<Map<String, Object>> list = new ArrayList<>();
        String[][] items = {
            {"parcial_1", "Parcial 1"},
            {"parcial_2", "Parcial 2"},
            {"tp", "Trabajo Práctico"},
            {"recuperatorio_1", "Recuperatorio 1"},
            {"recuperatorio_2", "Recuperatorio 2"},
            {"examen_final", "Examen Final"}
        };
        for (String[] item : items) {
            Map<String, Object> m = new HashMap<>();
            m.put("valor", item[0]);
            m.put("label", item[1]);
            list.add(m);
        }
        return list;
    }

    public static String instanciaLabel(String instancia) {
        if (instancia == null) return "";
        switch (instancia) {
            case "parcial_1": return "Parcial 1";
            case "parcial_2": return "Parcial 2";
            case "tp": return "Trabajo Práctico";
            case "recuperatorio_1": return "Recuperatorio 1";
            case "recuperatorio_2": return "Recuperatorio 2";
            case "examen_final": return "Examen Final";
            default: return instancia;
        }
    }

    // ── HELPERS INTERNOS ─────────────────────────────────────────

    private boolean esTitularDeComision(int personaId, int comisionId) {
        List<Map> rows = Base.findAll(
            "SELECT 1 FROM asignaciones_docentes ad " +
            "JOIN docentes d ON ad.docente_id = d.id " +
            "WHERE d.persona_id = ? AND ad.comision_id = ? AND ad.cargo = 'TITULAR' AND ad.activa = 1",
            personaId, comisionId
        );
        return !rows.isEmpty();
    }

    private boolean esJtpDeComision(int personaId, int comisionId) {
        List<Map> rows = Base.findAll(
            "SELECT 1 FROM asignaciones_docentes ad " +
            "JOIN docentes d ON ad.docente_id = d.id " +
            "WHERE d.persona_id = ? AND ad.comision_id = ? AND ad.cargo = 'JTP' AND ad.activa = 1",
            personaId, comisionId
        );
        return !rows.isEmpty();
    }

    private boolean toBoolean(Object val) {
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        return ((Number) val).intValue() == 1;
    }

    private Map<String, Object> actaRowToMap(Map row) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", row.get("id"));
        m.put("comisionId", row.get("comision_id"));
        m.put("periodoId", row.get("periodo_id"));
        m.put("instancia", row.get("instancia"));
        m.put("instanciaLabel", instanciaLabel((String) row.get("instancia")));
        m.put("estado", row.get("estado"));
        m.put("abierta", "abierta".equals(row.get("estado")));
        m.put("cerrada", "cerrada".equals(row.get("estado")));
        m.put("fechaCierre", row.get("fecha_cierre"));
        m.put("createdAt", row.get("created_at"));
        if (row.containsKey("comision_nombre")) m.put("comisionNombre", row.get("comision_nombre"));
        if (row.containsKey("materia_nombre")) m.put("materiaNombre", row.get("materia_nombre"));
        if (row.containsKey("materia_codigo")) m.put("materiaCodigo", row.get("materia_codigo"));
        if (row.containsKey("periodo_anio")) {
            int cuat = ((Number) row.get("periodo_cuat")).intValue();
            m.put("periodoLabel", row.get("periodo_anio") + " — " + (cuat == 1 ? "1er Cuatrimestre" : "2do Cuatrimestre"));
        }
        if (row.containsKey("total_notas")) m.put("totalNotas", row.get("total_notas"));
        if (row.containsKey("notas_cargadas")) m.put("notasCargadas", row.get("notas_cargadas"));
        return m;
    }

    private List<Map<String, Object>> mapComisionRows(List<Map> rows) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map row : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", row.get("id"));
            m.put("nombre", row.get("nombre"));
            m.put("codigo", row.get("materia_codigo"));
            result.add(m);
        }
        return result;
    }

    // ── SELECCIÓN DE ESTUDIANTES POR INSTANCIA ───────────────────

    /**
     * Determina qué estudiantes deben aparecer en el acta según la instancia.
     *
     * Parcial 1/2, TP  → todos los inscriptos (no retirados).
     * Recuperatorio 1  → quienes no aprobaron parcial_1 (o no estaban).
     * Recuperatorio 2  → quienes no aprobaron el bloque 2 (promedio tp+parcial_2).
     * Examen final     → quienes aprobaron bloque 1 Y bloque 2.
     */
    private List<Integer> getEstudiantesParaInstancia(int comisionId, int periodoId, String instancia) {
        switch (instancia) {
            case "parcial_1":
            case "parcial_2":
            case "tp":
                return todosInscriptos(comisionId);

            case "recuperatorio_1":
                return noAprobaronInstancia(comisionId, periodoId, "parcial_1");

            case "recuperatorio_2":
                return noAprobaronBloque2(comisionId, periodoId);

            case "examen_final":
                return aprobaronAmbosBloques(comisionId, periodoId);

            default:
                return todosInscriptos(comisionId);
        }
    }

    /** Todos los estudiantes inscritos y no retirados en la comisión. */
    private List<Integer> todosInscriptos(int comisionId) {
        List<Map> rows = Base.findAll(
            "SELECT e.id FROM inscripciones_comisiones ic " +
            "JOIN estudiantes e ON ic.estudiante_id = e.id " +
            "WHERE ic.comision_id = ? AND ic.estado != 'RETIRADO'",
            comisionId
        );
        List<Integer> ids = new ArrayList<>();
        for (Map r : rows) ids.add(((Number) r.get("id")).intValue());
        return ids;
    }

    /**
     * Estudiantes que NO aprobaron la instancia indicada:
     * - fallaron (valor < 5) o estaban ausentes en esa acta, o
     * - estaban inscritos pero no aparecen en esa acta.
     * Si el acta aún no existe, devuelve todos los inscriptos.
     */
    private List<Integer> noAprobaronInstancia(int comisionId, int periodoId, String instancia) {
        List<Map> actaRows = Base.findAll(
            "SELECT id FROM actas WHERE comision_id = ? AND periodo_id = ? AND instancia = ?",
            comisionId, periodoId, instancia
        );
        if (actaRows.isEmpty()) return todosInscriptos(comisionId);

        int actaId = ((Number) actaRows.get(0).get("id")).intValue();

        // Estudiantes en esa acta que NO aprobaron
        List<Map> failRows = Base.findAll(
            "SELECT estudiante_id FROM notas " +
            "WHERE acta_id = ? AND (valor IS NULL OR valor < ? OR ausente = 1)",
            actaId, UMBRAL_APROBACION
        );
        Set<Integer> result = new HashSet<>();
        for (Map r : failRows) result.add(((Number) r.get("estudiante_id")).intValue());

        // Inscriptos que no tenían fila en esa acta (faltaron antes de crearla)
        Set<Integer> enActa = new HashSet<>();
        List<Map> enActaRows = Base.findAll("SELECT estudiante_id FROM notas WHERE acta_id = ?", actaId);
        for (Map r : enActaRows) enActa.add(((Number) r.get("estudiante_id")).intValue());

        for (int id : todosInscriptos(comisionId)) {
            if (!enActa.contains(id)) result.add(id);
        }

        return new ArrayList<>(result);
    }

    /**
     * Estudiantes que NO aprobaron el bloque 2.
     * Nota bloque 2 = promedio(nota_tp, nota_parcial_2) si existe TP,
     *                 nota_parcial_2 si no hay TP.
     * El estudiante va a recuperatorio_2 si su nota bloque2 < 5, es ausente, o no tiene nota.
     */
    private List<Integer> noAprobaronBloque2(int comisionId, int periodoId) {
        Map<Integer, Double> notasP2 = getNotasPorInstancia(comisionId, periodoId, "parcial_2");
        Map<Integer, Double> notasTP = getNotasPorInstancia(comisionId, periodoId, "tp");

        if (notasP2.isEmpty()) return todosInscriptos(comisionId);

        Set<Integer> result = new HashSet<>();
        for (Map.Entry<Integer, Double> e : notasP2.entrySet()) {
            int estId = e.getKey();
            Double p2 = e.getValue();    // null = sin nota, -1 = ausente
            Double tp = notasTP.get(estId);

            double notaBloque2 = calcularNotaBloque2(tp, p2);
            if (notaBloque2 < UMBRAL_APROBACION) result.add(estId);
        }

        // Inscritos no presentes en parcial_2
        Set<Integer> enP2 = notasP2.keySet();
        for (int id : todosInscriptos(comisionId)) {
            if (!enP2.contains(id)) result.add(id);
        }
        return new ArrayList<>(result);
    }

    /**
     * Estudiantes que aprobaron bloque 1 Y bloque 2.
     * Bloque 1: nota_parcial_1 >= 5 OR nota_recuperatorio_1 >= 5
     * Bloque 2: calcularNotaBloque2(tp, max(parcial_2, recuperatorio_2)) >= 5
     */
    private List<Integer> aprobaronAmbosBloques(int comisionId, int periodoId) {
        Map<Integer, Double> p1  = getNotasPorInstancia(comisionId, periodoId, "parcial_1");
        Map<Integer, Double> r1  = getNotasPorInstancia(comisionId, periodoId, "recuperatorio_1");
        Map<Integer, Double> p2  = getNotasPorInstancia(comisionId, periodoId, "parcial_2");
        Map<Integer, Double> r2  = getNotasPorInstancia(comisionId, periodoId, "recuperatorio_2");
        Map<Integer, Double> tp  = getNotasPorInstancia(comisionId, periodoId, "tp");

        Set<Integer> todos = new HashSet<>(todosInscriptos(comisionId));
        Set<Integer> result = new HashSet<>();

        for (int estId : todos) {
            // Bloque 1
            boolean aprobB1 = aprobadoEnAlguna(estId, p1, r1);

            // Bloque 2: mejor entre parcial_2 y recuperatorio_2 combinada con TP
            Double notaP2 = p2.get(estId);
            Double notaR2 = r2.get(estId);
            Double notaTP = tp.get(estId);
            Double mejorP2 = mejorNota(notaP2, notaR2);
            double notaB2 = calcularNotaBloque2(notaTP, mejorP2);
            boolean aprobB2 = notaB2 >= UMBRAL_APROBACION;

            if (aprobB1 && aprobB2) result.add(estId);
        }
        return new ArrayList<>(result);
    }

    // ── CÁLCULO DE CONDICIÓN GLOBAL ──────────────────────────────

    /**
     * Recalcula la condición académica global de un estudiante considerando
     * todas las actas cerradas de la comisión.
     *
     * APROBADO  = aprobó bloque1 + bloque2 (regular promovido) O aprobó examen_final.
     * REPROBADO = reprobó examen_final.
     * ACTIVA    = aún en curso (no hay decisión definitiva).
     */
    private String calcularCondicionGlobal(int comisionId, int periodoId, int estId) {
        Map<Integer, Double> p1 = getNotasPorInstancia(comisionId, periodoId, "parcial_1");
        Map<Integer, Double> r1 = getNotasPorInstancia(comisionId, periodoId, "recuperatorio_1");
        Map<Integer, Double> p2 = getNotasPorInstancia(comisionId, periodoId, "parcial_2");
        Map<Integer, Double> r2 = getNotasPorInstancia(comisionId, periodoId, "recuperatorio_2");
        Map<Integer, Double> tp = getNotasPorInstancia(comisionId, periodoId, "tp");
        Map<Integer, Double> ef = getNotasPorInstancia(comisionId, periodoId, "examen_final");

        // Si tiene examen_final cerrado: es la decisión final
        Double notaEF = ef.get(estId);
        if (notaEF != null) {
            return notaEF >= UMBRAL_APROBACION ? "APROBADO" : "REPROBADO";
        }

        // Bloque 1
        boolean aprobB1 = aprobadoEnAlguna(estId, p1, r1);

        // Bloque 2 (mejor intento con promedio TP)
        Double notaTP   = tp.get(estId);
        Double mejorP2  = mejorNota(p2.get(estId), r2.get(estId));
        boolean aprobB2 = calcularNotaBloque2(notaTP, mejorP2) >= UMBRAL_APROBACION;

        if (aprobB1 && aprobB2) return "APROBADO";  // Promovido sin final

        return "ACTIVA"; // Aún en curso
    }

    // ── HELPERS DE NOTAS ─────────────────────────────────────────

    /**
     * Devuelve un mapa estudianteId → nota para una instancia y comisión+período dados.
     * Solo considera actas CERRADAS.
     * Valor especial: -1.0 indica "ausente" (cuenta como reprobado).
     * null = estudiante no presente en esa acta.
     */
    private Map<Integer, Double> getNotasPorInstancia(int comisionId, int periodoId, String instancia) {
        Map<Integer, Double> result = new HashMap<>();
        List<Map> rows = Base.findAll(
            "SELECT n.estudiante_id, n.valor, n.ausente " +
            "FROM notas n " +
            "JOIN actas a ON n.acta_id = a.id " +
            "WHERE a.comision_id = ? AND a.periodo_id = ? " +
            "  AND a.instancia = ? AND a.estado = 'cerrada'",
            comisionId, periodoId, instancia
        );
        for (Map r : rows) {
            int estId = ((Number) r.get("estudiante_id")).intValue();
            boolean ausente = toBoolean(r.get("ausente"));
            Object v = r.get("valor");
            double nota = ausente ? -1.0 : (v != null ? ((Number) v).doubleValue() : 0.0);
            result.put(estId, nota);
        }
        return result;
    }

    /** nota efectiva de bloque 2 = promedio(tp, p2) si tp existe, si no = p2. */
    private double calcularNotaBloque2(Double notaTP, Double notaP2) {
        if (notaP2 == null) return 0.0;          // sin nota = 0
        if (notaP2 < 0) return 0.0;              // ausente
        if (notaTP == null || notaTP < 0) return notaP2;  // sin TP o ausente en TP
        return (notaTP + notaP2) / 2.0;
    }

    /** Mejor nota entre dos (ignorando ausentes/-1 y nulls). */
    private Double mejorNota(Double a, Double b) {
        if (a == null || a < 0) return b;
        if (b == null || b < 0) return a;
        return Math.max(a, b);
    }

    /** True si el estudiante aprobó en alguno de los dos mapas de notas. */
    private boolean aprobadoEnAlguna(int estId, Map<Integer, Double> m1, Map<Integer, Double> m2) {
        Double n1 = m1.get(estId);
        Double n2 = m2.get(estId);
        boolean ok1 = n1 != null && n1 >= UMBRAL_APROBACION;
        boolean ok2 = n2 != null && n2 >= UMBRAL_APROBACION;
        return ok1 || ok2;
    }
}
