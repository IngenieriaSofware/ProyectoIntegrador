package com.is1.proyecto.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.service.AdminService;
import com.is1.proyecto.service.AuditService;
import com.is1.proyecto.service.UserService;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

/**
 * Controlador para gestión de Administrador.
 */
public class AdministradorController {

    private AdminService adminService;
    private UserService userService;

    public AdministradorController() {
        this.userService = new UserService();
        this.adminService = new AdminService();
    }

    public ModelAndView showAdministradorDashboard(Request req, Response res) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) req.session().attribute("roles");
        String email = req.session().attribute("email");
        Integer personaId = (Integer) req.session().attribute("personaId");

        if (email == null || roles == null || !roles.contains("ADMINISTRADOR")) {
            res.redirect("/?error=Acceso no autorizado");
            return null;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("email", email);
        model.put("personaId", personaId);
        model.put("roles", roles);
        model.put("isAdministrador", true);
        
        Map<String, Object> stats = adminService.getSystemStats();
        model.putAll(stats);

        return new ModelAndView(model, "admin_dashboard.mustache");
    }

    /**
     * Lista usuarios con paginación
     */
    public Object listUsuarios(Request req, Response res) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) req.session().attribute("roles");
        
        if (roles == null || !roles.contains("ADMINISTRADOR")) {
            res.status(403);
            return "{\"error\": \"Acceso denegado\"}";
        }

        try {
            int page = req.queryParams("page") != null ? Integer.parseInt(req.queryParams("page")) : 1;
            int pageSize = req.queryParams("pageSize") != null ? Integer.parseInt(req.queryParams("pageSize")) : 10;
            
            List<Map<String, Object>> usuarios = adminService.listPersonas(page, pageSize);
            
            res.type("application/json");
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(usuarios);
        } catch (Exception e) {
            res.status(500);
            res.type("application/json");
            return "{\"error\": \"Error al cargar usuarios: " + e.getMessage() + "\"}";
        }
    }

    /**
     * Busca usuarios
     */
    public Object searchUsuarios(Request req, Response res) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) req.session().attribute("roles");
        
        if (roles == null || !roles.contains("ADMINISTRADOR")) {
            res.status(403);
            return "{\"error\": \"Acceso denegado\"}";
        }

        try {
            String query = req.queryParams("q");
            if (query == null || query.isEmpty()) {
                res.type("application/json");
                return "[]";
            }
            
            List<Map<String, Object>> resultados = adminService.searchPersonas(query);
            
            res.type("application/json");
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(resultados);
        } catch (Exception e) {
            res.status(500);
            res.type("application/json");
            return "{\"error\": \"Error al buscar usuarios: " + e.getMessage() + "\"}";
        }
    }

    /**
     * Obtiene detalles de usuario
     */
    public Object getUsuarioDetails(Request req, Response res) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) req.session().attribute("roles");
        Integer adminId = (Integer) req.session().attribute("personaId");
        
        if (roles == null || !roles.contains("ADMINISTRADOR")) {
            res.status(403);
            return "{\"error\": \"Acceso denegado\"}";
        }

        int userId = Integer.parseInt(req.params("id"));
        Map<String, Object> detalles = adminService.getPersonaDetails(userId);
        
        if (detalles == null) {
            res.status(404);
            return "{\"error\": \"Usuario no encontrado\"}";
        }
        
        AuditService.log(adminId, "VIEW_USER", userId, "Consultó detalles de usuario");
        
        res.type("application/json");
        return detalles;
    }

    /**
     * Asigna rol a usuario
     */
    public Object assignRole(Request req, Response res) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) req.session().attribute("roles");
        Integer adminId = (Integer) req.session().attribute("personaId");
        
        if (roles == null || !roles.contains("ADMINISTRADOR")) {
            res.status(403);
            return "{\"error\": \"Acceso denegado\"}";
        }

        int userId = Integer.parseInt(req.params("id"));
        String rolNuevo = req.queryParams("rol");
        
        Map<String, Object> result = adminService.assignRole(userId, rolNuevo);
        
        if ((Boolean) result.get("success")) {
            AuditService.log(adminId, "ASSIGN_ROLE", userId, "Rol asignado: " + rolNuevo);
            res.status(200);
        } else {
            res.status(400);
        }
        
        res.type("application/json");
        return result;
    }

    /**
     * Revoca rol de usuario
     */
    public Object revokeRole(Request req, Response res) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) req.session().attribute("roles");
        Integer adminId = (Integer) req.session().attribute("personaId");
        
        if (roles == null || !roles.contains("ADMINISTRADOR")) {
            res.status(403);
            return "{\"error\": \"Acceso denegado\"}";
        }

        int userId = Integer.parseInt(req.params("id"));
        String rol = req.queryParams("rol");
        
        Map<String, Object> result = adminService.revokeRole(userId, rol);
        
        if ((Boolean) result.get("success")) {
            AuditService.log(adminId, "REVOKE_ROLE", userId, "Rol revocado: " + rol);
            res.status(200);
        } else {
            res.status(400);
        }
        
        res.type("application/json");
        return result;
    }

    /**
     * Deshabilita usuario (ban)
     */
    public Object disableUser(Request req, Response res) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) req.session().attribute("roles");
        Integer adminId = (Integer) req.session().attribute("personaId");
        
        if (roles == null || !roles.contains("ADMINISTRADOR")) {
            res.status(403);
            return "{\"error\": \"Acceso denegado\"}";
        }

        int userId = Integer.parseInt(req.params("id"));
        String razon = req.queryParams("razon");
        
        Map<String, Object> result = adminService.disableUser(userId, razon);
        
        if ((Boolean) result.get("success")) {
            AuditService.log(adminId, "DISABLE_USER", userId, "Razón: " + razon);
            res.status(200);
        } else {
            res.status(400);
        }
        
        res.type("application/json");
        return result;
    }

    /**
     * Habilita usuario
     */
    public Object enableUser(Request req, Response res) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) req.session().attribute("roles");
        Integer adminId = (Integer) req.session().attribute("personaId");
        
        if (roles == null || !roles.contains("ADMINISTRADOR")) {
            res.status(403);
            return "{\"error\": \"Acceso denegado\"}";
        }

        int userId = Integer.parseInt(req.params("id"));
        
        Map<String, Object> result = adminService.enableUser(userId);
        
        if ((Boolean) result.get("success")) {
            AuditService.log(adminId, "ENABLE_USER", userId, "Usuario habilitado");
            res.status(200);
        } else {
            res.status(400);
        }
        
        res.type("application/json");
        return result;
    }

    /**
     * Cambia contraseña de usuario
     */
    public Object changeUserPassword(Request req, Response res) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) req.session().attribute("roles");
        Integer adminId = (Integer) req.session().attribute("personaId");
        
        if (roles == null || !roles.contains("ADMINISTRADOR")) {
            res.status(403);
            return "{\"error\": \"Acceso denegado\"}";
        }

        int userId = Integer.parseInt(req.params("id"));
        String newPassword = req.queryParams("password");
        
        Map<String, Object> result = adminService.changeUserPassword(userId, newPassword);
        
        if ((Boolean) result.get("success")) {
            AuditService.log(adminId, "CHANGE_PASSWORD", userId, "Contraseña cambiada por admin");
            res.status(200);
        } else {
            res.status(400);
        }
        
        res.type("application/json");
        return result;
    }

    public ModelAndView showPersonaForm(Request req, Response res) {
        List<String> roles = (List<String>) req.session().attribute("roles");
        if (roles == null || !roles.contains("ADMINISTRADOR")) {
            res.redirect("/?error=Acceso no autorizado");
            return null;
        }
        Map<String, Object> model = new HashMap<>();
        String successMessage = req.queryParams("message");
        if (successMessage != null && !successMessage.isEmpty()) model.put("successMessage", successMessage);

        String errorMessage = req.queryParams("error");
        if (errorMessage != null && !errorMessage.isEmpty()) model.put("errorMessage", errorMessage);

        return new ModelAndView(model, "persona_form.mustache");
    }

    public Object createPersona(Request req, Response res) {
        List<String> sessionRoles = (List<String>) req.session().attribute("roles");
        if (sessionRoles == null || !sessionRoles.contains("ADMINISTRADOR")) {
            res.redirect("/?error=Acceso no autorizado");
            return "";
        }

        String dni         = req.queryParams("dni");
        String nombre      = req.queryParams("nombre");
        String apellido    = req.queryParams("apellido");
        String email       = req.queryParams("email");
        String password    = req.queryParams("password");
        String telefono    = req.queryParams("telefono");
        String localidad   = req.queryParams("localidad");
        String departamento = req.queryParams("departamento");

        String[] rolesArr = req.queryParamsValues("roles");
        List<String> roles = rolesArr != null ? List.of(rolesArr) : List.of();

        Map<String, Object> result = userService.registerPersonaConRoles(
            dni, nombre, apellido, email, password, telefono, localidad, roles, departamento
        );

        if ((Boolean) result.get("success")) {
            String encodedMsg = URLEncoder.encode((String) result.get("message"), StandardCharsets.UTF_8);
            res.redirect("/persona/new?message=" + encodedMsg);
        } else {
            String encodedMsg = URLEncoder.encode((String) result.get("message"), StandardCharsets.UTF_8);
            res.redirect("/persona/new?error=" + encodedMsg);
        }
        return "";
    }

    public ModelAndView showUsuarios(Request req, Response res) {

    @SuppressWarnings("unchecked")
    List<String> roles = (List<String>) req.session().attribute("roles");

    if (roles == null || !roles.contains("ADMINISTRADOR")) {
        res.redirect("/?error=Acceso no autorizado");
        return null;
    }

    List<Map<String, Object>> usuarios =
            adminService.listPersonas(1, 100);

    Map<String, Object> model = new HashMap<>();
    model.put("usuarios", usuarios);

    return new ModelAndView(model, "usuarios_list.mustache");
    }
}