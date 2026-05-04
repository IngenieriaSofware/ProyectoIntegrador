package com.is1.proyecto.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
 
import com.is1.proyecto.models.User;
import com.is1.proyecto.service.AuthService;
import com.is1.proyecto.service.UserService;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

/**
 * Controlador para manejar las rutas relacionadas con usuarios.
 * Delega la lógica de negocio al UserService.
 */
public class UserController {

    private UserService userService;
    private AuthService authService;

    public UserController() {
        this.userService = new UserService();
        this.authService = new AuthService();
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
        String role = req.queryParams("role");

        // Usar el servicio para registrar el usuario (con saneamiento y bcrypt)
        Map<String, Object> result = userService.registerUser(name, password, role);

        if ((Boolean) result.get("success")) {
            res.status(201);
            String message = (String) result.get("message");
            String encodedMsg = URLEncoder.encode(message, StandardCharsets.UTF_8);
            
            // CAMBIO: Redirige al login (/) tras el éxito
            res.redirect("/?message=" + encodedMsg);
        } else {
            res.status(400);
            String message = (String) result.get("message");
            String encodedMsg = URLEncoder.encode(message, StandardCharsets.UTF_8);
            
            // Mantiene en el formulario si hay error (ej: el nombre ya existe)
            res.redirect("/user/create?error=" + encodedMsg);
        }

        return "";
    }

    /**
     * POST /login - Procesa el login del usuario
     */
    public Object loginUser(Request req, Response res) {
        String username = req.queryParams("username");
        String password = req.queryParams("password");

        // Usar el servicio para autenticar el usuario
        Map<String, Object> authResult = userService.authenticateUser(username, password, authService);

        if ((Boolean) authResult.get("success")) {
            res.status(200);
            User user = (User) authResult.get("user");
            String token = (String) authResult.get("token");

            // Establecer token en cookie httpOnly
            res.cookie("token", token, 86400, true, true);

            // Gestionar sesión
            req.session(true).attribute("currentUserUsername", username);
            req.session().attribute("userId", user.getId());
            req.session().attribute("loggedIn", true);
            req.session().attribute("userRole", user.getRole());

            System.out.println("DEBUG: Login exitoso para la cuenta: " + username);
            System.out.println("DEBUG: ID de Sesión: " + req.session().id());

            // Redirigir según rol
            String role = user.getRole();
            switch (role) {
                case "ADMIN":
                    res.redirect("/admin");
                    break;
                case "DOCENTE":
                    res.redirect("/docente");
                    break;
                case "ESTUDIANTE":
                    res.redirect("/estudiante");
                    break;
                default:
                    res.redirect("/dashboard");
            }

            return "";
        } else {
            res.status(401);
            String message = (String) authResult.get("message");
            String encodedMsg = URLEncoder.encode(message, StandardCharsets.UTF_8);
            System.out.println("DEBUG: Intento de login fallido para: " + username);
            res.redirect("/?error=" + encodedMsg);
            return "";
        }
    }

    /**
     * POST /add_users - Endpoint API para agregar usuarios (devuelve JSON)
     */
    public String addUserAPI(Request req, Response res) {
        res.type("application/json");

        String name = req.queryParams("name");
        String password = req.queryParams("password");
        String role = req.queryParams("role");

        // Usar el servicio para registrar el usuario
        Map<String, Object> result = userService.registerUser(name, password, role);

        if ((Boolean) result.get("success")) {
            res.status(201);
            return jsonResponse(Map.of(
                    "message", result.get("message"),
                    "id", result.get("userId"),
                    "role", result.get("role")
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
