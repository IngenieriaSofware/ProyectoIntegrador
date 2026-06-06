package com.is1.proyecto.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import io.jsonwebtoken.Claims;

/**
 * Unit tests for AuthService.
 * Covers: token generation, validation, password hashing, and login lockout logic.
 */
class AuthServiceTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
    }

    /**
     * Test password hashing and verification
     */
    @Test
    void testHashPassword() {
        String password = "testPassword123";
        String hash = authService.hashPassword(password);
        
        assertNotNull(hash);
        assertNotEquals(password, hash);
        assertTrue(authService.verifyPassword(password, hash));
    }

    /**
     * Test password verification with wrong password
     */
    @Test
    void testVerifyPasswordFailure() {
        String password = "correctPassword";
        String hash = authService.hashPassword(password);
        
        assertFalse(authService.verifyPassword("wrongPassword", hash));
    }

    /**
     * Test login attempt tracking and lockout
     */
    @Test
    void testLoginLockout() {
        String identifier = "test@example.com";
        
        assertTrue(authService.isLoginAllowed(identifier));
        
        // Record 5 failed attempts
        for (int i = 0; i < 5; i++) {
            authService.recordFailedAttempt(identifier);
        }
        
        // Should be locked after 5 attempts
        assertFalse(authService.isLoginAllowed(identifier));
    }

    /**
     * Test reset of failed attempts after successful login
     */
    @Test
    void testResetFailedAttempts() {
        String identifier = "test@example.com";
        
        authService.recordFailedAttempt(identifier);
        authService.recordFailedAttempt(identifier);
        
        authService.resetFailedAttempts(identifier);
        
        assertTrue(authService.isLoginAllowed(identifier));
    }

    /**
     * Test token validation with invalid token
     */
    @Test
    void testValidateInvalidToken() {
        assertThrows(RuntimeException.class, () -> {
            authService.validateToken("invalidToken");
        });
    }

    /**
     * Test isTokenValid helper method
     */
    @Test
    void testIsTokenValid() {
        assertFalse(authService.isTokenValid("invalidToken"));
    }
}
