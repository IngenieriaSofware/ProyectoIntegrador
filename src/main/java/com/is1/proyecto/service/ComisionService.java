package com.is1.proyecto.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javalite.activejdbc.Base;

import com.is1.proyecto.models.Comision;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.PeriodoLectivo;
import com.is1.proyecto.models.PlanDeEstudio;

public class ComisionService {

    private static final int PAGE_SIZE = 20;

    public Map<String, Object> listarComisiones(int page, Integer periodoId) {
        int offset = (page - 1) * PAGE_SIZE;

        StringBuilder where = new StringBuilder("WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (periodoId != null) {
            where.append(" AND c.periodo_id = ?");
            params.add(periodoId);
        }

        String countSql = "SELECT COUNT(*) as total FROM comisiones c " + where;
        List<Map> countRow = Base.findAll(countSql, params.toArray());
        int total = ((Number) countRow.get(0).get("total")).intValue();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));

        List<Object> dp = new ArrayList<>(params);
        dp.add(PAGE_SIZE);
        dp.add(offset);

        String dataSql =
            "SELECT c.id, c.turno, c.nombre, c.estado, " +
            "       m.nombre AS materia_nombre, m.codigo AS materia_codigo, " +
            "       pe.anio_vigencia, " +
            "       pl.anio AS periodo_anio, pl.cuatrimestre AS periodo_cuat, " +
            "       (SELECT COUNT(*) FROM asignaciones_docentes ad WHERE ad.comision_id = c.id AND ad.activa = 1) AS docentes_count " +
            "FROM comisiones c " +
            "JOIN materias m ON c.materia_id = m.id " +
            "JOIN planes_estudio pe ON c.plan_estudio_id = pe.id " +
            "JOIN periodos_lectivos pl ON c.periodo_id = pl.id " +
            where + " ORDER BY pl.anio DESC, pl.cuatrimestre DESC, m.nombre ASC LIMIT ? OFFSET ?";

        List<Map> rows = Base.findAll(dataSql, dp.toArray());

        List<Map<String, Object>> comisiones = new ArrayList<>();
        for (Map row : rows) {
            comisiones.add(rowToMap(row));
        }

        List<Map<String, Object>> pages = buildPages(page, totalPages);

        Map<String, Object> result = new HashMap<>();
        result.put("comisiones", comisiones);
        result.put("pages", pages);
        result.put("currentPage", page);
        result.put("hasPrevious", page > 1);
        result.put("hasNext", page < totalPages);
        result.put("previousPage", Math.max(1, page - 1));
        result.put("nextPage", Math.min(totalPages, page + 1));
        result.put("totalComisiones", total);
        return result;
    }

    public Map<String, Object> getComisionDetalle(int id) {
        List<Map> rows = Base.findAll(
            "SELECT c.id, c.materia_id, c.plan_estudio_id, c.periodo_id, c.turno, c.nombre, c.estado, " +
            "       m.nombre AS materia_nombre, m.codigo AS materia_codigo, " +
            "       pe.anio_vigencia, car.nombre AS carrera_nombre, " +
            "       pl.anio AS periodo_anio, pl.cuatrimestre AS periodo_cuat, pl.descripcion AS periodo_desc " +
            "FROM comisiones c " +
            "JOIN materias m ON c.materia_id = m.id " +
            "JOIN planes_estudio pe ON c.plan_estudio_id = pe.id " +
            "JOIN carreras car ON pe.carrera_id = car.id " +
            "JOIN periodos_lectivos pl ON c.periodo_id = pl.id " +
            "WHERE c.id = ?", id
        );
        if (rows.isEmpty()) return null;
        Map row = rows.get(0);

        List<Map> asigRows = Base.findAll(
            "SELECT ad.id, ad.cargo, ad.activa, ad.fecha_asignacion, " +
            "       p.nombre AS docente_nombre, p.apellido AS docente_apellido, p.dni AS docente_dni, " +
            "       d.id AS docente_id " +
            "FROM asignaciones_docentes ad " +
            "JOIN docentes d ON ad.docente_id = d.id " +
            "JOIN personas p ON d.persona_id = p.id " +
            "WHERE ad.comision_id = ? AND ad.activa = 1 " +
            "ORDER BY ad.cargo ASC, p.apellido ASC",
            id
        );

        List<Map<String, Object>> asignaciones = new ArrayList<>();
        for (Map a : asigRows) {
            Map<String, Object> am = new HashMap<>();
            am.put("id", a.get("id"));
            am.put("comisionId", id);
            am.put("cargo", a.get("cargo"));
            am.put("cargoLabel", cargoLabel((String) a.get("cargo")));
            am.put("activa", toBoolean(a.get("activa")));
            am.put("docenteNombre", a.get("docente_nombre") + " " + a.get("docente_apellido"));
            am.put("docenteDni", a.get("docente_dni"));
            am.put("fechaAsignacion", a.get("fecha_asignacion"));
            asignaciones.add(am);
        }

        boolean activa = "ACTIVA".equals(row.get("estado"));
        int perioCuat = ((Number) row.get("periodo_cuat")).intValue();
        String periodoLabel = row.get("periodo_anio") + " — " + (perioCuat == 1 ? "Primer" : "Segundo") + " Cuatrimestre";

        Map<String, Object> result = new HashMap<>();
        result.put("id", row.get("id"));
        result.put("materiaId", row.get("materia_id"));
        result.put("planEstudioId", row.get("plan_estudio_id"));
        result.put("periodoId", row.get("periodo_id"));
        result.put("turno", row.get("turno"));
        result.put("turnoLabel", turnoLabel((String) row.get("turno")));
        result.put("nombre", row.get("nombre"));
        result.put("estado", row.get("estado"));
        result.put("activa", activa);
        result.put("cerrada", !activa);
        result.put("materiaNombre", row.get("materia_nombre"));
        result.put("materiaCodigo", row.get("materia_codigo"));
        result.put("anioVigencia", row.get("anio_vigencia"));
        result.put("carreraNombre", row.get("carrera_nombre"));
        result.put("periodoLabel", periodoLabel);
        result.put("periodoDesc", row.get("periodo_desc"));
        result.put("asignaciones", asignaciones);
        return result;
    }

    public Map<String, Object> crearComision(int materiaId, int planEstudioId, int periodoId,
            String turno, int adminId) {
        Map<String, Object> result = new HashMap<>();

        Materia materia = Materia.findById(materiaId);
        if (materia == null || !materia.isActiva()) {
            result.put("success", false);
            result.put("message", "La materia debe estar activa.");
            return result;
        }

        PlanDeEstudio plan = PlanDeEstudio.findById(planEstudioId);
        if (plan == null || !plan.isActivo()) {
            result.put("success", false);
            result.put("message", "El plan de estudio debe estar activo.");
            return result;
        }

        PeriodoLectivo periodo = PeriodoLectivo.findById(periodoId);
        if (periodo == null) {
            result.put("success", false);
            result.put("message", "Período no encontrado.");
            return result;
        }

        if (Comision.count("materia_id = ? AND plan_estudio_id = ? AND periodo_id = ? AND turno = ?",
                materiaId, planEstudioId, periodoId, turno) > 0) {
            result.put("success", false);
            result.put("message", "Ya existe una comisión para esa materia, plan, período y turno.");
            return result;
        }

        // Auto-generate nombre
        String nombre = materia.getNombre() + " — Plan " + plan.getAnioVigencia()
            + " — " + turnoLabel(turno) + " (" + periodo.getLabel() + ")";

        Comision c = new Comision();
        c.setMateriaId(materiaId);
        c.setPlanEstudioId(planEstudioId);
        c.setPeriodoId(periodoId);
        c.setTurno(turno);
        c.setNombre(nombre);
        c.setEstado("ACTIVA");
        c.setCreadaPor(adminId);
        c.saveIt();

        result.put("success", true);
        result.put("id", c.getLongId());
        result.put("message", "Comisión creada: " + nombre);
        return result;
    }

    public Map<String, Object> cerrarComision(int id) {
        Map<String, Object> result = new HashMap<>();
        Comision c = Comision.findById(id);
        if (c == null) {
            result.put("success", false);
            result.put("message", "Comisión no encontrada.");
            return result;
        }
        if (!c.isActiva()) {
            result.put("success", false);
            result.put("message", "La comisión ya está cerrada.");
            return result;
        }
        c.setEstado("CERRADA");
        c.touch();
        c.saveIt();

        result.put("success", true);
        result.put("message", "Comisión cerrada.");
        return result;
    }

    // ── SELECTORS ────────────────────────────────────────────────

    public List<Map<String, Object>> listarPeriodosParaSelector(Integer activoPeriodoId) {
        List<Map> rows = Base.findAll(
            "SELECT id, anio, cuatrimestre, descripcion, activo FROM periodos_lectivos ORDER BY anio DESC, cuatrimestre DESC"
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map row : rows) {
            Map<String, Object> m = new HashMap<>();
            int pid = ((Number) row.get("id")).intValue();
            m.put("id", pid);
            int c = ((Number) row.get("cuatrimestre")).intValue();
            m.put("label", row.get("anio") + " — " + (c == 1 ? "Primer" : "Segundo") + " Cuatrimestre");
            m.put("activo", toBoolean(row.get("activo")));
            m.put("selected", activoPeriodoId != null && activoPeriodoId == pid);
            result.add(m);
        }
        return result;
    }

    public List<Map<String, Object>> listarMateriasActivasParaSelector() {
        List<Map> rows = Base.findAll(
            "SELECT id, codigo, nombre FROM materias WHERE activa = 1 ORDER BY nombre ASC"
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map row : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", row.get("id"));
            m.put("displayText", row.get("codigo") + " — " + row.get("nombre"));
            result.add(m);
        }
        return result;
    }

    public List<Map<String, Object>> listarPlanesActivosParaSelector() {
        List<Map> rows = Base.findAll(
            "SELECT pe.id, pe.anio_vigencia, c.nombre AS carrera_nombre " +
            "FROM planes_estudio pe JOIN carreras c ON pe.carrera_id = c.id " +
            "WHERE pe.activo = 1 ORDER BY c.nombre ASC, pe.anio_vigencia ASC"
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map row : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", row.get("id"));
            m.put("displayText", row.get("carrera_nombre") + " — Plan " + row.get("anio_vigencia"));
            result.add(m);
        }
        return result;
    }

    // ── HELPERS ──────────────────────────────────────────────────

    private Map<String, Object> rowToMap(Map row) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", row.get("id"));
        m.put("turno", row.get("turno"));
        m.put("turnoLabel", turnoLabel((String) row.get("turno")));
        m.put("nombre", row.get("nombre"));
        m.put("estado", row.get("estado"));
        m.put("activa", "ACTIVA".equals(row.get("estado")));
        m.put("materiaNombre", row.get("materia_nombre"));
        m.put("materiaCodigo", row.get("materia_codigo"));
        m.put("anioVigencia", row.get("anio_vigencia"));
        int c = ((Number) row.get("periodo_cuat")).intValue();
        m.put("periodoLabel", row.get("periodo_anio") + " — " + (c == 1 ? "1C" : "2C"));
        m.put("docentesCount", row.get("docentes_count"));
        return m;
    }

    private List<Map<String, Object>> buildPages(int page, int totalPages) {
        List<Map<String, Object>> pages = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            Map<String, Object> p = new HashMap<>();
            p.put("number", i);
            p.put("isCurrent", i == page);
            pages.add(p);
        }
        return pages;
    }

    public static String turnoLabel(String t) {
        if ("MANANA".equals(t)) return "Mañana";
        if ("TARDE".equals(t)) return "Tarde";
        if ("NOCHE".equals(t)) return "Noche";
        return t;
    }

    public static String cargoLabel(String c) {
        if ("TITULAR".equals(c)) return "Titular";
        if ("JTP".equals(c)) return "JTP";
        if ("AYUDANTE".equals(c)) return "Ayudante";
        return c;
    }

    private boolean toBoolean(Object val) {
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        return ((Number) val).intValue() == 1;
    }
}
