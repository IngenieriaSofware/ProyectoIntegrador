package com.is1.proyecto.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.is1.proyecto.service.AsignacionDocenteService;
import com.is1.proyecto.service.AuditService;
import com.is1.proyecto.service.ComisionService;
import com.is1.proyecto.service.PeriodoService;
import org.javalite.activejdbc.Base;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

public class ComisionController {

    private final ComisionService comisionService = new ComisionService();
    private final PeriodoService periodoService = new PeriodoService();
    private final AsignacionDocenteService asignacionService = new AsignacionDocenteService();

    // ── AUTH ─────────────────────────────────────────────────────

    private boolean esAdmin(Request req) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) req.session().attribute("roles");
        return roles != null && roles.contains("ADMINISTRADOR");
    }

    private int adminId(Request req) {
        Object id = req.session().attribute("personaId");
        return id != null ? ((Number) id).intValue() : 0;
    }

    // ── COMISIONES ───────────────────────────────────────────────

    public ModelAndView listComisiones(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        int page = parseIntOrDefault(req.queryParams("page"), 1);
        Integer periodoId = parseIntOrNull(req.queryParams("periodoId"));

        // Si no filtra, mostrar el período activo por defecto
        Map<String, Object> periodoActivo = periodoService.getPeriodoActivo();
        if (periodoId == null && periodoActivo != null) {
            periodoId = ((Number) periodoActivo.get("id")).intValue();
        }

        Map<String, Object> data = comisionService.listarComisiones(page, periodoId);
        data.put("email", req.session().attribute("email"));
        data.put("periodos", comisionService.listarPeriodosParaSelector(periodoId));
        data.put("periodoActivo", periodoActivo);
        data.put("filtroPeriodoId", periodoId);

        String msg = req.queryParams("message");
        String err = req.queryParams("error");
        if (msg != null && !msg.isEmpty()) data.put("successMessage", msg);
        if (err != null && !err.isEmpty()) data.put("errorMessage", err);

        return new ModelAndView(data, "comisiones_list.mustache");
    }

    public ModelAndView showComisionForm(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        Map<String, Object> periodoActivo = periodoService.getPeriodoActivo();

        Map<String, Object> model = new HashMap<>();
        model.put("email", req.session().attribute("email"));
        model.put("periodoActivo", periodoActivo);
        model.put("sinPeriodoActivo", periodoActivo == null);
        model.put("materias", comisionService.listarMateriasActivasParaSelector());
        model.put("planes", comisionService.listarPlanesActivosParaSelector());

        String err = req.queryParams("error");
        if (err != null && !err.isEmpty()) model.put("errorMessage", err);

        return new ModelAndView(model, "comision_form.mustache");
    }

    public Object createComision(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        Map<String, Object> periodoActivo = periodoService.getPeriodoActivo();
        if (periodoActivo == null) {
            res.redirect("/admin/comisiones/nueva?error=" + encode("No hay período activo configurado."));
            return "";
        }

        int materiaId = parseIntOrDefault(req.queryParams("materiaId"), 0);
        int planId = parseIntOrDefault(req.queryParams("planEstudioId"), 0);
        int periodoId = ((Number) periodoActivo.get("id")).intValue();
        String turno = req.queryParams("turno");

        Map<String, Object> result = comisionService.crearComision(materiaId, planId, periodoId, turno, adminId(req));

        if ((Boolean) result.get("success")) {
            int newId = ((Number) result.get("id")).intValue();
            AuditService.log(adminId(req), "CREAR_COMISION", newId, (String) result.get("message"));
            res.redirect("/admin/comisiones/" + newId + "?message=" + encode("Comisión creada correctamente."));
        } else {
            res.redirect("/admin/comisiones/nueva?error=" + encode((String) result.get("message")));
        }
        return "";
    }

    public ModelAndView showComisionDetalle(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        int id = parseInt(req.params("id"));
        Map<String, Object> model = comisionService.getComisionDetalle(id);
        if (model == null) {
            res.redirect("/admin/comisiones?error=" + encode("Comisión no encontrada."));
            return null;
        }

        model.put("email", req.session().attribute("email"));

        // RN-01: verificar período activo para habilitar/deshabilitar asignaciones
        Map<String, Object> periodoActivo = periodoService.getPeriodoActivo();
        boolean periodoActivoCoincide = periodoActivo != null &&
            periodoActivo.get("id").toString().equals(model.get("periodoId").toString());
        model.put("puedeAsignar", periodoActivoCoincide && Boolean.TRUE.equals(model.get("activa")));

        String msg = req.queryParams("message");
        String err = req.queryParams("error");
        if (msg != null && !msg.isEmpty()) model.put("successMessage", msg);
        if (err != null && !err.isEmpty()) model.put("errorMessage", err);

        return new ModelAndView(model, "comision_detail.mustache");
    }

    public Object cerrarComision(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        int id = parseInt(req.params("id"));
        Map<String, Object> result = comisionService.cerrarComision(id);

        if ((Boolean) result.get("success")) {
            AuditService.log(adminId(req), "CERRAR_COMISION", id, "Comisión cerrada");
            res.redirect("/admin/comisiones/" + id + "?message=" + encode("Comisión cerrada."));
        } else {
            res.redirect("/admin/comisiones/" + id + "?error=" + encode((String) result.get("message")));
        }
        return "";
    }

    // ── ASIGNACIONES ─────────────────────────────────────────────

    public ModelAndView showAsignacionForm(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        int comisionId = parseInt(req.params("id"));
        Map<String, Object> comision = comisionService.getComisionDetalle(comisionId);
        if (comision == null) { res.redirect("/admin/comisiones"); return null; }

        // RN-01
        Map<String, Object> periodoActivo = periodoService.getPeriodoActivo();
        if (periodoActivo == null) {
            res.redirect("/admin/comisiones/" + comisionId + "?error=" +
                encode("No hay un período lectivo activo configurado. Configure uno antes de realizar asignaciones."));
            return null;
        }

        String q = req.queryParams("q");
        List<Map<String, Object>> resultadosBusqueda = null;
        if (q != null && !q.trim().isEmpty()) {
            resultadosBusqueda = asignacionService.buscarDocentes(q);
        }

        Map<String, Object> model = new HashMap<>();
        model.put("email", req.session().attribute("email"));
        model.put("comisionId", comisionId);
        model.put("comisionNombre", comision.get("nombre"));
        model.put("materiaNombre", comision.get("materiaNombre"));
        model.put("q", q);
        model.put("resultadosBusqueda", resultadosBusqueda);
        model.put("hayResultados", resultadosBusqueda != null && !resultadosBusqueda.isEmpty());
        model.put("sinResultados", resultadosBusqueda != null && resultadosBusqueda.isEmpty());

        String err = req.queryParams("error");
        if (err != null && !err.isEmpty()) model.put("errorMessage", err);

        return new ModelAndView(model, "asignacion_form.mustache");
    }

    public Object createAsignacion(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        int comisionId = parseInt(req.params("id"));

        // RN-01
        Map<String, Object> periodoActivo = periodoService.getPeriodoActivo();
        if (periodoActivo == null) {
            res.redirect("/admin/comisiones/" + comisionId + "?error=" +
                encode("No hay período lectivo activo. Configure uno antes de asignar docentes."));
            return "";
        }

        int docenteId = parseIntOrDefault(req.queryParams("docenteId"), 0);
        String cargo = req.queryParams("cargo");

        if (docenteId == 0) {
            res.redirect("/admin/comisiones/" + comisionId + "/asignaciones/nueva?error=" +
                encode("Debe seleccionar un docente."));
            return "";
        }

        Map<String, Object> result = asignacionService.asignarDocente(comisionId, docenteId, cargo, adminId(req));

        if ((Boolean) result.get("success")) {
            int asId = ((Number) result.get("id")).intValue();
            AuditService.log(adminId(req), "ALTA_ASIGNACION", asId,
                "{\"accion\":\"ALTA\",\"comision\":" + comisionId + ",\"docente\":" + docenteId + ",\"cargo\":\"" + cargo + "\"}");
            res.redirect("/admin/comisiones/" + comisionId + "?message=" + encode((String) result.get("message")));
        } else {
            res.redirect("/admin/comisiones/" + comisionId + "/asignaciones/nueva?error=" +
                encode((String) result.get("message")));
        }
        return "";
    }

    public ModelAndView showCambiarCargoForm(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        int comisionId = parseInt(req.params("id"));
        int asId = parseInt(req.params("asId"));

        Map<String, Object> comision = comisionService.getComisionDetalle(comisionId);
        if (comision == null) { res.redirect("/admin/comisiones"); return null; }

        // Fetch asignacion data
        List<Map> rows = Base.findAll(
            "SELECT ad.cargo, p.nombre, p.apellido FROM asignaciones_docentes ad " +
            "JOIN docentes d ON ad.docente_id = d.id " +
            "JOIN personas p ON d.persona_id = p.id " +
            "WHERE ad.id = ? AND ad.activa = 1",
            asId
        );
        if (rows.isEmpty()) {
            res.redirect("/admin/comisiones/" + comisionId + "?error=" + encode("Asignación no encontrada."));
            return null;
        }
        Map asRow = rows.get(0);
        String cargoActual = (String) asRow.get("cargo");

        Map<String, Object> model = new HashMap<>();
        model.put("email", req.session().attribute("email"));
        model.put("comisionId", comisionId);
        model.put("asId", asId);
        model.put("comisionNombre", comision.get("nombre"));
        model.put("docenteNombre", asRow.get("nombre") + " " + asRow.get("apellido"));
        model.put("cargoActual", cargoActual);
        model.put("cargoActualLabel", ComisionService.cargoLabel(cargoActual));
        model.put("esTitular", "TITULAR".equals(cargoActual));
        model.put("esJTP", "JTP".equals(cargoActual));
        model.put("esAyudante", "AYUDANTE".equals(cargoActual));

        String err = req.queryParams("error");
        if (err != null && !err.isEmpty()) model.put("errorMessage", err);

        return new ModelAndView(model, "cambiar_cargo_form.mustache");
    }

    public Object cambiarCargo(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        int comisionId = parseInt(req.params("id"));
        int asId = parseInt(req.params("asId"));
        String nuevoCargo = req.queryParams("cargo");

        Map<String, Object> result = asignacionService.cambiarCargo(asId, nuevoCargo, adminId(req));

        if ((Boolean) result.get("success")) {
            String detalle = "{\"accion\":\"MODIFICACION_CARGO\",\"anterior\":\"" + result.get("cargoAnterior") +
                "\",\"nuevo\":\"" + result.get("cargoNuevo") + "\"}";
            AuditService.log(adminId(req), "MODIFICACION_CARGO_ASIGNACION", asId, detalle);
            res.redirect("/admin/comisiones/" + comisionId + "?message=" + encode((String) result.get("message")));
        } else {
            res.redirect("/admin/comisiones/" + comisionId + "/asignaciones/" + asId + "/cargo?error=" +
                encode((String) result.get("message")));
        }
        return "";
    }

    public Object bajaAsignacion(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        int comisionId = parseInt(req.params("id"));
        int asId = parseInt(req.params("asId"));

        Map<String, Object> result = asignacionService.bajaAsignacion(asId, adminId(req));

        if ((Boolean) result.get("success")) {
            AuditService.log(adminId(req), "BAJA_ASIGNACION", asId,
                "{\"accion\":\"BAJA\",\"comision\":" + comisionId + "}");
            res.redirect("/admin/comisiones/" + comisionId + "?message=" + encode((String) result.get("message")));
        } else {
            res.redirect("/admin/comisiones/" + comisionId + "?error=" + encode((String) result.get("message")));
        }
        return "";
    }

    // ── HELPERS ──────────────────────────────────────────────────

    private int parseInt(String val) {
        try { return Integer.parseInt(val); } catch (Exception e) { return 0; }
    }

    private int parseIntOrDefault(String val, int def) {
        try { return val != null && !val.isEmpty() ? Integer.parseInt(val) : def; }
        catch (NumberFormatException e) { return def; }
    }

    private Integer parseIntOrNull(String val) {
        try { return val != null && !val.isEmpty() ? Integer.parseInt(val) : null; }
        catch (NumberFormatException e) { return null; }
    }

    private String encode(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
}
