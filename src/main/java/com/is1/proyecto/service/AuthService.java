package com.is1.proyecto.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

import org.mindrot.jbcrypt.BCrypt;

import com.is1.proyecto.models.Permiso;
import com.is1.proyecto.models.Persona;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

/**
 * Servicio de autenticación y autorización.
 * Usa Bcrypt para hasheo de contraseñas y JWT para tokens.
 * Soporta N:N roles (una Persona puede ser DOCENTE Y ESTUDIANTE).
 */
public class AuthService {
    // JWT Secret Key
    private static final SecretKey SECRET_KEY = Jwts.SIG.HS256.key().build(); 
    private static final long EXPIRATION_TIME = TimeUnit.HOURS.toMillis(24);
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_TIME = TimeUnit.MINUTES.toMillis(15);
    private static final Map<String, LoginAttempt> loginAttempts = new HashMap<>();

    private static class LoginAttempt {
        int attempts;
        long lastAttemptTime;
        boolean locked;
    }

    /**
     * Genera un token JWT para una Persona autenticada.
     * El token incluye: email, lista de roles y lista de permisos.
     */
    public String generateToken(Persona persona) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);

        // Extraer roles
        List<String> roles = persona.getRoleNames();
        
        // Extraer permisos asignados
        List<String> permisos = new ArrayList<>();
        List<Permiso> assignedPermisos = Permiso.where(
            "id IN (SELECT permiso_id FROM persona_permisos WHERE persona_id = ?)", 
            persona.getId()
        );
        for (Permiso p : assignedPermisos) {
            permisos.add(p.getNombre());
        }

        // Construir JWT con claims personalizados
        return Jwts.builder()
                .subject(persona.getEmail())  // Subject = email (identificador único)
                .claim("personaId", persona.getId())
                .claim("dni", persona.getDni())
                .claim("nombre", persona.getNombre())
                .claim("apellido", persona.getApellido())
                .claim("roles", roles)        // Lista de roles (puede ser >1)
                .claim("permisos", permisos)  // Lista de permisos
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(SECRET_KEY)
                .compact();
    }

    /**
     * Valida un token JWT y retorna los claims
     */
    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(SECRET_KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new RuntimeException("Token inválido");
        }
    }

    /**
     * Verifica si un token es válido
     */
    public boolean isTokenValid(String token) {
        try {
            validateToken(token);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Verifica si un token tiene un rol específico
     */
    @SuppressWarnings("unchecked")
    public boolean hasRoleInToken(Claims claims, String rol) {
        List<String> roles = (List<String>) claims.get("roles");
        if (roles == null) return false;
        return roles.contains(rol);
    }

    /**
     * Verifica si un token tiene un permiso específico
     */
    @SuppressWarnings("unchecked")
    public boolean hasPermissionInToken(Claims claims, String permiso) {
        List<String> permisos = (List<String>) claims.get("permisos");
        if (permisos == null) return false;
        return permisos.contains(permiso);
    }

    /**
     * Extrae la lista de roles del token
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(Claims claims) {
        List<String> roles = (List<String>) claims.get("roles");
        return roles != null ? roles : new ArrayList<>();
    }

    /**
     * Valida si el login es permitido (controla intentos fallidos)
     */
    public boolean isLoginAllowed(String identifier) {
        LoginAttempt attempt = loginAttempts.get(identifier);
        if (attempt == null) return true;

        if (attempt.locked) {
            long timeSinceLastAttempt = System.currentTimeMillis() - attempt.lastAttemptTime;
            if (timeSinceLastAttempt >= LOCKOUT_TIME) {
                loginAttempts.remove(identifier);
                return true;
            }
            return false;
        }
        return true;
    }

    /**
     * Registra un intento fallido de login
     */
    public void recordFailedAttempt(String identifier) {
        LoginAttempt attempt = loginAttempts.getOrDefault(identifier, new LoginAttempt());
        attempt.attempts++;
        attempt.lastAttemptTime = System.currentTimeMillis();

        if (attempt.attempts >= MAX_LOGIN_ATTEMPTS) {
            attempt.locked = true;
        }
        loginAttempts.put(identifier, attempt);
    }

    /**
     * Limpia intentos fallidos al login exitoso
     */
    public void resetFailedAttempts(String identifier) {
        loginAttempts.remove(identifier);
    }

    /**
     * Obtiene tiempo restante de bloqueo
     */
    public String getLockoutTimeLeft(String identifier) {
        LoginAttempt attempt = loginAttempts.get(identifier);
        if (attempt != null && attempt.locked) {
            long timeSinceLastAttempt = System.currentTimeMillis() - attempt.lastAttemptTime;
            long timeLeft = LOCKOUT_TIME - timeSinceLastAttempt;
            if (timeLeft > 0) {
                return TimeUnit.MILLISECONDS.toMinutes(timeLeft) + " minutos";
            }
        }
        return null;
    }

    /**
     * Hashea una contraseña usando Bcrypt
     */
    public String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
     * Verifica si una contraseña coincide con su hash Bcrypt
     */
    public boolean verifyPassword(String password, String hashedPassword) {
        try {
            return BCrypt.checkpw(password, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }
}