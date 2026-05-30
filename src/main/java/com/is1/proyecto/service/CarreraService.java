package com.is1.proyecto.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javalite.activejdbc.Base;

import com.is1.proyecto.models.Carrera;
import com.is1.proyecto.models.Estudiante;
import com.is1.proyecto.models.PlanDeEstudio;

public class CarreraService {

    private static final int PAGE_SIZE = 20;

    // ── CARRERAS ─────────────────────────────────────────────────

    public Map<String, Object> listarCarreras(int page) {
        int offset = (page - 1) * PAGE_SIZE;

        List<Map> countRow = Base.findAll("SELECT COUNT(*) as total FROM carreras");
        int total = ((Number) countRow.get(0).get("total")).intValue();
        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;

        List<Map> rows = Base.findAll(
            "SELECT c.id, c.codigo, c.nombre, c.activa, " +
            "  (SELECT GROUP_CONCAT(p.anio_vigencia, ', ') " +
            "   FROM planes_estudio p WHERE p.carrera_id = c.id AND p.activo = 1) AS planes_activos " +
            "FROM carreras c ORDER BY c.nombre ASC LIMIT ? OFFSET ?",
            PAGE_SIZE, offset
        );

        List<Map<String, Object>> carreras = new ArrayList<>();
        for (Map row : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", row.get("id"));
            m.put("codigo", row.get("codigo"));
            m.put("nombre", row.get("nombre"));
            m.put("activa", toBoolean(row.get("activa")));
            Object pa = row.get("planes_activos");
            m.put("planActivoAnio", pa != null ? pa.toString() : "Sin plan activo");
            carreras.add(m);
        }

        // Build page list for template
        List<Map<String, Object>> pages = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            Map<String, Object> p = new HashMap<>();
            p.put("number", i);
            p.put("isCurrent", i == page);
            pages.add(p);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("carreras", carreras);
        result.put("pages", pages);
        result.put("currentPage", page);
        result.put("hasPrevious", page > 1);
        result.put("hasNext", page < totalPages);
        result.put("previousPage", Math.max(1, page - 1));
        result.put("nextPage", Math.min(totalPages, page + 1));
        result.put("totalCarreras", total);
        return result;
    }

    public Map<String, Object> getCarreraDetalle(int id) {
        Carrera carrera = Carrera.findById(id);
        if (carrera == null) return null;

        List<Map> planRows = Base.findAll(
            "SELECT * FROM planes_estudio WHERE carrera_id = ? ORDER BY anio_vigencia DESC", id
        );

        List<Map<String, Object>> planes = new ArrayList<>();
        int activosCount = 0;
        for (Map row : planRows) {
            Map<String, Object> p = new HashMap<>();
            p.put("id", row.get("id"));
            p.put("carreraId", id);
            p.put("anioVigencia", row.get("anio_vigencia"));
            p.put("descripcion", row.get("descripcion"));
            p.put("duracionAnios", row.get("duracion_anios"));
            boolean activo = toBoolean(row.get("activo"));
            p.put("activo", activo);
            p.put("estadoLabel", activo ? "Activo" : "Inactivo");
            if (activo) activosCount++;
            planes.add(p);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", carrera.getLongId());
        result.put("codigo", carrera.getCodigo());
        result.put("nombre", carrera.getNombre());
        result.put("descripcion", carrera.getDescripcion());
        result.put("activa", carrera.isActiva());
        result.put("planes", planes);
        result.put("tieneMultiplesActivos", activosCount > 1);
        result.put("sinPlanActivo", activosCount == 0);
        return result;
    }

    public Map<String, Object> listarCarrerasVigentes() {
        List<Map> rows = Base.findAll(
            "SELECT c.id, c.codigo, c.nombre, c.descripcion, c.activa, " +
            "  (SELECT GROUP_CONCAT(p.anio_vigencia, ', ') " +
            "   FROM planes_estudio p WHERE p.carrera_id = c.id AND p.activo = 1) AS planes_activos " +
            "FROM carreras c WHERE c.activa = 1 ORDER BY c.nombre ASC"
        );

        List<Map<String, Object>> carreras = new ArrayList<>();
        for (Map row : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", row.get("id"));
            m.put("codigo", row.get("codigo"));
            m.put("nombre", row.get("nombre"));
            m.put("descripcion", row.get("descripcion"));
            m.put("activa", toBoolean(row.get("activa")));
            Object pa = row.get("planes_activos");
            String planActivoAnio = pa != null ? pa.toString() : "Sin plan vigente";
            m.put("planActivoAnio", planActivoAnio);
            m.put("tienePlanesActivos", pa != null && !pa.toString().trim().isEmpty());
            carreras.add(m);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("carreras", carreras);
        return result;
    }

    public Map<String, Object> getCarreraConPlanesActivos(int id) {
        Carrera carrera = Carrera.findById(id);
        if (carrera == null || !carrera.isActiva()) return null;

        List<Map> planRows = Base.findAll(
            "SELECT * FROM planes_estudio WHERE carrera_id = ? AND activo = 1 ORDER BY anio_vigencia DESC", id
        );

        List<Map<String, Object>> planes = new ArrayList<>();
        for (Map row : planRows) {
            Map<String, Object> p = new HashMap<>();
            p.put("id", row.get("id"));
            p.put("anioVigencia", row.get("anio_vigencia"));
            p.put("descripcion", row.get("descripcion"));
            p.put("duracionAnios", row.get("duracion_anios"));
            p.put("activo", true);
            planes.add(p);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", carrera.getLongId());
        result.put("codigo", carrera.getCodigo());
        result.put("nombre", carrera.getNombre());
        result.put("descripcion", carrera.getDescripcion());
        result.put("planes", planes);
        result.put("tienePlanes", !planes.isEmpty());
        result.put("sinPlanes", planes.isEmpty());
        return result;
    }

    public Map<String, Object> inscribirEstudiante(int personaId, int carreraId, int planId) {
        Map<String, Object> result = new HashMap<>();

        Estudiante estudiante = Estudiante.findByPersonaId(personaId).orElse(null);
        if (estudiante == null) {
            result.put("success", false);
            result.put("message", "Perfil de estudiante no encontrado.");
            return result;
        }

        Carrera carrera = Carrera.findById(carreraId);
        if (carrera == null || !carrera.isActiva()) {
            result.put("success", false);
            result.put("message", "Carrera no válida o inactiva.");
            return result;
        }

        PlanDeEstudio plan = PlanDeEstudio.findById(planId);
        if (plan == null || plan.getCarreraId() != carreraId || !plan.isActivo()) {
            result.put("success", false);
            result.put("message", "Plan de estudio no válido o no vigente.");
            return result;
        }

        String planLabel = "Plan " + plan.getAnioVigencia();
        if (plan.getDescripcion() != null && !plan.getDescripcion().trim().isEmpty()) {
            planLabel += " - " + plan.getDescripcion().trim();
        }

        estudiante.setCarrera(carrera.getNombre());
        estudiante.setPlanEstudio(planLabel);
        estudiante.saveIt();

        result.put("success", true);
        result.put("message", "Inscripción a la carrera registrada con éxito.");
        result.put("carrera", carrera.getNombre());
        result.put("planEstudio", planLabel);
        return result;
    }

    public Map<String, Object> crearCarrera(String codigo, String nombre, String descripcion) {
        Map<String, Object> result = new HashMap<>();
        codigo = codigo.toUpperCase().replaceAll("\\s+", "").trim();
        nombre = nombre.trim();

        if (codigo.isEmpty() || nombre.isEmpty()) {
            result.put("success", false);
            result.put("message", "Código y nombre son obligatorios.");
            return result;
        }

        if (codigoExiste(codigo, -1)) {
            result.put("success", false);
            result.put("field", "codigo");
            result.put("message", "El código '" + codigo + "' ya está en uso.");
            return result;
        }

        if (nombreExiste(nombre, -1)) {
            result.put("success", false);
            result.put("field", "nombre");
            result.put("message", "Ya existe una carrera con este nombre.");
            return result;
        }

        Carrera c = new Carrera();
        c.setCodigo(codigo);
        c.setNombre(nombre);
        c.setDescripcion(descripcion);
        c.setActiva(true);
        c.saveIt();

        result.put("success", true);
        result.put("id", c.getLongId());
        result.put("message", "Carrera creada correctamente.");
        return result;
    }

    public Map<String, Object> editarCarrera(int id, String nombre, String descripcion) {
        Map<String, Object> result = new HashMap<>();
        Carrera c = Carrera.findById(id);
        if (c == null) {
            result.put("success", false);
            result.put("message", "Carrera no encontrada.");
            return result;
        }

        nombre = nombre.trim();
        if (nombre.isEmpty()) {
            result.put("success", false);
            result.put("message", "El nombre es obligatorio.");
            return result;
        }

        if (nombreExiste(nombre, id)) {
            result.put("success", false);
            result.put("field", "nombre");
            result.put("message", "Ya existe una carrera con este nombre.");
            return result;
        }

        c.setNombre(nombre);
        c.setDescripcion(descripcion);
        c.touch();
        c.saveIt();

        result.put("success", true);
        result.put("message", "Carrera actualizada correctamente.");
        return result;
    }

    public Map<String, Object> darDeBajaCarrera(int id) {
        Map<String, Object> result = new HashMap<>();
        Carrera c = Carrera.findById(id);
        if (c == null) {
            result.put("success", false);
            result.put("message", "Carrera no encontrada.");
            return result;
        }
        c.setActiva(false);
        c.touch();
        c.saveIt();

        result.put("success", true);
        result.put("message", "Carrera dada de baja correctamente.");
        return result;
    }

    // ── PLANES DE ESTUDIO ────────────────────────────────────────

    public Map<String, Object> crearPlan(int carreraId, int anioVigencia, String descripcion, Integer duracionAnios) {
        Map<String, Object> result = new HashMap<>();

        Carrera carrera = Carrera.findById(carreraId);
        if (carrera == null || !carrera.isActiva()) {
            result.put("success", false);
            result.put("message", "No se pueden agregar planes a una carrera inactiva.");
            return result;
        }

        int anioActual = Calendar.getInstance().get(Calendar.YEAR);
        if (anioVigencia < 1900 || anioVigencia > anioActual + 5) {
            result.put("success", false);
            result.put("field", "anioVigencia");
            result.put("message", "El año de vigencia debe estar entre 1900 y " + (anioActual + 5) + ".");
            return result;
        }

        long existe = PlanDeEstudio.count("carrera_id = ? AND anio_vigencia = ?", carreraId, anioVigencia);
        if (existe > 0) {
            result.put("success", false);
            result.put("field", "anioVigencia");
            result.put("message", "Ya existe un plan de vigencia " + anioVigencia + " para esta carrera.");
            return result;
        }

        PlanDeEstudio plan = new PlanDeEstudio();
        plan.setCarreraId(carreraId);
        plan.setAnioVigencia(anioVigencia);
        plan.setDescripcion(descripcion);
        plan.setDuracionAnios(duracionAnios);
        plan.setActivo(false);
        plan.saveIt();

        result.put("success", true);
        result.put("id", plan.getLongId());
        result.put("message", "Plan de estudio " + anioVigencia + " creado correctamente. Este plan aún no está activo.");
        return result;
    }

    public Map<String, Object> activarPlan(int planId) {
        Map<String, Object> result = new HashMap<>();
        PlanDeEstudio plan = PlanDeEstudio.findById(planId);
        if (plan == null) {
            result.put("success", false);
            result.put("message", "Plan no encontrado.");
            return result;
        }

        Carrera carrera = Carrera.findById(plan.getCarreraId());
        if (carrera == null || !carrera.isActiva()) {
            result.put("success", false);
            result.put("message", "No se pueden activar planes de una carrera inactiva.");
            return result;
        }

        long otrosActivos = PlanDeEstudio.count("carrera_id = ? AND activo = 1 AND id != ?", plan.getCarreraId(), planId);
        plan.setActivo(true);
        plan.touch();
        plan.saveIt();

        result.put("success", true);
        result.put("tieneOtroActivo", otrosActivos > 0);
        result.put("message", "Plan " + plan.getAnioVigencia() + " activado correctamente.");
        return result;
    }

    public Map<String, Object> desactivarPlan(int planId) {
        Map<String, Object> result = new HashMap<>();
        PlanDeEstudio plan = PlanDeEstudio.findById(planId);
        if (plan == null) {
            result.put("success", false);
            result.put("message", "Plan no encontrado.");
            return result;
        }

        // Conteo de estudiantes bajo este plan (cuando se implemente la FK real)
        int estudiantesCount = 0;

        plan.setActivo(false);
        plan.touch();
        plan.saveIt();

        result.put("success", true);
        result.put("estudiantesAfectados", estudiantesCount);
        result.put("message", "Plan " + plan.getAnioVigencia() + " desactivado correctamente.");
        return result;
    }

    public Map<String, Object> editarPlan(int planId, String descripcion, Integer duracionAnios) {
        Map<String, Object> result = new HashMap<>();
        PlanDeEstudio plan = PlanDeEstudio.findById(planId);
        if (plan == null) {
            result.put("success", false);
            result.put("message", "Plan no encontrado.");
            return result;
        }

        if (duracionAnios != null && duracionAnios < 1) {
            result.put("success", false);
            result.put("field", "duracionAnios");
            result.put("message", "La duración debe ser mayor a 0.");
            return result;
        }

        plan.setDescripcion(descripcion);
        plan.setDuracionAnios(duracionAnios);
        plan.touch();
        plan.saveIt();

        result.put("success", true);
        result.put("message", "Plan actualizado correctamente.");
        return result;
    }

    public Map<String, Object> getPlanById(int planId) {
        PlanDeEstudio plan = PlanDeEstudio.findById(planId);
        if (plan == null) return null;
        Map<String, Object> m = new HashMap<>();
        m.put("id", plan.getLongId());
        m.put("carreraId", plan.getCarreraId());
        m.put("anioVigencia", plan.getAnioVigencia());
        m.put("descripcion", plan.getDescripcion());
        m.put("duracionAnios", plan.getDuracionAnios());
        m.put("activo", plan.isActivo());
        return m;
    }

    // ── HELPERS ──────────────────────────────────────────────────

    private boolean codigoExiste(String codigo, int excludeId) {
        if (excludeId <= 0) {
            return Carrera.count("UPPER(codigo) = ?", codigo.toUpperCase()) > 0;
        }
        return Carrera.count("UPPER(codigo) = ? AND id != ?", codigo.toUpperCase(), excludeId) > 0;
    }

    private boolean nombreExiste(String nombre, int excludeId) {
        if (excludeId <= 0) {
            return Carrera.count("LOWER(nombre) = ?", nombre.toLowerCase()) > 0;
        }
        return Carrera.count("LOWER(nombre) = ? AND id != ?", nombre.toLowerCase(), excludeId) > 0;
    }

    private boolean toBoolean(Object val) {
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        return ((Number) val).intValue() == 1;
    }
}
