package com.is1.proyecto.controller;

import spark.Request;
import spark.Response;
import spark.ModelAndView;
import java.util.HashMap;
import java.util.Map;

public class DocenteController {

    public ModelAndView showDocenteDashboard(Request req, Response res) {
        // Verificar autenticación y rol
        String username = req.session().attribute("currentUserUsername");
        String role = req.session().attribute("userRole");

        if (username == null || !"DOCENTE".equals(role)) {
            res.redirect("/?error=Acceso no autorizado");
            return null;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("username", username);
        model.put("role", "DOCENTE");
        model.put("isDocente", true);

        return new ModelAndView(model, "dashboard.mustache");
    }
}