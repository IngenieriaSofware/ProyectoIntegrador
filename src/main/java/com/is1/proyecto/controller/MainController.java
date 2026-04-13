package com.is1.proyecto.controller;

import spark.Request;
import spark.Response;
import spark.ModelAndView;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para manejar las rutas principales de la aplicación.
 * Maneja login, dashboard y logout.
 */
public class MainController {

    /**
     * GET / - Muestra la página de login
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
     * GET /dashboard - Muestra el dashboard del usuario logueado
     */
    public ModelAndView showDashboard(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();

        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");

        // Verificar si el usuario está autenticado
        if (currentUsername == null || loggedIn == null || !loggedIn) {
            System.out.println("DEBUG: Acceso no autorizado a /dashboard. Redirigiendo a /login.");
            res.redirect("/?error=Debes iniciar sesión para acceder al dashboard.");
            return null;
        }

        model.put("username", currentUsername);
        return new ModelAndView(model, "dashboard.mustache");
    }

    /**
     * GET /logout - Cierra la sesión del usuario
     */
    public Object logout(Request req, Response res) {
        // Invalidar la sesión
        req.session().invalidate();

        System.out.println("DEBUG: Sesión cerrada. Redirigiendo a /login.");
        res.redirect("/");

        return null;
    }

    /**
     * GET /user/new - Muestra el formulario de creación de usuario (alias)
     */
    public ModelAndView showUserCreateForm(Request req, Response res) {
        return new ModelAndView(new HashMap<>(), "user_form.mustache");
    }
}
