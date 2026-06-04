package com.is1.proyecto.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.is1.proyecto.models.Carrera;
import com.is1.proyecto.models.Estudiante;
import com.is1.proyecto.service.CarreraService;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

/**
 * Controlador para dashboard de Estudiante.
 */
public class EstudianteController {

    private final CarreraService carreraService = new CarreraService();

    public ModelAndView showEstudianteDashboard(Request req, Response res) {
        // Verificar autenticación y rol ESTUDIANTE
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) req.session().attribute("roles");
        String email = req.session().attribute("email");
        Integer personaId = (Integer) req.session().attribute("personaId");

        if (email == null || roles == null || !roles.contains("ESTUDIANTE")) {
            res.redirect("/?error=Acceso no autorizado");
            return null;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("email", email);
        model.put("personaId", personaId);
        model.put("roles", roles);
        model.put("isEstudiante", true);

        return new ModelAndView(model, "estudiante_dashboard.mustache");
    }

    public ModelAndView showInscripcionCarreras(Request req, Response res) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) req.session().attribute("roles");
        String email = req.session().attribute("email");

        if (email == null || roles == null || !roles.contains("ESTUDIANTE")) {
            res.redirect("/?error=Acceso no autorizado");
            return null;
        }

        Map<String, Object> model = carreraService.listarCarrerasVigentes();
        model.put("email", email);
        model.put("title", "Inscripción a Carrera");

        String msg = req.queryParams("message");
        String err = req.queryParams("error");
        if (msg != null && !msg.isEmpty()) model.put("successMessage", msg);
        if (err != null && !err.isEmpty()) model.put("errorMessage", err);

        return new ModelAndView(model, "estudiante_inscripcion_carreras.mustache");
    }

    public ModelAndView showInscripcionCarreraDetalle(Request req, Response res) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) req.session().attribute("roles");
        String email = req.session().attribute("email");
        Integer personaId = (Integer) req.session().attribute("personaId");

        if (email == null || roles == null || !roles.contains("ESTUDIANTE")) {
            res.redirect("/?error=Acceso no autorizado");
            return null;
        }

        int carreraId = parseIntOrDefault(req.params("id"), 0);
        Map<String, Object> model = carreraService.getCarreraConPlanesActivos(carreraId);
        if (model == null) {
            res.redirect("/estudiante/inscripcion/carreras?error=" + encode("Carrera no encontrada o inactiva."));
            return null;
        }

        // Verificar si ya está inscripto a esta carrera
        if (personaId != null) {
            Estudiante estudiante = Estudiante.findByPersonaId(personaId).orElse(null);
            if (estudiante != null && estudiante.getCarrera() != null) {
                Carrera carreraActual = Carrera.findById(carreraId);
                if (carreraActual != null && estudiante.getCarrera().equals(carreraActual.getNombre())) {
                    model.put("yaInscripto", true);
                    model.put("planEstudioActual", estudiante.getPlanEstudio());
                }
            }
        }

        model.put("email", email);
        model.put("titulo", "Planes de estudio vigentes");
        model.put("carreraId", carreraId);

        String msg = req.queryParams("message");
        String err = req.queryParams("error");
        if (msg != null && !msg.isEmpty()) model.put("successMessage", msg);
        if (err != null && !err.isEmpty()) model.put("errorMessage", err);

        return new ModelAndView(model, "estudiante_inscripcion_carrera_detalle.mustache");
    }

    public Object enrollCarrera(Request req, Response res) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) req.session().attribute("roles");
        String email = req.session().attribute("email");
        Integer personaId = (Integer) req.session().attribute("personaId");

        if (email == null || roles == null || !roles.contains("ESTUDIANTE") || personaId == null) {
            res.redirect("/?error=Acceso no autorizado");
            return null;
        }

        int carreraId = parseIntOrDefault(req.params("id"), 0);
        int planId = parseIntOrDefault(req.queryParams("planId"), 0);

        if (carreraId <= 0 || planId <= 0) {
            res.redirect("/estudiante/inscripcion/carreras/" + carreraId + "?error=" + encode("Debe seleccionar un plan de estudio válido."));
            return "";
        }

        Map<String, Object> result = carreraService.inscribirEstudiante(personaId, carreraId, planId);
        if ((Boolean) result.get("success")) {
            res.redirect("/estudiante/inscripcion/carreras/" + carreraId + "?message=" + encode((String) result.get("message")));
        } else {
            res.redirect("/estudiante/inscripcion/carreras/" + carreraId + "?error=" + encode((String) result.get("message")));
        }
        return "";
    }

    public ModelAndView showMisMaticas(Request req, Response res) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) req.session().attribute("roles");
        String email = req.session().attribute("email");
        Integer personaId = (Integer) req.session().attribute("personaId");

        if (email == null || roles == null || !roles.contains("ESTUDIANTE") || personaId == null) {
            res.redirect("/?error=Acceso no autorizado");
            return null;
        }

        Map<String, Object> model = carreraService.listarMateriasDisponibles(personaId);
        model.put("email", email);
        model.put("personaId", personaId);
        model.put("title", "Mis Materias - Seleccionar Cursos");

        String msg = req.queryParams("message");
        String err = req.queryParams("error");
        if (msg != null && !msg.isEmpty()) model.put("successMessage", msg);
        if (err != null && !err.isEmpty()) model.put("errorMessage", err);

        return new ModelAndView(model, "estudiante_mis_materias.mustache");
    }

    public Object inscribirseAComision(Request req, Response res) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) req.session().attribute("roles");
        String email = req.session().attribute("email");
        Integer personaId = (Integer) req.session().attribute("personaId");

        if (email == null || roles == null || !roles.contains("ESTUDIANTE") || personaId == null) {
            res.redirect("/?error=Acceso no autorizado");
            return null;
        }

        int comisionId = parseIntOrDefault(req.queryParams("comisionId"), 0);
        if (comisionId <= 0) {
            res.redirect("/estudiante/materias?error=" + encode("Comisión inválida."));
            return "";
        }

        Map<String, Object> result = carreraService.inscribirseAComision(personaId, comisionId);
        if ((Boolean) result.get("success")) {
            res.redirect("/estudiante/mis-cursos?message=" + encode((String) result.get("message")));
        } else {
            res.redirect("/estudiante/materias?error=" + encode((String) result.get("message")));
        }
        return "";
    }

    public ModelAndView showMisCursos(Request req, Response res) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) req.session().attribute("roles");
        String email = req.session().attribute("email");
        Integer personaId = (Integer) req.session().attribute("personaId");

        if (email == null || roles == null || !roles.contains("ESTUDIANTE") || personaId == null) {
            res.redirect("/?error=Acceso no autorizado");
            return null;
        }

        Map<String, Object> model = carreraService.obtenerMisCursos(personaId);
        model.put("email", email);
        model.put("personaId", personaId);
        model.put("title", "Mis Cursos");

        String msg = req.queryParams("message");
        String err = req.queryParams("error");
        if (msg != null && !msg.isEmpty()) model.put("successMessage", msg);
        if (err != null && !err.isEmpty()) model.put("errorMessage", err);

        return new ModelAndView(model, "estudiante_mis_cursos.mustache");
    }

    private int parseIntOrDefault(String value, int def) {
        try { return value != null && !value.isEmpty() ? Integer.parseInt(value) : def; }
        catch (NumberFormatException e) { return def; }
    }

    private String encode(String value) {
        return URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8);
    }
}
