package com.is1.proyecto.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

public class AuthService {
    // En 0.12.x se recomienda usar SecretKey explícitamente
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

    public String generateToken(String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);

        // Nueva sintaxis 0.12.x: subject() y claim() directamente, signWith sin algoritmo explícito
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(SECRET_KEY)
                .compact();
    }

    public Claims validateToken(String token) {
        try {
            // CAMBIO CLAVE: En 0.12.x se usa Jwts.parser() y verifyWith()
            return Jwts.parser()
                    .verifyWith(SECRET_KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new RuntimeException("Token inválido");
        }
    }

    public boolean isTokenValid(String token) {
        try {
            validateToken(token);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public boolean isLoginAllowed(String username) {
        LoginAttempt attempt = loginAttempts.get(username);
        if (attempt == null) return true;

        if (attempt.locked) {
            long timeSinceLastAttempt = System.currentTimeMillis() - attempt.lastAttemptTime;
            if (timeSinceLastAttempt >= LOCKOUT_TIME) {
                loginAttempts.remove(username);
                return true;
            }
            return false;
        }
        return true;
    }

    public void recordFailedAttempt(String username) {
        LoginAttempt attempt = loginAttempts.getOrDefault(username, new LoginAttempt());
        attempt.attempts++;
        attempt.lastAttemptTime = System.currentTimeMillis();

        if (attempt.attempts >= MAX_LOGIN_ATTEMPTS) {
            attempt.locked = true;
        }
        loginAttempts.put(username, attempt);
    }

    public void resetFailedAttempts(String username) {
        loginAttempts.remove(username);
    }

    public String getLockoutTimeLeft(String username) {
        LoginAttempt attempt = loginAttempts.get(username);
        if (attempt != null && attempt.locked) {
            long timeSinceLastAttempt = System.currentTimeMillis() - attempt.lastAttemptTime;
            long timeLeft = LOCKOUT_TIME - timeSinceLastAttempt;
            if (timeLeft > 0) {
                return TimeUnit.MILLISECONDS.toMinutes(timeLeft) + " minutos";
            }
        }
        return null;
    }
}