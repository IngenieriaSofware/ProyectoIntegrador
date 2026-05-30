package com.is1.proyecto.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

/**
 * Controlador para dashboard de Estudiante.
 */
public class EstudianteController {

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
}