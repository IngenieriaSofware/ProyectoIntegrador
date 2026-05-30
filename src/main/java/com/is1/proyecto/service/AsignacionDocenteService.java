package com.is1.proyecto.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javalite.activejdbc.Base;

import com.is1.proyecto.models.AsignacionDocente;
import com.is1.proyecto.models.Comision;
import com.is1.proyecto.models.PeriodoLectivo;

public class AsignacionDocenteService {

    // ── BÚSQUEDA DE DOCENTES ─────────────────────────────────────

    public List<Map<String, Object>> buscarDocentes(String q) {
        if (q == null || q.trim().isEmpty()) return List.of();
        String like = "%" + q.trim().toLowerCase() + "%";

        List<Map> rows = Base.findAll(
            "SELECT d.id AS docente_id, p.nombre, p.apellido, p.dni, p.email " +
            "FROM docentes d " +
            "JOIN personas p ON d.persona_id = p.id " +
            "WHERE p.enabled = 1 " +
            "  AND EXISTS (SELECT 1 FROM persona_roles pr WHERE pr.persona_id = p.id AND pr.rol = 'DOCENTE') " +
            "  AND (LOWER(p.nombre) LIKE ? OR LOWER(p.apellido) LIKE ? OR p.dni LIKE ?) " +
            "ORDER BY p.apellido ASC, p.nombre ASC",
            like, like, like
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map row : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("docenteId", row.get("docente_id"));
            m.put("nombre", row.get("nombre"));
            m.put("apellido", row.get("apellido"));
            m.put("dni", row.get("dni"));
            m.put("email", row.get("email"));
            m.put("nombreCompleto", row.get("apellido") + ", " + row.get("nombre") + " — DNI: " + row.get("dni"));
            result.add(m);
        }
        return result;
    }

    // ── ALTA ─────────────────────────────────────────────────────

    public Map<String, Object> asignarDocente(int comisionId, int docenteId, String cargo, int adminId) {
        Map<String, Object> result = new HashMap<>();

        // RN-01: período activo requerido (verificado antes en controller, pero lo reforzamos)
        Comision comision = Comision.findById(comisionId);
        if (comision == null) {
            result.put("success", false);
            result.put("message", "Comisión no encontrada.");
            return result;
        }

        // RN-02: comision debe pertenecer al período activo
        PeriodoLectivo periodo = PeriodoLectivo.findById(comision.getPeriodoId());
        if (periodo == null || !periodo.isActivo()) {
            result.put("success", false);
            result.put("message", "Solo se pueden hacer asignaciones en comisiones del período lectivo activo.");
            return result;
        }

        // RN-04: cargo válido
        if (!isCargovalido(cargo)) {
            result.put("success", false);
            result.put("message", "Cargo inválido. Valores permitidos: TITULAR, JTP, AYUDANTE.");
            return result;
        }

        // RN-07: sin duplicados (docente ya asignado a esta comision)
        List<Map> existing = Base.findAll(
            "SELECT ad.cargo, p.nombre, p.apellido FROM asignaciones_docentes ad " +
            "JOIN docentes d ON ad.docente_id = d.id " +
            "JOIN personas p ON d.persona_id = p.id " +
            "WHERE ad.comision_id = ? AND ad.docente_id = ? AND ad.activa = 1",
            comisionId, docenteId
        );
        if (!existing.isEmpty()) {
            Map ex = existing.get(0);
            result.put("success", false);
            result.put("message", "El docente " + ex.get("nombre") + " " + ex.get("apellido") +
                " ya está asignado a esta comisión con cargo " + ComisionService.cargoLabel((String) ex.get("cargo")) + ".");
            return result;
        }

        // RN-05: máximo un TITULAR por comisión
        if ("TITULAR".equals(cargo)) {
            List<Map> titularRows = Base.findAll(
                "SELECT p.nombre, p.apellido FROM asignaciones_docentes ad " +
                "JOIN docentes d ON ad.docente_id = d.id " +
                "JOIN personas p ON d.persona_id = p.id " +
                "WHERE ad.comision_id = ? AND ad.cargo = 'TITULAR' AND ad.activa = 1",
                comisionId
            );
            if (!titularRows.isEmpty()) {
                Map t = titularRows.get(0);
                result.put("success", false);
                result.put("message", "La comisión ya tiene un Titular: " + t.get("nombre") + " " + t.get("apellido") +
                    ". Solo puede haber un Titular por comisión.");
                return result;
            }
        }

        AsignacionDocente ad = new AsignacionDocente();
        ad.setComisionId(comisionId);
        ad.setDocenteId(docenteId);
        ad.setCargo(cargo);
        ad.setAsignadoPor(adminId);
        ad.setActiva(true);
        ad.saveIt();

        result.put("success", true);
        result.put("id", ad.getLongId());
        result.put("message", "Docente asignado correctamente con cargo " + ComisionService.cargoLabel(cargo) + ".");
        return result;
    }

    // ── CAMBIO DE CARGO ──────────────────────────────────────────

    public Map<String, Object> cambiarCargo(int asignacionId, String nuevoCargo, int adminId) {
        Map<String, Object> result = new HashMap<>();

        AsignacionDocente ad = AsignacionDocente.findById(asignacionId);
        if (ad == null || !ad.isActiva()) {
            result.put("success", false);
            result.put("message", "Asignación no encontrada o ya inactiva.");
            return result;
        }

        if (!isCargovalido(nuevoCargo)) {
            result.put("success", false);
            result.put("message", "Cargo inválido.");
            return result;
        }

        String cargoAnterior = ad.getCargo();
        if (cargoAnterior.equals(nuevoCargo)) {
            result.put("success", false);
            result.put("message", "El cargo seleccionado es el mismo que el actual.");
            return result;
        }

        // RN-05 al cambiar a TITULAR
        if ("TITULAR".equals(nuevoCargo)) {
            List<Map> titularRows = Base.findAll(
                "SELECT p.nombre, p.apellido FROM asignaciones_docentes ad2 " +
                "JOIN docentes d ON ad2.docente_id = d.id " +
                "JOIN personas p ON d.persona_id = p.id " +
                "WHERE ad2.comision_id = ? AND ad2.cargo = 'TITULAR' AND ad2.activa = 1 AND ad2.id != ?",
                ad.getComisionId(), asignacionId
            );
            if (!titularRows.isEmpty()) {
                Map t = titularRows.get(0);
                result.put("success", false);
                result.put("message", "Ya existe un Titular en la comisión: " + t.get("nombre") + " " + t.get("apellido") +
                    ". Cambie su cargo primero.");
                return result;
            }
        }

        ad.setCargo(nuevoCargo);
        ad.saveIt();

        result.put("success", true);
        result.put("cargoAnterior", cargoAnterior);
        result.put("cargoNuevo", nuevoCargo);
        result.put("comisionId", ad.getComisionId());
        result.put("message", "Cargo actualizado de " + ComisionService.cargoLabel(cargoAnterior) +
            " a " + ComisionService.cargoLabel(nuevoCargo) + ".");
        return result;
    }

    // ── BAJA ─────────────────────────────────────────────────────

    public Map<String, Object> bajaAsignacion(int asignacionId, int adminId) {
        Map<String, Object> result = new HashMap<>();

        AsignacionDocente ad = AsignacionDocente.findById(asignacionId);
        if (ad == null || !ad.isActiva()) {
            result.put("success", false);
            result.put("message", "Asignación no encontrada o ya inactiva.");
            return result;
        }

        // RN-12: bloquear si la comisión está cerrada
        Comision comision = Comision.findById(ad.getComisionId());
        if (comision == null || !comision.isActiva()) {
            result.put("success", false);
            result.put("message", "No es posible eliminar la asignación porque el acta de esta comisión ya fue cerrada.");
            return result;
        }

        // RN-11: verificar notas (stub — tabla notas implementada en D.2)
        // Por ahora siempre sin notas. Cuando D.2 exista:
        // SELECT COUNT(*) FROM notas WHERE asignacion_docente_id = ? AND...
        boolean tieneNotas = false;

        ad.setActiva(false);
        ad.saveIt();

        result.put("success", true);
        result.put("tieneNotas", tieneNotas);
        result.put("comisionId", ad.getComisionId());
        result.put("message", "Asignación eliminada correctamente.");
        return result;
    }

    // ── MIS COMISIONES (DOCENTE) ─────────────────────────────────

    public List<Map<String, Object>> getMisComisiones(int personaId) {
        // Flujo 5.4: solo comisiones del período activo
        List<Map> rows = Base.findAll(
            "SELECT ad.id, ad.cargo, c.id AS comision_id, c.nombre AS comision_nombre, " +
            "       c.turno, c.estado, " +
            "       m.nombre AS materia_nombre, m.codigo AS materia_codigo, " +
            "       pe.anio_vigencia, " +
            "       pl.anio AS periodo_anio, pl.cuatrimestre AS periodo_cuat " +
            "FROM asignaciones_docentes ad " +
            "JOIN comisiones c ON ad.comision_id = c.id " +
            "JOIN materias m ON c.materia_id = m.id " +
            "JOIN planes_estudio pe ON c.plan_estudio_id = pe.id " +
            "JOIN periodos_lectivos pl ON c.periodo_id = pl.id " +
            "JOIN docentes d ON ad.docente_id = d.id " +
            "WHERE d.persona_id = ? AND ad.activa = 1 AND pl.activo = 1 " +
            "ORDER BY m.nombre ASC",
            personaId
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map row : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", row.get("id"));
            m.put("comisionId", row.get("comision_id"));
            m.put("comisionNombre", row.get("comision_nombre"));
            m.put("turnoLabel", ComisionService.turnoLabel((String) row.get("turno")));
            m.put("estado", row.get("estado"));
            m.put("cargo", row.get("cargo"));
            m.put("cargoLabel", ComisionService.cargoLabel((String) row.get("cargo")));
            m.put("materiaNombre", row.get("materia_nombre"));
            m.put("materiaCodigo", row.get("materia_codigo"));
            m.put("anioVigencia", row.get("anio_vigencia"));
            int c = ((Number) row.get("periodo_cuat")).intValue();
            m.put("periodoLabel", row.get("periodo_anio") + " — " + (c == 1 ? "1er Cuatrimestre" : "2do Cuatrimestre"));
            // Permisos derivados del cargo (RN-09)
            String cargo = (String) row.get("cargo");
            m.put("puedeCargarNotas", "TITULAR".equals(cargo) || "JTP".equals(cargo));
            m.put("puedeCerrarActa", "TITULAR".equals(cargo));
            result.add(m);
        }
        return result;
    }

    // ── HELPERS ──────────────────────────────────────────────────

    private boolean isCargovalido(String cargo) {
        return "TITULAR".equals(cargo) || "JTP".equals(cargo) || "AYUDANTE".equals(cargo);
    }
}
