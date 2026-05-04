package com.is1.proyecto.controller;

import spark.Request;
import spark.Response;
import spark.ModelAndView;
import java.util.HashMap;
import java.util.Map;

public class EstudianteController {

    public ModelAndView showEstudianteDashboard(Request req, Response res) {
        // Verificar autenticación y rol
        String username = req.session().attribute("currentUserUsername");
        String role = req.session().attribute("userRole");

        if (username == null || !"ESTUDIANTE".equals(role)) {
            res.redirect("/?error=Acceso no autorizado");
            return null;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("username", username);
        model.put("role", "ESTUDIANTE");
        model.put("isEstudiante", true);

        return new ModelAndView(model, "dashboard.mustache");
    }
}