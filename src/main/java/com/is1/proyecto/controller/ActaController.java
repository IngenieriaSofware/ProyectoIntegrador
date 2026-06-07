package com.is1.proyecto.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.service.ActaService;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

public class ActaController {

    private final ActaService actaService = new ActaService();
    private final ObjectMapper json = new ObjectMapper();

    // ── HELPERS ──────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<String> roles(Request req) {
        Object r = req.session().attribute("roles");
        return r != null ? (List<String>) r : List.of();
    }

    private int personaId(Request req) {
        Object id = req.session().attribute("personaId");
        return id != null ? ((Number) id).intValue() : 0;
    }

    private boolean esDocente(Request req) { return roles(req).contains("DOCENTE"); }
    private boolean esAdmin(Request req)   { return roles(req).contains("ADMINISTRADOR"); }

    private String jsonOk(Object data) {
        try { return json.writeValueAsString(data); }
        catch (Exception e) { return "{\"error\":\"Serialization error\"}"; }
    }

    private String jsonError(String msg) {
        return "{\"success\":false,\"message\":" + jsonQuote(msg) + "}";
    }

    private String jsonQuote(String s) {
        try { return json.writeValueAsString(s); }
        catch (Exception e) { return "\"" + s.replace("\"","\\\"") + "\""; }
    }

    private int parseIntOrDefault(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private Integer parseIntOrNull(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return Integer.parseInt(s); } catch (Exception e) { return null; }
    }

    // ── API: GET /api/actas ───────────────────────────────────────

    public Object listarActas(Request req, Response res) {
        res.type("application/json");
        List<String> rolesList = roles(req);
        int pid = personaId(req);

        if (pid == 0 || rolesList.isEmpty()) {
            res.status(401);
            return jsonError("No autenticado.");
        }

        Integer comisionId = parseIntOrNull(req.queryParams("comision_id"));
        Integer periodoId  = parseIntOrNull(req.queryParams("periodo_id"));
        String estado      = req.queryParams("estado");
        String instancia   = req.queryParams("instancia");

        String rol = rolesList.contains("ADMINISTRADOR") ? "ADMINISTRADOR" : "DOCENTE";
        List<Map<String, Object>> actas = actaService.listarActas(
            comisionId, periodoId, estado, instancia, pid, rol
        );
        return jsonOk(actas);
    }

    // ── API: POST /api/actas ──────────────────────────────────────

    public Object crearActa(Request req, Response res) {
        res.type("application/json");
        if (!esDocente(req) && !esAdmin(req)) {
            res.status(403);
            return jsonError("Acceso denegado.");
        }

        int pid = personaId(req);
        Integer comisionId = parseIntOrNull(req.queryParams("comision_id"));
        Integer periodoId  = parseIntOrNull(req.queryParams("periodo_id"));
        String instancia   = req.queryParams("instancia");

        if (comisionId == null || periodoId == null || instancia == null) {
            res.status(422);
            return jsonError("Los campos comision_id, periodo_id e instancia son obligatorios.");
        }

        Map<String, Object> result = actaService.crearActa(comisionId, periodoId, instancia, pid);
        int status = result.containsKey("status") ? (int) result.get("status") : 200;
        res.status(status);
        return jsonOk(result);
    }

    // ── API: GET /api/actas/:id ───────────────────────────────────

    public Object getActaDetalle(Request req, Response res) {
        res.type("application/json");
        int pid = personaId(req);
        if (pid == 0) { res.status(401); return jsonError("No autenticado."); }

        int actaId;
        try { actaId = Integer.parseInt(req.params("id")); }
        catch (NumberFormatException e) { res.status(400); return jsonError("ID inválido."); }

        Map<String, Object> result = actaService.getActaDetalle(actaId, pid, roles(req));
        int status = result.containsKey("status") ? (int) result.get("status") : 200;
        res.status(status);
        return jsonOk(result);
    }

    // ── API: PUT /api/actas/:id/notas ─────────────────────────────

    @SuppressWarnings("unchecked")
    public Object guardarLoteNotas(Request req, Response res) {
        res.type("application/json");
        if (!esDocente(req) && !esAdmin(req)) {
            res.status(403);
            return jsonError("Acceso denegado.");
        }

        int pid = personaId(req);
        int actaId;
        try { actaId = Integer.parseInt(req.params("id")); }
        catch (NumberFormatException e) { res.status(400); return jsonError("ID inválido."); }

        List<Map<String, Object>> notasPayload;
        try {
            Map<String, Object> body = json.readValue(req.body(), Map.class);
            Object notasRaw = body.get("notas");
            if (!(notasRaw instanceof List)) {
                res.status(422);
                return jsonError("El campo 'notas' debe ser un array.");
            }
            notasPayload = (List<Map<String, Object>>) notasRaw;
        } catch (Exception e) {
            res.status(400);
            return jsonError("JSON inválido: " + e.getMessage());
        }

        Map<String, Object> result = actaService.guardarLoteNotas(actaId, notasPayload, pid);
        int status = result.containsKey("status") ? (int) result.get("status") : 200;
        res.status(status);
        return jsonOk(result);
    }

    // ── API: POST /api/actas/:id/cerrar ───────────────────────────

    public Object cerrarActa(Request req, Response res) {
        res.type("application/json");
        if (!esDocente(req)) {
            res.status(403);
            return jsonError("Acceso denegado.");
        }

        int pid = personaId(req);
        int actaId;
        try { actaId = Integer.parseInt(req.params("id")); }
        catch (NumberFormatException e) { res.status(400); return jsonError("ID inválido."); }

        Map<String, Object> result = actaService.cerrarActa(actaId, pid);
        int status = result.containsKey("status") ? (int) result.get("status") : 200;
        res.status(status);
        return jsonOk(result);
    }

    // ── API: POST /api/actas/:id/reabrir ──────────────────────────

    public Object reabrirActa(Request req, Response res) {
        res.type("application/json");
        if (!esAdmin(req)) {
            res.status(403);
            return jsonError("Solo el Administrador puede reabrir actas.");
        }

        int pid = personaId(req);
        int actaId;
        try { actaId = Integer.parseInt(req.params("id")); }
        catch (NumberFormatException e) { res.status(400); return jsonError("ID inválido."); }

        String motivo;
        try {
            Map<String, Object> body = json.readValue(req.body(), Map.class);
            motivo = (String) body.get("motivo");
        } catch (Exception e) {
            motivo = req.queryParams("motivo");
        }

        Map<String, Object> result = actaService.reabrirActa(actaId, pid, roles(req), motivo);
        int status = result.containsKey("status") ? (int) result.get("status") : 200;
        res.status(status);
        return jsonOk(result);
    }

    // ── API: GET /api/actas/:id/auditoria ─────────────────────────

    public Object getAuditoria(Request req, Response res) {
        res.type("application/json");
        if (!esAdmin(req)) {
            res.status(403);
            return jsonError("Solo el Administrador puede consultar la auditoría.");
        }

        int pid = personaId(req);
        int actaId;
        try { actaId = Integer.parseInt(req.params("id")); }
        catch (NumberFormatException e) { res.status(400); return jsonError("ID inválido."); }

        Map<String, Object> result = actaService.getAuditoria(actaId, pid, roles(req));
        int status = result.containsKey("status") ? (int) result.get("status") : 200;
        res.status(status);
        return jsonOk(result);
    }

    // ── WEB: GET /docente/actas ───────────────────────────────────

    public ModelAndView showActasList(Request req, Response res) {
        if (!esDocente(req) && !esAdmin(req)) {
            res.redirect("/?error=Acceso no autorizado");
            return null;
        }

        int pid = personaId(req);
        List<String> rolesList = roles(req);
        String rol = rolesList.contains("ADMINISTRADOR") ? "ADMINISTRADOR" : "DOCENTE";

        Integer filtroComisionId = parseIntOrNull(req.queryParams("comision_id"));
        String filtroEstado      = req.queryParams("estado");
        String filtroInstancia   = req.queryParams("instancia");

        List<Map<String, Object>> actas = actaService.listarActas(
            filtroComisionId, null, filtroEstado, filtroInstancia, pid, rol
        );

        Map<String, Object> model = new HashMap<>();
        model.put("email", req.session().attribute("email"));
        model.put("personaId", pid);
        model.put("actas", actas);
        model.put("totalActas", actas.size());
        model.put("comisiones", actaService.getComisionesParaSelector(pid, rol));
        model.put("instancias", actaService.getInstanciasParaSelector());
        model.put("filtroEstado", filtroEstado);
        model.put("filtroComisionId", filtroComisionId);
        model.put("filtroInstancia", filtroInstancia);

        String msg = req.queryParams("message");
        String err = req.queryParams("error");
        if (msg != null) model.put("successMessage", msg);
        if (err != null) model.put("errorMessage", err);

        return new ModelAndView(model, "actas_list.mustache");
    }

    // ── WEB: GET /docente/actas/:id ───────────────────────────────

    public ModelAndView showActaDetalle(Request req, Response res) {
        if (!esDocente(req) && !esAdmin(req)) {
            res.redirect("/?error=Acceso no autorizado");
            return null;
        }

        int pid = personaId(req);
        int actaId;
        try { actaId = Integer.parseInt(req.params("id")); }
        catch (NumberFormatException e) {
            res.redirect("/docente/actas?error=ID de acta inválido");
            return null;
        }

        Map<String, Object> result = actaService.getActaDetalle(actaId, pid, roles(req));
        if (!(Boolean) result.get("success")) {
            int status = result.containsKey("status") ? (int) result.get("status") : 500;
            if (status == 404) {
                res.redirect("/docente/actas?error=Acta no encontrada");
            } else {
                res.redirect("/docente/actas?error=" + result.get("message"));
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> acta = (Map<String, Object>) result.get("acta");

        Map<String, Object> model = new HashMap<>();
        model.put("email", req.session().attribute("email"));
        model.put("personaId", pid);
        model.putAll(acta);

        String msg = req.queryParams("message");
        String err = req.queryParams("error");
        if (msg != null) model.put("successMessage", msg);
        if (err != null) model.put("errorMessage", err);

        return new ModelAndView(model, "acta_detalle.mustache");
    }

    // ── WEB: POST /docente/actas/nueva ───────────────────────────

    public Object crearActaWeb(Request req, Response res) {
        if (!esDocente(req)) {
            res.redirect("/?error=Acceso no autorizado");
            return null;
        }

        int pid = personaId(req);
        Integer comisionId = parseIntOrNull(req.queryParams("comision_id"));
        Integer periodoId  = parseIntOrNull(req.queryParams("periodo_id"));
        String instancia   = req.queryParams("instancia");

        if (comisionId == null || periodoId == null || instancia == null || instancia.isEmpty()) {
            res.redirect("/docente/actas/nueva?error=Todos los campos son obligatorios");
            return null;
        }

        Map<String, Object> result = actaService.crearActa(comisionId, periodoId, instancia, pid);
        if ((Boolean) result.get("success")) {
            Object id = result.get("id");
            res.redirect("/docente/actas/" + id + "?message=Acta creada correctamente");
        } else {
            res.redirect("/docente/actas/nueva?error=" + result.get("message"));
        }
        return null;
    }

    // ── WEB: GET /docente/actas/nueva ─────────────────────────────

    public ModelAndView showNuevaActaForm(Request req, Response res) {
        if (!esDocente(req)) {
            res.redirect("/?error=Acceso no autorizado");
            return null;
        }

        int pid = personaId(req);
        Map<String, Object> model = new HashMap<>();
        model.put("email", req.session().attribute("email"));
        model.put("personaId", pid);
        model.put("comisiones", actaService.getComisionesParaSelector(pid, "DOCENTE"));
        model.put("periodos", actaService.getPeriodosParaSelector());
        model.put("instancias", actaService.getInstanciasParaSelector());

        String err = req.queryParams("error");
        if (err != null) model.put("errorMessage", err);

        return new ModelAndView(model, "acta_nueva.mustache");
    }

    // ── WEB: POST /docente/actas/:id/cerrar ───────────────────────

    public Object cerrarActaWeb(Request req, Response res) {
        if (!esDocente(req)) {
            res.redirect("/?error=Acceso no autorizado");
            return null;
        }

        int pid = personaId(req);
        int actaId;
        try { actaId = Integer.parseInt(req.params("id")); }
        catch (NumberFormatException e) {
            res.redirect("/docente/actas?error=ID inválido");
            return null;
        }

        Map<String, Object> result = actaService.cerrarActa(actaId, pid);
        if ((Boolean) result.get("success")) {
            res.redirect("/docente/actas/" + actaId + "?message=Acta cerrada correctamente");
        } else {
            res.redirect("/docente/actas/" + actaId + "?error=" + result.get("message"));
        }
        return null;
    }

    // ── WEB: POST /docente/actas/:id/guardar-notas ────────────────

    @SuppressWarnings("unchecked")
    public Object guardarNotasWeb(Request req, Response res) {
        if (!esDocente(req) && !esAdmin(req)) {
            res.redirect("/?error=Acceso no autorizado");
            return null;
        }

        int pid = personaId(req);
        int actaId;
        try { actaId = Integer.parseInt(req.params("id")); }
        catch (NumberFormatException e) {
            res.redirect("/docente/actas?error=ID inválido");
            return null;
        }

        // Build notas payload from form fields: nota_<estudianteId> and ausente_<estudianteId>
        String[] estudianteIds = req.queryParamsValues("estudiante_ids");
        List<Map<String, Object>> notasPayload = new ArrayList<>();

        if (estudianteIds != null) {
            for (String estIdStr : estudianteIds) {
                Map<String, Object> np = new HashMap<>();
                int estId = Integer.parseInt(estIdStr);
                np.put("estudiante_id", estId);

                String ausenteVal = req.queryParams("ausente_" + estId);
                boolean ausente = "on".equals(ausenteVal) || "true".equals(ausenteVal);
                np.put("ausente", ausente);

                if (!ausente) {
                    String valorStr = req.queryParams("nota_" + estId);
                    if (valorStr != null && !valorStr.isEmpty()) {
                        try { np.put("valor", Double.parseDouble(valorStr)); }
                        catch (NumberFormatException e) { np.put("valor", null); }
                    } else {
                        np.put("valor", null);
                    }
                } else {
                    np.put("valor", null);
                }

                notasPayload.add(np);
            }
        }

        Map<String, Object> result = actaService.guardarLoteNotas(actaId, notasPayload, pid);
        if ((Boolean) result.get("success")) {
            res.redirect("/docente/actas/" + actaId + "?message=Notas guardadas correctamente");
        } else {
            res.redirect("/docente/actas/" + actaId + "?error=" + result.get("message"));
        }
        return null;
    }
}
