package com.is1.proyecto.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.is1.proyecto.service.AuthService;

import io.jsonwebtoken.Claims;
import spark.ModelAndView;
import spark.Request;
import spark.Response;

/**
 * Controlador principal - Login, Dashboard y Logout.
 * Gestiona redirección según roles (N:N).
 */
public class MainController {
    
    private static AuthService authService = new AuthService();

    /**
     * GET / - Página de login
     * Si hay token válido en cookie, redirecciona al dashboard
     */
    public ModelAndView showLogin(Request req, Response res) {
        // Verificar si hay token válido en cookie
        String token = req.cookie("token");
        
        if (token != null) {
            try {
                // Validar token JWT
                Claims claims = authService.validateToken(token);
                
                // Token válido - recrear sesión
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) claims.get("roles");
                
                if (roles != null && !roles.isEmpty()) {
                    // Guardar datos en sesión para acceso en controladores
                    req.session(true).attribute("personaId", claims.get("personaId"));
                    req.session().attribute("email", claims.getSubject());
                    req.session().attribute("roles", roles);
                    
                    // Redirigir al dashboard correspondiente según roles
                    if (roles.size() > 1) {
                        // Múltiples roles - redirigir a dashboard selector
                        if (roles.contains("ADMINISTRADOR")) {
                            res.redirect("/admin");
                        } else if (roles.contains("DOCENTE")) {
                            res.redirect("/docente");
                        } else if (roles.contains("ESTUDIANTE")) {
                            res.redirect("/estudiante");
                        }
                    } else {
                        // Un solo rol - redirigir directo
                        String rol = roles.get(0);
                        if ("DOCENTE".equals(rol)) {
                            res.redirect("/docente");
                        } else if ("ESTUDIANTE".equals(rol)) {
                            res.redirect("/estudiante");
                        } else if ("ADMINISTRADOR".equals(rol)) {
                            res.redirect("/admin");
                        }
                    }
                    return null; // No renderizar, hacer redirect
                }
            } catch (Exception e) {
                // Token inválido o expirado - continuar a login
            }
        }
        
        // No hay token válido - mostrar login
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
