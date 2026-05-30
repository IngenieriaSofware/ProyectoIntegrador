package com.is1.proyecto.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

/**
 * Vista genérica "En construcción".
 */
public class EnConstruccionController {
    public ModelAndView showEnConstruccion(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();

        // Leer el origen, con fallback por rol si no viene el param
        String from = req.queryParams("from");

        if (from != null && !from.isEmpty()) {
            model.put("dashboardUrl", from);
        } else {
            // Fallback: inferir por rol (por si alguien entra directo a /en_construccion)
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) req.session().attribute("roles");
            String dashboardUrl = "/dashboard";
            if (roles != null) {
                if (roles.contains("ADMINISTRADOR"))   dashboardUrl = "/admin";
                else if (roles.contains("DOCENTE"))    dashboardUrl = "/docente";
                else if (roles.contains("ESTUDIANTE")) dashboardUrl = "/estudiante";
            }
            model.put("dashboardUrl", dashboardUrl);
        }

        return new ModelAndView(model, "en_construccion.mustache");
    }
}
