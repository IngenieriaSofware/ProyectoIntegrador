package com.is1.proyecto.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.is1.proyecto.service.AuditService;
import com.is1.proyecto.service.CarreraService;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

public class CarreraController {

    private final CarreraService carreraService = new CarreraService();

    // ── AUTH HELPER ──────────────────────────────────────────────

    private boolean esAdmin(Request req) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) req.session().attribute("roles");
        return roles != null && roles.contains("ADMINISTRADOR");
    }

    private Integer adminId(Request req) {
        return (Integer) req.session().attribute("personaId");
    }

    // ── CARRERAS ─────────────────────────────────────────────────

    public ModelAndView listCarreras(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=Acceso no autorizado"); return null; }

        int page = parseIntOrDefault(req.queryParams("page"), 1);
        Map<String, Object> data = carreraService.listarCarreras(page);
        data.put("email", req.session().attribute("email"));

        String msg = req.queryParams("message");
        String err = req.queryParams("error");
        if (msg != null && !msg.isEmpty()) data.put("successMessage", msg);
        if (err != null && !err.isEmpty()) data.put("errorMessage", err);

        return new ModelAndView(data, "carreras_list.mustache");
    }

    public ModelAndView showCarreraForm(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=Acceso no autorizado"); return null; }
        Map<String, Object> model = new HashMap<>();
        model.put("email", req.session().attribute("email"));
        model.put("accion", "Crear");
        model.put("formAction", "/admin/carreras/nueva");
        return new ModelAndView(model, "carrera_form.mustache");
    }

    public Object createCarrera(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=Acceso no autorizado"); return null; }

        String codigo = req.queryParams("codigo");
        String nombre = req.queryParams("nombre");
        String descripcion = req.queryParams("descripcion");

        Map<String, Object> result = carreraService.crearCarrera(codigo, nombre, descripcion);

        if ((Boolean) result.get("success")) {
            AuditService.log(adminId(req), "CREAR_CARRERA", ((Number) result.get("id")).intValue(), "Carrera: " + nombre);
            res.redirect("/admin/carreras/" + result.get("id") + "?message=" + encode("Carrera creada correctamente."));
        } else {
            res.redirect("/admin/carreras/nueva?error=" + encode((String) result.get("message")));
        }
        return "";
    }

    public ModelAndView showCarreraDetalle(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=Acceso no autorizado"); return null; }

        int id = Integer.parseInt(req.params("id"));
        Map<String, Object> model = carreraService.getCarreraDetalle(id);
        if (model == null) { res.redirect("/admin/carreras?error=" + encode("Carrera no encontrada.")); return null; }

        model.put("email", req.session().attribute("email"));
        String msg = req.queryParams("message");
        String err = req.queryParams("error");
        if (msg != null && !msg.isEmpty()) model.put("successMessage", msg);
        if (err != null && !err.isEmpty()) model.put("errorMessage", err);

        return new ModelAndView(model, "carrera_detail.mustache");
    }

    public ModelAndView showEditCarreraForm(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=Acceso no autorizado"); return null; }

        int id = Integer.parseInt(req.params("id"));
        Map<String, Object> carrera = carreraService.getCarreraDetalle(id);
        if (carrera == null) { res.redirect("/admin/carreras"); return null; }

        Map<String, Object> model = new HashMap<>();
        model.put("email", req.session().attribute("email"));
        model.put("accion", "Editar");
        model.put("formAction", "/admin/carreras/" + id + "/editar");
        model.put("id", id);
        model.put("codigo", carrera.get("codigo"));
        model.put("nombre", carrera.get("nombre"));
        model.put("descripcion", carrera.get("descripcion"));

        String err = req.queryParams("error");
        if (err != null && !err.isEmpty()) model.put("errorMessage", err);

        return new ModelAndView(model, "carrera_form.mustache");
    }

    public Object updateCarrera(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=Acceso no autorizado"); return null; }

        int id = Integer.parseInt(req.params("id"));
        String nombre = req.queryParams("nombre");
        String descripcion = req.queryParams("descripcion");

        Map<String, Object> result = carreraService.editarCarrera(id, nombre, descripcion);

        if ((Boolean) result.get("success")) {
            AuditService.log(adminId(req), "EDITAR_CARRERA", id, "Nombre: " + nombre);
            res.redirect("/admin/carreras/" + id + "?message=" + encode("Carrera actualizada correctamente."));
        } else {
            res.redirect("/admin/carreras/" + id + "/editar?error=" + encode((String) result.get("message")));
        }
        return "";
    }

    public Object darDeBajaCarrera(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=Acceso no autorizado"); return null; }

        int id = Integer.parseInt(req.params("id"));
        Map<String, Object> result = carreraService.darDeBajaCarrera(id);

        if ((Boolean) result.get("success")) {
            AuditService.log(adminId(req), "BAJA_CARRERA", id, "Baja lógica de carrera");
            res.redirect("/admin/carreras?message=" + encode("Carrera dada de baja correctamente."));
        } else {
            res.redirect("/admin/carreras/" + id + "?error=" + encode((String) result.get("message")));
        }
        return "";
    }

    // ── PLANES DE ESTUDIO ────────────────────────────────────────

    public ModelAndView showPlanForm(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=Acceso no autorizado"); return null; }

        int carreraId = Integer.parseInt(req.params("id"));
        Map<String, Object> carrera = carreraService.getCarreraDetalle(carreraId);
        if (carrera == null) { res.redirect("/admin/carreras"); return null; }

        Map<String, Object> model = new HashMap<>();
        model.put("email", req.session().attribute("email"));
        model.put("carreraId", carreraId);
        model.put("carreraNombre", carrera.get("nombre"));
        model.put("accion", "Crear");
        model.put("formAction", "/admin/carreras/" + carreraId + "/planes/nuevo");

        String err = req.queryParams("error");
        if (err != null && !err.isEmpty()) model.put("errorMessage", err);

        return new ModelAndView(model, "plan_form.mustache");
    }

    public Object createPlan(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=Acceso no autorizado"); return null; }

        int carreraId = Integer.parseInt(req.params("id"));
        int anioVigencia = parseIntOrDefault(req.queryParams("anioVigencia"), 0);
        String descripcion = req.queryParams("descripcion");
        Integer duracionAnios = parseIntOrNull(req.queryParams("duracionAnios"));

        Map<String, Object> result = carreraService.crearPlan(carreraId, anioVigencia, descripcion, duracionAnios);

        if ((Boolean) result.get("success")) {
            AuditService.log(adminId(req), "CREAR_PLAN", ((Number) result.get("id")).intValue(),
                "Plan " + anioVigencia + " en carrera " + carreraId);
            res.redirect("/admin/carreras/" + carreraId + "?message=" + encode((String) result.get("message")));
        } else {
            res.redirect("/admin/carreras/" + carreraId + "/planes/nuevo?error=" + encode((String) result.get("message")));
        }
        return "";
    }

    public ModelAndView showEditPlanForm(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=Acceso no autorizado"); return null; }

        int carreraId = Integer.parseInt(req.params("id"));
        int planId = Integer.parseInt(req.params("planId"));
        Map<String, Object> plan = carreraService.getPlanById(planId);
        Map<String, Object> carrera = carreraService.getCarreraDetalle(carreraId);
        if (plan == null || carrera == null) { res.redirect("/admin/carreras/" + carreraId); return null; }

        Map<String, Object> model = new HashMap<>();
        model.put("email", req.session().attribute("email"));
        model.put("carreraId", carreraId);
        model.put("carreraNombre", carrera.get("nombre"));
        model.put("accion", "Editar");
        model.put("formAction", "/admin/carreras/" + carreraId + "/planes/" + planId + "/editar");
        model.put("planId", planId);
        model.put("anioVigencia", plan.get("anioVigencia"));
        model.put("descripcion", plan.get("descripcion"));
        model.put("duracionAnios", plan.get("duracionAnios"));
        model.put("esEdicion", true);

        String err = req.queryParams("error");
        if (err != null && !err.isEmpty()) model.put("errorMessage", err);

        return new ModelAndView(model, "plan_form.mustache");
    }

    public Object updatePlan(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=Acceso no autorizado"); return null; }

        int carreraId = Integer.parseInt(req.params("id"));
        int planId = Integer.parseInt(req.params("planId"));
        String descripcion = req.queryParams("descripcion");
        Integer duracionAnios = parseIntOrNull(req.queryParams("duracionAnios"));

        Map<String, Object> result = carreraService.editarPlan(planId, descripcion, duracionAnios);

        if ((Boolean) result.get("success")) {
            AuditService.log(adminId(req), "EDITAR_PLAN", planId, "Plan editado en carrera " + carreraId);
            res.redirect("/admin/carreras/" + carreraId + "?message=" + encode("Plan actualizado correctamente."));
        } else {
            res.redirect("/admin/carreras/" + carreraId + "/planes/" + planId + "/editar?error=" + encode((String) result.get("message")));
        }
        return "";
    }

    public Object activarPlan(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=Acceso no autorizado"); return null; }

        int carreraId = Integer.parseInt(req.params("id"));
        int planId = Integer.parseInt(req.params("planId"));
        Map<String, Object> result = carreraService.activarPlan(planId);

        if ((Boolean) result.get("success")) {
            AuditService.log(adminId(req), "ACTIVAR_PLAN", planId, "Plan activado en carrera " + carreraId);
            res.redirect("/admin/carreras/" + carreraId + "?message=" + encode((String) result.get("message")));
        } else {
            res.redirect("/admin/carreras/" + carreraId + "?error=" + encode((String) result.get("message")));
        }
        return "";
    }

    public Object desactivarPlan(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=Acceso no autorizado"); return null; }

        int carreraId = Integer.parseInt(req.params("id"));
        int planId = Integer.parseInt(req.params("planId"));
        Map<String, Object> result = carreraService.desactivarPlan(planId);

        if ((Boolean) result.get("success")) {
            AuditService.log(adminId(req), "DESACTIVAR_PLAN", planId, "Plan desactivado en carrera " + carreraId);
            res.redirect("/admin/carreras/" + carreraId + "?message=" + encode((String) result.get("message")));
        } else {
            res.redirect("/admin/carreras/" + carreraId + "?error=" + encode((String) result.get("message")));
        }
        return "";
    }

    // ── HELPERS ──────────────────────────────────────────────────

    private int parseIntOrDefault(String val, int def) {
        try { return val != null && !val.isEmpty() ? Integer.parseInt(val) : def; }
        catch (NumberFormatException e) { return def; }
    }

    private Integer parseIntOrNull(String val) {
        try { return val != null && !val.isEmpty() ? Integer.parseInt(val) : null; }
        catch (NumberFormatException e) { return null; }
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
