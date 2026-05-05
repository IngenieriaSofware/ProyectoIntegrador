package com.is1.proyecto.middleware;

import java.util.List;

import com.is1.proyecto.service.AuthService;

import io.jsonwebtoken.Claims;
import spark.Filter;
import static spark.Spark.halt;

/**
 * Middleware de autenticación y autorización.
 * Valida tokens JWT y verifica roles/permisos.
 * Soporta múltiples roles por Persona (DOCENTE Y ESTUDIANTE simultáneamente).
 */
public class AuthMiddleware {

    private static AuthService authService = new AuthService();

    /**
     * Filtro para requerir autenticación (token válido)
     * No verifica rol específico, solo valida que el token sea válido.
     */
    public static Filter requireAuth = (req, res) -> {
        String token = getTokenFromRequest(req);

        if (token == null) {
            res.redirect("/?error=Token no proporcionado");
            halt();
        }

        try {
            Claims claims = authService.validateToken(token);
            // Almacenar claims en atributo de sesión para acceso posterior
            req.session(true).attribute("claims", claims);
        } catch (Exception e) {
            res.redirect("/?error=Sesion invalida o expirada");
            halt();
        }
    };

    /**
     * Crea un filtro que requiere un rol específico.
     * Soporta múltiples roles: si la persona tiene ANY de los roles requeridos, accede.
     * 
     * @param requiredRoles Roles requeridos (ej: "DOCENTE", "ESTUDIANTE")
     */
    public static Filter requireRole(String... requiredRoles) {
        return (req, res) -> {
            String token = getTokenFromRequest(req);

            if (token == null) {
                res.redirect("/?error=Debe iniciar sesion");
                halt();
            }

            try {
                Claims claims = authService.validateToken(token);
                
                // Obtener lista de roles del token
                @SuppressWarnings("unchecked")
                List<String> personaRoles = (List<String>) claims.get("roles");
                
                if (personaRoles == null || personaRoles.isEmpty()) {
                    res.redirect("/?error=Acceso denegado: sin roles asignados");
                    halt();
                }

                // Verificar si la persona tiene ANY de los roles requeridos
                boolean hasRequiredRole = false;
                for (String requiredRole : requiredRoles) {
                    if (personaRoles.contains(requiredRole)) {
                        hasRequiredRole = true;
                        break;
                    }
                }

                if (!hasRequiredRole) {
                    res.redirect("/?error=Acceso denegado para su rol");
                    halt();
                }

                // Almacenar claims para acceso posterior en controladores
                req.session(true).attribute("claims", claims);
                req.session().attribute("personaId", claims.get("personaId"));
                req.session().attribute("email", claims.getSubject());
                req.session().attribute("roles", personaRoles);

            } catch (Exception e) {
                res.redirect("/?error=Error de autenticacion");
                halt();
            }
        };
    }

    /**
     * Crea un filtro que requiere un permiso específico.
     * 
     * @param requiredPermission Nombre del permiso requerido
     */
    public static Filter requirePermission(String requiredPermission) {
        return (req, res) -> {
            String token = getTokenFromRequest(req);

            if (token == null) {
                res.redirect("/?error=Debe iniciar sesion");
                halt();
            }

            try {
                Claims claims = authService.validateToken(token);

                if (!authService.hasPermissionInToken(claims, requiredPermission)) {
                    res.redirect("/?error=Permiso insuficiente");
                    halt();
                }

                req.session(true).attribute("claims", claims);

            } catch (Exception e) {
                res.redirect("/?error=Error de autenticacion");
                halt();
            }
        };
    }

    /**
     * Obtiene el token JWT del request (desde cookie o Authorization header)
     */
    private static String getTokenFromRequest(spark.Request req) {
        // Buscar en cookie
        String token = req.cookie("token");
        
        if (token == null) {
            // Buscar en header Authorization: Bearer <token>
            String authHeader = req.headers("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        return token;
    }
}