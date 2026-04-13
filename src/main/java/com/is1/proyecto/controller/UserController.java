package com.is1.proyecto.controller;

import com.is1.proyecto.service.UserService;
import com.is1.proyecto.models.User;
import spark.Request;
import spark.Response;
import spark.ModelAndView;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para manejar las rutas relacionadas con usuarios.
 * Delega la lógica de negocio al UserService.
 */
public class UserController {

    private UserService userService;

    public UserController() {
        this.userService = new UserService();
    }

    /**
     * GET /user/create - Muestra el formulario de creación de cuenta
     */
    public ModelAndView showCreateForm(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();

        String successMessage = req.queryParams("message");
        if (successMessage != null && !successMessage.isEmpty()) {
            model.put("successMessage", successMessage);
        }

        String errorMessage = req.queryParams("error");
        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.put("errorMessage", errorMessage);
        }

        return new ModelAndView(model, "user_form.mustache");
    }

    /**
     * POST /user/new - Procesa el registro de un nuevo usuario
     */
    public Object registerUser(Request req, Response res) {
        String name = req.queryParams("name");
        String password = req.queryParams("password");

        // Usar el servicio para registrar el usuario
        Map<String, Object> result = userService.registerUser(name, password);

        if ((Boolean) result.get("success")) {
            res.status(201);
            String message = (String) result.get("message");
            String encodedMsg = URLEncoder.encode(message, StandardCharsets.UTF_8);
            res.redirect("/user/create?message=" + encodedMsg);
        } else {
            res.status(400);
            String message = (String) result.get("message");
            String encodedMsg = URLEncoder.encode(message, StandardCharsets.UTF_8);
            res.redirect("/user/create?error=" + encodedMsg);
        }

        return "";
    }

    /**
     * POST /login - Procesa el login del usuario
     */
    public ModelAndView loginUser(Request req, Response res) {
        String username = req.queryParams("username");
        String password = req.queryParams("password");

        Map<String, Object> model = new HashMap<>();

        // Usar el servicio para autenticar el usuario
        Map<String, Object> authResult = userService.authenticateUser(username, password);

        if ((Boolean) authResult.get("success")) {
            res.status(200);
            User user = (User) authResult.get("user");

            // Gestionar sesión
            req.session(true).attribute("currentUserUsername", username);
            req.session().attribute("userId", user.getId());
            req.session().attribute("loggedIn", true);

            System.out.println("DEBUG: Login exitoso para la cuenta: " + username);
            System.out.println("DEBUG: ID de Sesión: " + req.session().id());

            model.put("username", username);
            return new ModelAndView(model, "dashboard.mustache");
        } else {
            res.status(401);
            String message = (String) authResult.get("message");
            model.put("errorMessage", message);
            System.out.println("DEBUG: Intento de login fallido para: " + username);
            return new ModelAndView(model, "login.mustache");
        }
    }

    /**
     * POST /add_users - Endpoint API para agregar usuarios (devuelve JSON)
     */
    public String addUserAPI(Request req, Response res) {
        res.type("application/json");

        String name = req.queryParams("name");
        String password = req.queryParams("password");

        // Usar el servicio para registrar el usuario
        Map<String, Object> result = userService.registerUser(name, password);

        if ((Boolean) result.get("success")) {
            res.status(201);
            return jsonResponse(Map.of(
                    "message", result.get("message"),
                    "id", result.get("userId")
            ));
        } else {
            res.status(400);
            return jsonResponse(Map.of("error", result.get("message")));
        }
    }

    /**
     * Método auxiliar para convertir un mapa a JSON
     */
    private String jsonResponse(Map<String, Object> map) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{\"error\": \"Error al procesar la solicitud\"}";
        }
    }
}
