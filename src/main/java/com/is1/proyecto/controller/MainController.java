package com.is1.proyecto.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

/**
 * Controlador principal - Login, Dashboard y Logout.
 * Gestiona redirección según roles (N:N).
 */
public class MainController {

    /**
     * GET / - Página de login
     */
    public ModelAndView showLogin(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();

        String errorMessage = req.queryParams("error");
        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.put("errorMessage", errorMessage);
        }

        String successMessage = req.queryParams("message");
        if (successMessage != null && !successMessage.isEmpty()) {
            model.put("successMessage", successMessage);
        }

        return new ModelAndView(model, "login.mustache");
    }

    /**
     * GET /dashboard - Dashboard con selector de rol si hay múltiples
     */
    public ModelAndView showDashboard(Request req, Response res) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) req.session().attribute("roles");
        String email = req.session().attribute("email");

        if (email == null || roles == null || roles.isEmpty()) {
            res.redirect("/?error=Sesion inválida");
            return null;
        }

        // Si tiene múltiples roles, redirigir al primero
        if (roles.size() > 1) {
            if (roles.contains("ADMINISTRADOR")) {
                res.redirect("/admin");
            } else if (roles.contains("DOCENTE")) {
                res.redirect("/docente");
            } else if (roles.contains("ESTUDIANTE")) {
                res.redirect("/estudiante");
            } else {
                res.redirect("/?error=Rol desconocido");
            }
            return null;
        }

        // Si tiene solo 1 rol, redirigir directamente
        String rol = roles.get(0);
        if ("DOCENTE".equals(rol)) {
            res.redirect("/docente");
        } else if ("ESTUDIANTE".equals(rol)) {
            res.redirect("/estudiante");
        } else if ("ADMINISTRADOR".equals(rol)) {
            res.redirect("/admin");
        } else {
            res.redirect("/?error=Rol desconocido");
        }

        return null;
    }

    /**
     * GET /logout - Cierra sesión
     */
    public Object logout(Request req, Response res) {
        req.session().invalidate();
        res.removeCookie("token");
        res.redirect("/?message=Sesion cerrada exitosamente");
        return null;
    }
}
