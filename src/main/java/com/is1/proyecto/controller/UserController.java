package com.is1.proyecto.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.is1.proyecto.models.Persona;
import com.is1.proyecto.service.UserService;
import com.is1.proyecto.service.CarreraService;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

/**
 * Controlador para login y registro de Personas.
 * Gestiona autenticación Persona + asignación de roles.
 */
public class UserController {

    private UserService userService;
    private CarreraService carreraService;

    public UserController() {
        this.userService = new UserService();
        this.carreraService = new CarreraService();
    }

    /**
     * GET /user/create - Muestra formulario de registro (Persona)
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

        // Cargar carreras vigentes
        Map<String, Object> carrerasResult = carreraService.listarCarrerasVigentes();
        if ((Boolean) carrerasResult.get("success")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> carreras = (List<Map<String, Object>>) carrerasResult.get("carreras");
            model.put("carreras", carreras);
        }

        return new ModelAndView(model, "user_form.mustache");
    }

    /**
     * POST /user/new - Registra una nueva Persona
     */
    public Object registerPersona(Request req, Response res) {
        String dni = req.queryParams("dni");
        String nombre = req.queryParams("nombre");
        String apellido = req.queryParams("apellido");
        String email = req.queryParams("email");
        String password = req.queryParams("password");
        String telefono = req.queryParams("telefono");
        String localidad = req.queryParams("localidad");
        String rolInicial = req.queryParams("rol");
        String carreraIdStr = req.queryParams("carreraId");

        // Validar carreraId si el rol es ESTUDIANTE
        int carreraId = 0;
        if ("ESTUDIANTE".equals(rolInicial)) {
            if (carreraIdStr == null || carreraIdStr.trim().isEmpty()) {
                res.status(400);
                String msg = URLEncoder.encode("Debes seleccionar una carrera", StandardCharsets.UTF_8);
                res.redirect("/user/new?error=" + msg);
                return "";
            }
            try {
                carreraId = Integer.parseInt(carreraIdStr);
            } catch (NumberFormatException e) {
                res.status(400);
                String msg = URLEncoder.encode("Selección de carrera inválida", StandardCharsets.UTF_8);
                res.redirect("/user/new?error=" + msg);
                return "";
            }
        }

        // Registrar Persona
        Map<String, Object> result = userService.registerPersona(dni, nombre, apellido, email, 
                                                                 password, telefono, localidad, rolInicial, carreraId);

        if ((Boolean) result.get("success")) {
            res.status(201);
            String message = (String) result.get("message");
            String encodedMsg = URLEncoder.encode(message, StandardCharsets.UTF_8);
            
            // Redirigir al login tras registro exitoso
            res.redirect("/?message=" + encodedMsg);
        } else {
            res.status(400);
            String message = (String) result.get("message");
            String encodedMsg = URLEncoder.encode(message, StandardCharsets.UTF_8);
            
            // Mantener en formulario si hay error
            res.redirect("/user/create?error=" + encodedMsg);
        }

        return "";
    }

    /**
     * POST /login - Autentica una Persona por DNI o Email
     */
    public Object loginPersona(Request req, Response res) {
        String identifier = req.queryParams("identifier");  // DNI o Email
        String password = req.queryParams("password");

        // Autenticar Persona
        Map<String, Object> authResult = userService.authenticatePersona(identifier, password);

        if ((Boolean) authResult.get("success")) {
            res.status(200);
            Persona persona = (Persona) authResult.get("persona");
            String token = (String) authResult.get("token");
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) authResult.get("roles");

            // Establecer token en cookie httpOnly
            res.cookie("token", token, 86400, true, true);

            // Gestionar sesión
            req.session(true).attribute("personaId", persona.getId());
            req.session().attribute("email", persona.getEmail());
            req.session().attribute("roles", roles);
            req.session().attribute("loggedIn", true);

            // REDIRECCIÓN según roles
            if (roles.size() == 1) {
                // Único rol → redirigir directo
                String rol = roles.get(0);
                if ("DOCENTE".equals(rol)) {
                    res.redirect("/docente");
                } else if ("ESTUDIANTE".equals(rol)) {
                    res.redirect("/estudiante");
                } else if ("ADMINISTRADOR".equals(rol)) {
                    res.redirect("/admin");
                }
            } else if (roles.size() > 1) {
                // Múltiples roles → mostrar selector
                res.redirect("/dashboard?selectRole=true");
            }
        } else {
            res.status(401);
            String message = (String) authResult.get("message");
            String encodedMsg = URLEncoder.encode(message, StandardCharsets.UTF_8);
            
            // Volver al login con error
            res.redirect("/?error=" + encodedMsg);
        }

        return "";
    }

    /**
     * GET /logout - Cierra sesión
     */
    public Object logout(Request req, Response res) {
        req.session().invalidate();
        res.removeCookie("token");
        res.redirect("/?message=Sesion cerrada exitosamente");
        return "";
    }
}
