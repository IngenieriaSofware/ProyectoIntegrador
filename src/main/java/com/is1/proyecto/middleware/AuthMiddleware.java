package com.is1.proyecto.middleware;

import com.is1.proyecto.service.AuthService;

import spark.Filter;
import static spark.Spark.halt;

public class AuthMiddleware {

    private static AuthService authService = new AuthService();

    public static Filter requireAuth = (req, res) -> {
        // Buscamos primero en la cookie para el navegador, luego en el header
        String token = req.cookie("token");
        
        if (token == null) {
            String authHeader = req.headers("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        if (token == null) {
            // En lugar de un error crudo, redirigimos al login si es una petición de página
            res.redirect("/?error=Token no proporcionado");
            halt();
        }

        try {
            authService.validateToken(token);
        } catch (Exception e) {
            res.redirect("/?error=Sesion invalida o expirada");
            halt();
        }
    };

    public static Filter requireRole(String requiredRole) {
        return (req, res) -> {
            // Misma lógica de búsqueda dual (Cookie + Header)
            String token = req.cookie("token");
            
            if (token == null) {
                String authHeader = req.headers("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }

            if (token == null) {
                res.redirect("/?error=Debe iniciar sesion");
                halt();
            }

            try {
                var claims = authService.validateToken(token);
                String userRole = claims.get("role", String.class);

                if (!userRole.equals(requiredRole)) {
                    // Si el rol no coincide (ej: un ESTUDIANTE intentando entrar a /admin)
                    res.redirect("/?error=Acceso denegado para su rol");
                    halt();
                }
            } catch (Exception e) {
                res.redirect("/?error=Error de autenticacion");
                halt();
            }
        };
    }
}