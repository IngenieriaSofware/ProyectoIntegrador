package com.is1.proyecto.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.is1.proyecto.service.AuditService;
import com.is1.proyecto.service.PeriodoService;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

public class PeriodoController {

    private final PeriodoService periodoService = new PeriodoService();

    private boolean esAdmin(Request req) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) req.session().attribute("roles");
        return roles != null && roles.contains("ADMINISTRADOR");
    }

    private int adminId(Request req) {
        Object id = req.session().attribute("personaId");
        return id != null ? ((Number) id).intValue() : 0;
    }

    public ModelAndView listPeriodos(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        Map<String, Object> model = new HashMap<>();
        model.put("email", req.session().attribute("email"));
        model.put("periodos", periodoService.listarPeriodos());

        String msg = req.queryParams("message");
        String err = req.queryParams("error");
        if (msg != null && !msg.isEmpty()) model.put("successMessage", msg);
        if (err != null && !err.isEmpty()) model.put("errorMessage", err);

        return new ModelAndView(model, "periodos_list.mustache");
    }

    public ModelAndView showPeriodoForm(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        Map<String, Object> model = new HashMap<>();
        model.put("email", req.session().attribute("email"));

        String err = req.queryParams("error");
        if (err != null && !err.isEmpty()) model.put("errorMessage", err);

        return new ModelAndView(model, "periodo_form.mustache");
    }

    public Object createPeriodo(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        int anio = parseIntOrDefault(req.queryParams("anio"), 0);
        int cuatrimestre = parseIntOrDefault(req.queryParams("cuatrimestre"), 0);
        String descripcion = req.queryParams("descripcion");

        Map<String, Object> result = periodoService.crearPeriodo(anio, cuatrimestre, descripcion);

        if ((Boolean) result.get("success")) {
            AuditService.log(adminId(req), "CREAR_PERIODO", ((Number) result.get("id")).intValue(),
                "Período " + anio + "-" + cuatrimestre);
            res.redirect("/admin/periodos?message=" + encode((String) result.get("message")));
        } else {
            res.redirect("/admin/periodos/nuevo?error=" + encode((String) result.get("message")));
        }
        return "";
    }

    public Object activarPeriodo(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        int id = parseInt(req.params("id"));
        Map<String, Object> result = periodoService.activarPeriodo(id);

        if ((Boolean) result.get("success")) {
            AuditService.log(adminId(req), "ACTIVAR_PERIODO", id, "Período activado");
            res.redirect("/admin/periodos?message=" + encode((String) result.get("message")));
        } else {
            res.redirect("/admin/periodos?error=" + encode((String) result.get("message")));
        }
        return "";
    }

    public Object desactivarPeriodo(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        int id = parseInt(req.params("id"));
        Map<String, Object> result = periodoService.desactivarPeriodo(id);

        if ((Boolean) result.get("success")) {
            AuditService.log(adminId(req), "DESACTIVAR_PERIODO", id, "Período desactivado");
            res.redirect("/admin/periodos?message=" + encode((String) result.get("message")));
        } else {
            res.redirect("/admin/periodos?error=" + encode((String) result.get("message")));
        }
        return "";
    }

    private int parseInt(String val) {
        try { return Integer.parseInt(val); } catch (Exception e) { return 0; }
    }

    private int parseIntOrDefault(String val, int def) {
        try { return val != null && !val.isEmpty() ? Integer.parseInt(val) : def; }
        catch (NumberFormatException e) { return def; }
    }

    private String encode(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
}
