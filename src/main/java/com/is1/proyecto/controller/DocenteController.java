package com.is1.proyecto.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.is1.proyecto.service.AsignacionDocenteService;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

public class DocenteController {

    private final AsignacionDocenteService asignacionService = new AsignacionDocenteService();

    public ModelAndView showDocenteDashboard(Request req, Response res) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) req.session().attribute("roles");
        String email = req.session().attribute("email");
        Integer personaId = (Integer) req.session().attribute("personaId");

        if (email == null || roles == null || !roles.contains("DOCENTE")) {
            res.redirect("/?error=Acceso no autorizado");
            return null;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("email", email);
        model.put("personaId", personaId);
        model.put("roles", roles);
        model.put("isDocente", true);

        return new ModelAndView(model, "docente_dashboard.mustache");
    }

    // Flujo 5.4: docente consulta sus comisiones asignadas (solo lectura, RN-13)
    public ModelAndView showMisComisiones(Request req, Response res) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) req.session().attribute("roles");
        String email = req.session().attribute("email");
        Integer personaId = (Integer) req.session().attribute("personaId");

        if (email == null || roles == null || !roles.contains("DOCENTE")) {
            res.redirect("/?error=Acceso no autorizado");
            return null;
        }

        List<Map<String, Object>> comisiones = asignacionService.getMisComisiones(personaId);

        Map<String, Object> model = new HashMap<>();
        model.put("email", email);
        model.put("comisiones", comisiones);
        model.put("sinComisiones", comisiones.isEmpty());

        return new ModelAndView(model, "docente_comisiones.mustache");
    }
}