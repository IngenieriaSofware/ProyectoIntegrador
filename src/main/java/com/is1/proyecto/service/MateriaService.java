package com.is1.proyecto.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.javalite.activejdbc.Base;

import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.MateriaPlan;
import com.is1.proyecto.models.PlanDeEstudio;

public class MateriaService {

    private static final int PAGE_SIZE = 20;

    // ── MATERIAS ─────────────────────────────────────────────────

    public Map<String, Object> listarMaterias(int page, Integer carreraId, Integer planId, boolean incluirInactivas) {
        int offset = (page - 1) * PAGE_SIZE;

        StringBuilder where = new StringBuilder("WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (!incluirInactivas) {
            where.append(" AND m.activa = 1");
        }
        if (planId != null) {
            where.append(" AND EXISTS (SELECT 1 FROM materias_planes mp WHERE mp.materia_id = m.id AND mp.plan_estudio_id = ?)");
            params.add(planId);
        } else if (carreraId != null) {
            where.append(" AND EXISTS (SELECT 1 FROM materias_planes mp JOIN planes_estudio pe ON mp.plan_estudio_id = pe.id WHERE mp.materia_id = m.id AND pe.carrera_id = ?)");
            params.add(carreraId);
        }

        String countSql = "SELECT COUNT(*) as total FROM materias m " + where;
        List<Map> countRow = Base.findAll(countSql, params.toArray());
        int total = ((Number) countRow.get(0).get("total")).intValue();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));

        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(PAGE_SIZE);
        dataParams.add(offset);

        String dataSql = "SELECT m.id, m.codigo, m.nombre, m.activa, " +
            "(SELECT COUNT(*) FROM materias_planes mp2 WHERE mp2.materia_id = m.id AND mp2.activa = 1) AS planes_count " +
            "FROM materias m " + where + " ORDER BY m.nombre ASC LIMIT ? OFFSET ?";

        List<Map> rows = Base.findAll(dataSql, dataParams.toArray());

        List<Map<String, Object>> materias = new ArrayList<>();
        for (Map row : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", row.get("id"));
            m.put("codigo", row.get("codigo"));
            m.put("nombre", row.get("nombre"));
            m.put("activa", toBoolean(row.get("activa")));
            m.put("planesCount", row.get("planes_count"));
            materias.add(m);
        }

        List<Map<String, Object>> pages = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            Map<String, Object> p = new HashMap<>();
            p.put("number", i);
            p.put("isCurrent", i == page);
            pages.add(p);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("materias", materias);
        result.put("pages", pages);
        result.put("currentPage", page);
        result.put("hasPrevious", page > 1);
        result.put("hasNext", page < totalPages);
        result.put("previousPage", Math.max(1, page - 1));
        result.put("nextPage", Math.min(totalPages, page + 1));
        result.put("totalMaterias", total);
        return result;
    }

    public Map<String, Object> getMateriaDetalle(int id) {
        Materia materia = Materia.findById(id);
        if (materia == null) return null;

        List<Map> assocRows = Base.findAll(
            "SELECT mp.id, mp.materia_id, mp.plan_estudio_id, mp.anio_plan, mp.cuatrimestre, " +
            "       mp.carga_horaria, mp.caracter, mp.activa, " +
            "       pe.anio_vigencia, c.nombre AS carrera_nombre, c.codigo AS carrera_codigo " +
            "FROM materias_planes mp " +
            "JOIN planes_estudio pe ON mp.plan_estudio_id = pe.id " +
            "JOIN carreras c ON pe.carrera_id = c.id " +
            "WHERE mp.materia_id = ? " +
            "ORDER BY c.nombre ASC, pe.anio_vigencia ASC",
            id
        );

        List<Map<String, Object>> asociaciones = new ArrayList<>();
        for (Map row : assocRows) {
            Map<String, Object> a = new HashMap<>();
            a.put("id", row.get("id"));
            a.put("materiaId", id);
            a.put("planEstudioId", row.get("plan_estudio_id"));
            a.put("anioPlan", row.get("anio_plan"));
            String cuat = (String) row.get("cuatrimestre");
            a.put("cuatrimestre", cuat);
            a.put("cuatrimestreLabel", cuatrimestreLabel(cuat));
            a.put("cargaHoraria", row.get("carga_horaria"));
            String car = (String) row.get("caracter");
            a.put("caracter", car);
            a.put("caracterLabel", caracterLabel(car));
            boolean activa = toBoolean(row.get("activa"));
            a.put("activa", activa);
            a.put("anioVigencia", row.get("anio_vigencia"));
            a.put("carreraNombre", row.get("carrera_nombre"));
            a.put("carreraCodigo", row.get("carrera_codigo"));
            asociaciones.add(a);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", materia.getLongId());
        result.put("codigo", materia.getCodigo());
        result.put("nombre", materia.getNombre());
        result.put("descripcion", materia.getDescripcion());
        result.put("activa", materia.isActiva());
        result.put("asociaciones", asociaciones);
        return result;
    }

    public Map<String, Object> getMateriaById(int id) {
        Materia materia = Materia.findById(id);
        if (materia == null) return null;
        Map<String, Object> m = new HashMap<>();
        m.put("id", materia.getLongId());
        m.put("codigo", materia.getCodigo());
        m.put("nombre", materia.getNombre());
        m.put("descripcion", materia.getDescripcion());
        m.put("activa", materia.isActiva());
        return m;
    }

    public Map<String, Object> crearMateria(String codigo, String nombre, String descripcion, int adminId) {
        Map<String, Object> result = new HashMap<>();

        if (codigo == null || codigo.trim().isEmpty()) {
            result.put("success", false);
            result.put("field", "codigo");
            result.put("message", "El código es obligatorio.");
            return result;
        }
        codigo = codigo.toUpperCase().trim();

        // CB-02: alphanumeric + dash only, no spaces
        if (!codigo.matches("[A-Z0-9\\-]+")) {
            result.put("success", false);
            result.put("field", "codigo");
            result.put("message", "El código solo puede contener letras, números y guion medio, sin espacios.");
            return result;
        }

        if (nombre == null || nombre.trim().isEmpty()) {
            result.put("success", false);
            result.put("field", "nombre");
            result.put("message", "El nombre es obligatorio.");
            return result;
        }
        nombre = nombre.trim();

        // RN-01: unique codigo (active or inactive)
        if (Materia.count("UPPER(codigo) = ?", codigo) > 0) {
            result.put("success", false);
            result.put("field", "codigo");
            result.put("message", "El código '" + codigo + "' ya está en uso.");
            return result;
        }

        // RN-02 + CB-03: unique nombre case-insensitive (active or inactive)
        if (Materia.count("LOWER(nombre) = ?", nombre.toLowerCase()) > 0) {
            result.put("success", false);
            result.put("field", "nombre");
            result.put("message", "Ya existe una materia con ese nombre.");
            return result;
        }

        Materia m = new Materia();
        m.setCodigo(codigo);
        m.setNombre(nombre);
        m.setDescripcion(descripcion);
        m.setActiva(true);
        m.setCreadaPor(adminId);
        m.saveIt();

        result.put("success", true);
        result.put("id", m.getLongId());
        result.put("nombre", nombre);
        result.put("message", "Materia creada correctamente.");
        return result;
    }

    public Map<String, Object> editarMateria(int id, String nombre, String descripcion, int adminId) {
        Map<String, Object> result = new HashMap<>();
        Materia m = Materia.findById(id);
        if (m == null) {
            result.put("success", false);
            result.put("message", "Materia no encontrada.");
            return result;
        }

        if (nombre == null || nombre.trim().isEmpty()) {
            result.put("success", false);
            result.put("field", "nombre");
            result.put("message", "El nombre es obligatorio.");
            return result;
        }
        nombre = nombre.trim();
        String newDesc = (descripcion != null && !descripcion.trim().isEmpty()) ? descripcion.trim() : null;

        // RN-02 + CB-03: unique nombre, excluding self
        if (Materia.count("LOWER(nombre) = ? AND id != ?", nombre.toLowerCase(), id) > 0) {
            result.put("success", false);
            result.put("field", "nombre");
            result.put("message", "Ya existe una materia con ese nombre.");
            return result;
        }

        boolean nombreChanged = !m.getNombre().equals(nombre);
        boolean descChanged = !Objects.equals(m.getDescripcion(), newDesc);

        if (!nombreChanged && !descChanged) {
            result.put("success", false);
            result.put("noChanges", true);
            result.put("message", "No se realizaron modificaciones.");
            return result;
        }

        result.put("oldNombre", m.getNombre());
        m.setNombre(nombre);
        m.setDescripcion(newDesc);
        m.touch();
        m.saveIt();

        result.put("success", true);
        result.put("newNombre", nombre);
        result.put("message", "Materia actualizada correctamente.");
        return result;
    }

    public Map<String, Object> desactivarMateria(int id) {
        Map<String, Object> result = new HashMap<>();
        Materia m = Materia.findById(id);
        if (m == null) {
            result.put("success", false);
            result.put("message", "Materia no encontrada.");
            return result;
        }

        // CB-07: already inactive
        if (!m.isActiva()) {
            result.put("success", false);
            result.put("message", "La materia ya está inactiva.");
            return result;
        }

        long asociacionesActivas = MateriaPlan.count("materia_id = ? AND activa = 1", id);

        m.setActiva(false);
        m.touch();
        m.saveIt();

        result.put("success", true);
        result.put("tieneAsociacionesActivas", asociacionesActivas > 0);
        result.put("asociacionesActivasCount", asociacionesActivas);
        if (asociacionesActivas > 0) {
            result.put("message", "Materia desactivada. Advertencia: tiene " + asociacionesActivas +
                " asociación(es) activa(s) vigente(s) que deben revisarse.");
        } else {
            result.put("message", "Materia desactivada correctamente.");
        }
        return result;
    }

    public Map<String, Object> reactivarMateria(int id) {
        Map<String, Object> result = new HashMap<>();
        Materia m = Materia.findById(id);
        if (m == null) {
            result.put("success", false);
            result.put("message", "Materia no encontrada.");
            return result;
        }

        // CB-06: already active
        if (m.isActiva()) {
            result.put("success", false);
            result.put("message", "La materia ya está activa.");
            return result;
        }

        m.setActiva(true);
        m.touch();
        m.saveIt();

        result.put("success", true);
        result.put("message", "Materia reactivada. Las asociaciones previas desactivadas no se reactivan automáticamente.");
        return result;
    }

    // ── MATERIAS-PLANES ──────────────────────────────────────────

    public Map<String, Object> asociarPlan(int materiaId, int planId, int anioPlan,
            String cuatrimestre, int cargaHoraria, String caracter, int adminId) {
        Map<String, Object> result = new HashMap<>();

        // RN-10: materia must be active
        Materia materia = Materia.findById(materiaId);
        if (materia == null || !materia.isActiva()) {
            result.put("success", false);
            result.put("message", "La materia debe estar activa para realizar asociaciones.");
            return result;
        }

        // RN-09: plan must exist and be active
        PlanDeEstudio plan = PlanDeEstudio.findById(planId);
        if (plan == null || !plan.isActivo()) {
            result.put("success", false);
            result.put("message", "El plan de estudio debe estar activo.");
            return result;
        }

        // RN-04: uniqueness of (materia_id, plan_estudio_id) — active or inactive
        if (MateriaPlan.count("materia_id = ? AND plan_estudio_id = ?", materiaId, planId) > 0) {
            result.put("success", false);
            result.put("message", "Esta materia ya está asociada a ese plan de estudio.");
            return result;
        }

        // CB-09
        if (anioPlan < 1) {
            result.put("success", false);
            result.put("field", "anioPlan");
            result.put("message", "El año del plan debe ser mayor a 0.");
            return result;
        }
        // CB-08
        if (cargaHoraria < 1) {
            result.put("success", false);
            result.put("field", "cargaHoraria");
            result.put("message", "La carga horaria debe ser mayor a 0.");
            return result;
        }

        MateriaPlan mp = new MateriaPlan();
        mp.setMateriaId(materiaId);
        mp.setPlanEstudioId(planId);
        mp.setAnioPlan(anioPlan);
        mp.setCuatrimestre(cuatrimestre);
        mp.setCargaHoraria(cargaHoraria);
        mp.setCaracter(caracter);
        mp.setActiva(true);
        mp.setCreadoPor(adminId);
        mp.saveIt();

        result.put("success", true);
        result.put("id", mp.getLongId());
        result.put("planId", planId);
        result.put("message", "Materia asociada al plan correctamente.");
        return result;
    }

    public Map<String, Object> editarAsociacion(int mpId, int anioPlan, String cuatrimestre,
            int cargaHoraria, String caracter) {
        Map<String, Object> result = new HashMap<>();
        MateriaPlan mp = MateriaPlan.findById(mpId);
        if (mp == null) {
            result.put("success", false);
            result.put("message", "Asociación no encontrada.");
            return result;
        }

        // CB-09
        if (anioPlan < 1) {
            result.put("success", false);
            result.put("field", "anioPlan");
            result.put("message", "El año del plan debe ser mayor a 0.");
            return result;
        }
        // CB-08
        if (cargaHoraria < 1) {
            result.put("success", false);
            result.put("field", "cargaHoraria");
            result.put("message", "La carga horaria debe ser mayor a 0.");
            return result;
        }

        mp.setAnioPlan(anioPlan);
        mp.setCuatrimestre(cuatrimestre);
        mp.setCargaHoraria(cargaHoraria);
        mp.setCaracter(caracter);
        mp.touch();
        mp.saveIt();

        result.put("success", true);
        result.put("materiaId", mp.getMateriaId());
        result.put("message", "Asociación actualizada correctamente.");
        return result;
    }

    public Map<String, Object> desactivarAsociacion(int mpId) {
        Map<String, Object> result = new HashMap<>();
        MateriaPlan mp = MateriaPlan.findById(mpId);
        if (mp == null) {
            result.put("success", false);
            result.put("message", "Asociación no encontrada.");
            return result;
        }

        // RN-08: check cursadas (stub — tabla cursadas se implementa en HU-C)
        // Cuando se implemente: SELECT COUNT(*) FROM cursadas WHERE materia_id=? AND plan_estudio_id=?
        // Por ahora siempre 0 cursadas.

        mp.setActiva(false);
        mp.touch();
        mp.saveIt();

        result.put("success", true);
        result.put("materiaId", mp.getMateriaId());
        result.put("planEstudioId", mp.getPlanEstudioId());
        result.put("message", "Asociación desactivada correctamente.");
        return result;
    }

    public Map<String, Object> getAsociacionById(int mpId) {
        MateriaPlan mp = MateriaPlan.findById(mpId);
        if (mp == null) return null;

        List<Map> rows = Base.findAll(
            "SELECT mp.id, mp.materia_id, mp.plan_estudio_id, mp.anio_plan, mp.cuatrimestre, " +
            "       mp.carga_horaria, mp.caracter, mp.activa, " +
            "       pe.anio_vigencia, c.nombre AS carrera_nombre " +
            "FROM materias_planes mp " +
            "JOIN planes_estudio pe ON mp.plan_estudio_id = pe.id " +
            "JOIN carreras c ON pe.carrera_id = c.id " +
            "WHERE mp.id = ?",
            mpId
        );

        if (rows.isEmpty()) return null;
        Map row = rows.get(0);

        Map<String, Object> m = new HashMap<>();
        m.put("id", mp.getLongId());
        m.put("materiaId", mp.getMateriaId());
        m.put("planEstudioId", mp.getPlanEstudioId());
        m.put("anioPlan", mp.getAnioPlan());
        m.put("cuatrimestre", mp.getCuatrimestre());
        m.put("cargaHoraria", mp.getCargaHoraria());
        m.put("caracter", mp.getCaracter());
        m.put("activa", mp.isActiva());
        m.put("anioVigencia", row.get("anio_vigencia"));
        m.put("carreraNombre", row.get("carrera_nombre"));
        m.put("planDisplayText", row.get("carrera_nombre") + " — Plan " + row.get("anio_vigencia"));
        // Pre-select flags for cuatrimestre
        m.put("cuatPrimero", "PRIMERO".equals(mp.getCuatrimestre()));
        m.put("cuatSegundo", "SEGUNDO".equals(mp.getCuatrimestre()));
        m.put("cuatAnual", "ANUAL".equals(mp.getCuatrimestre()));
        // Pre-select flags for caracter
        m.put("caracObligatoria", "OBLIGATORIA".equals(mp.getCaracter()));
        m.put("caracElectiva", "ELECTIVA".equals(mp.getCaracter()));
        return m;
    }

    // ── SELECTORS ────────────────────────────────────────────────

    public List<Map<String, Object>> listarPlanesActivosParaSelector() {
        List<Map> rows = Base.findAll(
            "SELECT pe.id, pe.anio_vigencia, c.nombre AS carrera_nombre " +
            "FROM planes_estudio pe " +
            "JOIN carreras c ON pe.carrera_id = c.id " +
            "WHERE pe.activo = 1 " +
            "ORDER BY c.nombre ASC, pe.anio_vigencia ASC"
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map row : rows) {
            Map<String, Object> p = new HashMap<>();
            p.put("id", row.get("id"));
            p.put("displayText", row.get("carrera_nombre") + " — Plan " + row.get("anio_vigencia"));
            result.add(p);
        }
        return result;
    }

    public List<Map<String, Object>> listarCarrerasParaFiltro() {
        List<Map> rows = Base.findAll(
            "SELECT id, nombre FROM carreras ORDER BY nombre ASC"
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map row : rows) {
            Map<String, Object> c = new HashMap<>();
            c.put("id", row.get("id"));
            c.put("nombre", row.get("nombre"));
            result.add(c);
        }
        return result;
    }

    public List<Map<String, Object>> listarPlanesParaFiltro() {
        List<Map> rows = Base.findAll(
            "SELECT pe.id, pe.anio_vigencia, c.nombre AS carrera_nombre " +
            "FROM planes_estudio pe JOIN carreras c ON pe.carrera_id = c.id " +
            "ORDER BY c.nombre ASC, pe.anio_vigencia ASC"
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map row : rows) {
            Map<String, Object> p = new HashMap<>();
            p.put("id", row.get("id"));
            p.put("displayText", row.get("carrera_nombre") + " — " + row.get("anio_vigencia"));
            result.add(p);
        }
        return result;
    }

    // ── HELPERS ──────────────────────────────────────────────────

    private boolean toBoolean(Object val) {
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        return ((Number) val).intValue() == 1;
    }

    private String cuatrimestreLabel(String c) {
        if ("PRIMERO".equals(c)) return "1er Cuatrimestre";
        if ("SEGUNDO".equals(c)) return "2do Cuatrimestre";
        if ("ANUAL".equals(c)) return "Anual";
        return c;
    }

    private String caracterLabel(String c) {
        if ("OBLIGATORIA".equals(c)) return "Obligatoria";
        if ("ELECTIVA".equals(c)) return "Electiva";
        return c;
    }
}
