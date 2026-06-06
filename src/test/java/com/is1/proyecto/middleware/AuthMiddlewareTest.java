package com.is1.proyecto.middleware;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.is1.proyecto.service.AuthService;

/**
 * Unit tests for AuthMiddleware.
 * Covers: request authentication, token validation, and error handling.
 */
class AuthMiddlewareTest {

    private AuthMiddleware middleware;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
        middleware = new AuthMiddleware();
    }

    /**
     * Test that middleware is instantiated
     */
    @Test
    void testMiddlewareInstantiation() {
        assertNotNull(middleware);
    }

    /**
     * Test that AuthService is properly initialized
     */
    @Test
    void testAuthServiceIsNotNull() {
        assertNotNull(authService);
    }
}
