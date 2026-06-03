package com.is1.proyecto.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.is1.proyecto.service.AuditService;
import com.is1.proyecto.service.MateriaService;
import com.is1.proyecto.service.CorrelatividadService;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

public class MateriaController {

    private final MateriaService materiaService = new MateriaService();
    private final CorrelatividadService correlatividadService = new CorrelatividadService();

    // ── AUTH HELPERS ─────────────────────────────────────────────

    private boolean esAdmin(Request req) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) req.session().attribute("roles");
        return roles != null && roles.contains("ADMINISTRADOR");
    }

    private int adminId(Request req) {
        Object id = req.session().attribute("personaId");
        return id != null ? ((Number) id).intValue() : 0;
    }

    // ── LISTADO ──────────────────────────────────────────────────

    public ModelAndView listMaterias(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        int page = parseIntOrDefault(req.queryParams("page"), 1);
        Integer carreraId = parseIntOrNull(req.queryParams("carreraId"));
        Integer planId = parseIntOrNull(req.queryParams("planId"));

        Map<String, Object> data = materiaService.listarMaterias(page, carreraId, planId, true);
        data.put("email", req.session().attribute("email"));
        data.put("carreras", materiaService.listarCarrerasParaFiltro());
        data.put("planes", materiaService.listarPlanesParaFiltro());
        data.put("filtroCarreraId", carreraId);
        data.put("filtroPlanId", planId);

        String msg = req.queryParams("message");
        String err = req.queryParams("error");
        if (msg != null && !msg.isEmpty()) data.put("successMessage", msg);
        if (err != null && !err.isEmpty()) data.put("errorMessage", err);

        return new ModelAndView(data, "materias_list.mustache");
    }

    // ── CREAR MATERIA ────────────────────────────────────────────

    public ModelAndView showMateriaForm(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        Map<String, Object> model = new HashMap<>();
        model.put("email", req.session().attribute("email"));
        model.put("accion", "Crear");
        model.put("formAction", "/admin/materias/nueva");

        String err = req.queryParams("error");
        if (err != null && !err.isEmpty()) model.put("errorMessage", err);

        return new ModelAndView(model, "materia_form.mustache");
    }

    public Object createMateria(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        String codigo = req.queryParams("codigo");
        String nombre = req.queryParams("nombre");
        String descripcion = req.queryParams("descripcion");

        Map<String, Object> result = materiaService.crearMateria(codigo, nombre, descripcion, adminId(req));

        if ((Boolean) result.get("success")) {
            int newId = ((Number) result.get("id")).intValue();
            AuditService.log(adminId(req), "CREAR_MATERIA", newId,
                "Codigo: " + codigo + ", Nombre: " + result.get("nombre"));
            res.redirect("/admin/materias/" + newId + "?message=" + encode("Materia creada correctamente."));
        } else {
            res.redirect("/admin/materias/nueva?error=" + encode((String) result.get("message")));
        }
        return "";
    }

    // ── DETALLE ──────────────────────────────────────────────────

    public ModelAndView showMateriaDetalle(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        int id = parseInt(req.params("id"));
        Map<String, Object> model = materiaService.getMateriaDetalle(id);
        if (model == null) {
            res.redirect("/admin/materias?error=" + encode("Materia no encontrada."));
            return null;
        }

        List<Map<String, Object>> asociaciones = (List<Map<String, Object>>) model.get("asociaciones");
        if (asociaciones != null) {
            for (Map<String, Object> asoc : asociaciones) {
                int mpId = ((Number) asoc.get("id")).intValue();
                
                Map<String, Object> datosGrafo = correlatividadService.getCorrelatividadesDelPlan(mpId);
                asoc.put("anteriores", datosGrafo.get("anteriores"));
                asoc.put("posteriores", datosGrafo.get("posteriores"));

                List<Map> materiasFiltradas = materiaService.listarMateriasDisponiblesParaCorrelativas(mpId, id);
                asoc.put("materiasDisponibles", materiasFiltradas);
            }
        }

        model.put("email", req.session().attribute("email"));
        String msg = req.queryParams("message");
        String err = req.queryParams("error");
        if (msg != null && !msg.isEmpty()) model.put("successMessage", msg);
        if (err != null && !err.isEmpty()) model.put("errorMessage", err);

        return new ModelAndView(model, "materia_detail.mustache");
    }

    // ── EDITAR MATERIA ───────────────────────────────────────────

    public ModelAndView showEditMateriaForm(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        int id = parseInt(req.params("id"));
        Map<String, Object> materia = materiaService.getMateriaById(id);
        if (materia == null) { res.redirect("/admin/materias"); return null; }

        Map<String, Object> model = new HashMap<>(materia);
        model.put("email", req.session().attribute("email"));
        model.put("accion", "Editar");
        model.put("formAction", "/admin/materias/" + id + "/editar");
        model.put("esEdicion", true);

        String err = req.queryParams("error");
        if (err != null && !err.isEmpty()) model.put("errorMessage", err);

        return new ModelAndView(model, "materia_form.mustache");
    }

    public Object updateMateria(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        int id = parseInt(req.params("id"));
        String nombre = req.queryParams("nombre");
        String descripcion = req.queryParams("descripcion");

        Map<String, Object> result = materiaService.editarMateria(id, nombre, descripcion, adminId(req));

        if ((Boolean) result.get("success")) {
            String detalle = "{\"anterior\":\"" + result.get("oldNombre") + "\",\"nuevo\":\"" + result.get("newNombre") + "\"}";
            AuditService.log(adminId(req), "EDITAR_MATERIA", id, detalle);
            res.redirect("/admin/materias/" + id + "?message=" + encode("Materia actualizada correctamente."));
        } else if (Boolean.TRUE.equals(result.get("noChanges"))) {
            res.redirect("/admin/materias/" + id + "?message=" + encode("No se realizaron modificaciones."));
        } else {
            res.redirect("/admin/materias/" + id + "/editar?error=" + encode((String) result.get("message")));
        }
        return "";
    }

    // ── DESACTIVAR / REACTIVAR MATERIA ───────────────────────────

    public Object desactivarMateria(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        int id = parseInt(req.params("id"));
        Map<String, Object> result = materiaService.desactivarMateria(id);

        if ((Boolean) result.get("success")) {
            AuditService.log(adminId(req), "DESACTIVAR_MATERIA", id, "Baja lógica");
            res.redirect("/admin/materias/" + id + "?message=" + encode((String) result.get("message")));
        } else {
            res.redirect("/admin/materias/" + id + "?error=" + encode((String) result.get("message")));
        }
        return "";
    }

    public Object reactivarMateria(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        int id = parseInt(req.params("id"));
        Map<String, Object> result = materiaService.reactivarMateria(id);

        if ((Boolean) result.get("success")) {
            AuditService.log(adminId(req), "REACTIVAR_MATERIA", id, "Reactivación");
            res.redirect("/admin/materias/" + id + "?message=" + encode((String) result.get("message")));
        } else {
            res.redirect("/admin/materias/" + id + "?error=" + encode((String) result.get("message")));
        }
        return "";
    }

    // ── ASOCIAR PLAN ─────────────────────────────────────────────

    public ModelAndView showAsociarPlanForm(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        int materiaId = parseInt(req.params("id"));
        Map<String, Object> materia = materiaService.getMateriaById(materiaId);
        if (materia == null) { res.redirect("/admin/materias"); return null; }

        Map<String, Object> model = new HashMap<>();
        model.put("email", req.session().attribute("email"));
        model.put("materiaId", materiaId);
        model.put("materiaNombre", materia.get("nombre"));
        model.put("materiaCodigo", materia.get("codigo"));
        model.put("accion", "Asociar");
        model.put("formAction", "/admin/materias/" + materiaId + "/planes/nueva");
        model.put("planes", materiaService.listarPlanesActivosParaSelector());

        String err = req.queryParams("error");
        if (err != null && !err.isEmpty()) model.put("errorMessage", err);

        return new ModelAndView(model, "materia_plan_form.mustache");
    }

    public Object asociarPlan(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        int materiaId = parseInt(req.params("id"));
        int planId = parseIntOrDefault(req.queryParams("planEstudioId"), 0);
        int anioPlan = parseIntOrDefault(req.queryParams("anioPlan"), 0);
        String cuatrimestre = req.queryParams("cuatrimestre");
        int cargaHoraria = parseIntOrDefault(req.queryParams("cargaHoraria"), 0);
        String caracter = req.queryParams("caracter");

        Map<String, Object> result = materiaService.asociarPlan(
            materiaId, planId, anioPlan, cuatrimestre, cargaHoraria, caracter, adminId(req)
        );

        if ((Boolean) result.get("success")) {
            int mpId = ((Number) result.get("id")).intValue();
            AuditService.log(adminId(req), "ASOCIAR_MATERIA_PLAN", mpId,
                "Materia " + materiaId + " asociada a Plan " + planId);
            res.redirect("/admin/materias/" + materiaId + "?message=" + encode("Materia asociada al plan correctamente."));
        } else {
            res.redirect("/admin/materias/" + materiaId + "/planes/nueva?error=" + encode((String) result.get("message")));
        }
        return "";
    }

    // ── EDITAR ASOCIACIÓN ────────────────────────────────────────

    public ModelAndView showEditAsociacionForm(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        int materiaId = parseInt(req.params("id"));
        int mpId = parseInt(req.params("mpId"));

        Map<String, Object> materia = materiaService.getMateriaById(materiaId);
        Map<String, Object> asoc = materiaService.getAsociacionById(mpId);
        if (materia == null || asoc == null) {
            res.redirect("/admin/materias/" + materiaId);
            return null;
        }

        Map<String, Object> model = new HashMap<>(asoc);
        model.put("email", req.session().attribute("email"));
        model.put("materiaId", materiaId);
        model.put("materiaNombre", materia.get("nombre"));
        model.put("materiaCodigo", materia.get("codigo"));
        model.put("accion", "Editar asociación");
        model.put("formAction", "/admin/materias/" + materiaId + "/planes/" + mpId + "/editar");
        model.put("esEdicion", true);

        String err = req.queryParams("error");
        if (err != null && !err.isEmpty()) model.put("errorMessage", err);

        return new ModelAndView(model, "materia_plan_form.mustache");
    }

    public Object updateAsociacion(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        int materiaId = parseInt(req.params("id"));
        int mpId = parseInt(req.params("mpId"));
        int anioPlan = parseIntOrDefault(req.queryParams("anioPlan"), 0);
        String cuatrimestre = req.queryParams("cuatrimestre");
        int cargaHoraria = parseIntOrDefault(req.queryParams("cargaHoraria"), 0);
        String caracter = req.queryParams("caracter");

        Map<String, Object> result = materiaService.editarAsociacion(mpId, anioPlan, cuatrimestre, cargaHoraria, caracter);

        if ((Boolean) result.get("success")) {
            AuditService.log(adminId(req), "EDITAR_ASOCIACION", mpId,
                "Asociación editada para materia " + materiaId);
            res.redirect("/admin/materias/" + materiaId + "?message=" + encode("Asociación actualizada correctamente."));
        } else {
            res.redirect("/admin/materias/" + materiaId + "/planes/" + mpId + "/editar?error=" + encode((String) result.get("message")));
        }
        return "";
    }

    // ── DESACTIVAR ASOCIACIÓN ────────────────────────────────────

    public Object desactivarAsociacion(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        int materiaId = parseInt(req.params("id"));
        int mpId = parseInt(req.params("mpId"));

        Map<String, Object> result = materiaService.desactivarAsociacion(mpId);

        if ((Boolean) result.get("success")) {
            AuditService.log(adminId(req), "DESACTIVAR_ASOCIACION", mpId,
                "Asociación desactivada para materia " + materiaId);
            res.redirect("/admin/materias/" + materiaId + "?message=" + encode((String) result.get("message")));
        } else {
            res.redirect("/admin/materias/" + materiaId + "?error=" + encode((String) result.get("message")));
        }
        return "";
    }

// ── REACTIVAR ASOCIACIÓN ────────────────────────────────────

    public Object reactivarAsociacion(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        int materiaId = parseInt(req.params("id"));
        int mpId = parseInt(req.params("mpId"));

        Map<String, Object> result = materiaService.reactivarAsociacion(mpId);

        if ((Boolean) result.get("success")) {
            AuditService.log(adminId(req), "REACTIVAR_ASOCIACION", mpId,
                "Asociación reactivada para materia " + materiaId);
            res.redirect("/admin/materias/" + materiaId + "?message=" + encode("Asociación reactivada correctamente."));
        } else {
            res.redirect("/admin/materias/" + materiaId + "?error=" + encode((String) result.get("message")));
        }
        return "";
    }

// ── ELIMINAR ASOCIACIÓN ────────────────────────────────────

    public Object eliminarAsociacionDefinitiva(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        int materiaId = parseInt(req.params("id"));
        int mpId = parseInt(req.params("mpId"));

        Map<String, Object> result = materiaService.eliminarAsociacionDefinitiva(mpId);

        if ((Boolean) result.get("success")) {
            AuditService.log(adminId(req), "ELIMINAR_ASOCIACION_DEFINITIVA", mpId,
                "Borrado físico de asociación y correlatividades para materia " + materiaId);
            res.redirect("/admin/materias/" + materiaId + "?message=" + encode((String) result.get("message")));
        } else {
            res.redirect("/admin/materias/" + materiaId + "?error=" + encode((String) result.get("message")));
        }
        return "";
    }

    // ── ACCIONES DE CORRELATIVIDADES ─────────────────────────────

    public Object crearCorrelatividad(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        int materiaId = parseInt(req.params("id")); 
        int destinoMpId = parseIntOrDefault(req.queryParams("destinoMateriaPlanId"), 0);
        int origenMpId = parseIntOrDefault(req.queryParams("materiaOrigenMpId"), 0);
        String condicion = req.queryParams("condicion");

        Map<String, Object> result = correlatividadService.agregarCorrelatividad(origenMpId, destinoMpId, condicion);

        if ((Boolean) result.get("success")) {
            AuditService.log(adminId(req), "CREAR_CORRELATIVIDAD", destinoMpId,
                "Asociacion Plan ID " + destinoMpId + " ahora requiere Origen Plan ID " + origenMpId + " (" + condicion + ")");
            res.redirect("/admin/materias/" + materiaId + "?message=" + encode((String) result.get("message")));
        } else {
            res.redirect("/admin/materias/" + materiaId + "?error=" + encode((String) result.get("message")));
        }
        return "";
    }

    public Object eliminarCorrelatividad(Request req, Response res) {
        if (!esAdmin(req)) { res.redirect("/?error=" + encode("Acceso no autorizado")); return null; }

        int materiaId = parseInt(req.params("id"));
        int correlatividadId = parseInt(req.params("corrId"));

        Map<String, Object> result = correlatividadService.eliminarCorrelatividad(correlatividadId);

        if ((Boolean) result.get("success")) {
            AuditService.log(adminId(req), "ELIMINAR_CORRELATIVIDAD", correlatividadId,
                "Se elimino regla de correlatividad ID: " + correlatividadId);
            res.redirect("/admin/materias/" + materiaId + "?message=" + encode((String) result.get("message")));
        } else {
            res.redirect("/admin/materias/" + materiaId + "?error=" + encode((String) result.get("message")));
        }
        return "";
    }

    // ── HELPERS ──────────────────────────────────────────────────

    private int parseInt(String val) {
        try { return Integer.parseInt(val); }
        catch (Exception e) { return 0; }
    }

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
