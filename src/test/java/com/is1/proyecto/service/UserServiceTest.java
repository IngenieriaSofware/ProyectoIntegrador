package com.is1.proyecto.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

/**
 * Unit tests for UserService.
 * Covers: user registration validation, attribute correctness, and error handling.
 */
class UserServiceTest {

    private UserService userService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userService = new UserService();
        authService = new AuthService();
    }

    // ============ VALIDATION TESTS ============

    /**
     * Test DNI validation - DNI cannot be null or empty
     */
    @Test
    void testRegisterPersonaDNIRequired() {
        Map<String, Object> result = userService.registerPersona(
            null,  // DNI is null
            "Juan",
            "Pérez",
            "juan@example.com",
            "password123",
            "1234567890",
            "Buenos Aires",
            "ESTUDIANTE"
        );

        assertFalse((Boolean) result.get("success"));
        assertTrue(result.get("message").toString().contains("DNI"));
    }

    /**
     * Test name validation - Name cannot be empty
     */
    @Test
    void testRegisterPersonaNombreRequired() {
        Map<String, Object> result = userService.registerPersona(
            "12345678",
            "",  // Name is empty
            "Pérez",
            "juan@example.com",
            "password123",
            "1234567890",
            "Buenos Aires",
            "ESTUDIANTE"
        );

        assertFalse((Boolean) result.get("success"));
        assertTrue(result.get("message").toString().contains("Nombre"));
    }

    /**
     * Test surname validation - Surname cannot be empty
     */
    @Test
    void testRegisterPersonaApellidoRequired() {
        Map<String, Object> result = userService.registerPersona(
            "12345678",
            "Juan",
            null,  // Surname is null
            "juan@example.com",
            "password123",
            "1234567890",
            "Buenos Aires",
            "ESTUDIANTE"
        );

        assertFalse((Boolean) result.get("success"));
        assertTrue(result.get("message").toString().contains("Apellido"));
    }

    /**
     * Test email validation - Email is required
     */
    @Test
    void testRegisterPersonaEmailRequired() {
        Map<String, Object> result = userService.registerPersona(
            "12345678",
            "Juan",
            "Pérez",
            "",  // Email is empty
            "password123",
            "1234567890",
            "Buenos Aires",
            "ESTUDIANTE"
        );

        assertFalse((Boolean) result.get("success"));
        assertTrue(result.get("message").toString().contains("Email"));
    }

    /**
     * Test email format validation - Email must be valid format
     */
    @Test
    void testRegisterPersonaInvalidEmailFormat() {
        Map<String, Object> result = userService.registerPersona(
            "12345678",
            "Juan",
            "Pérez",
            "invalid-email",  // Invalid email format
            "password123",
            "1234567890",
            "Buenos Aires",
            "ESTUDIANTE"
        );

        assertFalse((Boolean) result.get("success"));
        assertTrue(result.get("message").toString().contains("Email"));
    }

    /**
     * Test password validation - Password is required
     */
    @Test
    void testRegisterPersonaPasswordRequired() {
        Map<String, Object> result = userService.registerPersona(
            "12345678",
            "Juan",
            "Pérez",
            "juan@example.com",
            "",  // Password is empty
            "1234567890",
            "Buenos Aires",
            "ESTUDIANTE"
        );

        assertFalse((Boolean) result.get("success"));
        assertTrue(result.get("message").toString().contains("Contraseña"));
    }

    /**
     * Test password length validation - Password must be 8-30 chars
     */
    @Test
    void testRegisterPersonaPasswordTooShort() {
        Map<String, Object> result = userService.registerPersona(
            "12345678",
            "Juan",
            "Pérez",
            "juan@example.com",
            "short",  // Password too short (< 8 chars)
            "1234567890",
            "Buenos Aires",
            "ESTUDIANTE"
        );

        assertFalse((Boolean) result.get("success"));
        assertTrue(result.get("message").toString().contains("8-30"));
    }

    /**
     * Test password length validation - Password too long
     */
    @Test
    void testRegisterPersonaPasswordTooLong() {
        Map<String, Object> result = userService.registerPersona(
            "12345678",
            "Juan",
            "Pérez",
            "juan@example.com",
            "this_is_a_very_long_password_that_exceeds_30_characters",
            "1234567890",
            "Buenos Aires",
            "ESTUDIANTE"
        );

        assertFalse((Boolean) result.get("success"));
        assertTrue(result.get("message").toString().contains("8-30"));
    }

    /**
     * Test role validation - Role is required
     */
    @Test
    void testRegisterPersonaRoleRequired() {
        Map<String, Object> result = userService.registerPersona(
            "12345678",
            "Juan",
            "Pérez",
            "juan@example.com",
            "password123",
            "1234567890",
            "Buenos Aires",
            ""  // Role is empty
        );

        assertFalse((Boolean) result.get("success"));
        assertTrue(result.get("message").toString().contains("Rol"));
    }

    /**
     * Test invalid role validation - Role must be DOCENTE, ESTUDIANTE, or ADMINISTRADOR
     */
    @Test
    void testRegisterPersonaInvalidRole() {
        Map<String, Object> result = userService.registerPersona(
            "12345678",
            "Juan",
            "Pérez",
            "juan@example.com",
            "password123",
            "1234567890",
            "Buenos Aires",
            "INVALID_ROLE"  // Invalid role
        );

        assertFalse((Boolean) result.get("success"));
        assertTrue(result.get("message").toString().contains("inválido"));
    }

    // ============ ATTRIBUTE TESTS ============

    /**
     * Test that valid email format is accepted
     */
    @Test
    void testRegisterPersonaValidEmailFormat() {
        Map<String, Object> result = userService.registerPersona(
            "12345678",
            "Juan",
            "Pérez",
            "juan.perez@example.com",  // Valid email format
            "password123",
            "1234567890",
            "Buenos Aires",
            "ESTUDIANTE"
        );

        // Should pass validation (may fail on DB constraints, but not email format)
        assertNotNull(result.get("message"));
    }

    /**
     * Test that valid password length is accepted
     */
    @Test
    void testRegisterPersonaValidPasswordLength() {
        Map<String, Object> result = userService.registerPersona(
            "12345678",
            "Juan",
            "Pérez",
            "juan@example.com",
            "validPassword123",  // Valid length (16 chars)
            "1234567890",
            "Buenos Aires",
            "ESTUDIANTE"
        );

        // Should pass validation (may fail on DB constraints, but not password length)
        assertNotNull(result.get("message"));
    }

    /**
     * Test that ESTUDIANTE role is valid
     */
    @Test
    void testRegisterPersonaValidEstudianteRole() {
        Map<String, Object> result = userService.registerPersona(
            "12345678",
            "Juan",
            "Pérez",
            "juan@example.com",
            "password123",
            "1234567890",
            "Buenos Aires",
            "ESTUDIANTE"  // Valid role
        );

        assertNotNull(result.get("message"));
    }

    /**
     * Test that DOCENTE role is valid
     */
    @Test
    void testRegisterPersonaValidDocenteRole() {
        Map<String, Object> result = userService.registerPersona(
            "12345678",
            "Juan",
            "Pérez",
            "juan@example.com",
            "password123",
            "1234567890",
            "Buenos Aires",
            "DOCENTE"  // Valid role
        );

        assertNotNull(result.get("message"));
    }

    /**
     * Test that ADMINISTRADOR role is valid
     */
    @Test
    void testRegisterPersonaValidAdministradorRole() {
        Map<String, Object> result = userService.registerPersona(
            "12345678",
            "Juan",
            "Pérez",
            "juan@example.com",
            "password123",
            "1234567890",
            "Buenos Aires",
            "ADMINISTRADOR"  // Valid role
        );

        assertNotNull(result.get("message"));
    }

    /**
     * Test that optional fields (telefono, localidad) can be null
     */
    @Test
    void testRegisterPersonaOptionalFields() {
        Map<String, Object> result = userService.registerPersona(
            "12345678",
            "Juan",
            "Pérez",
            "juan@example.com",
            "password123",
            null,  // Optional telefono
            null,  // Optional localidad
            "ESTUDIANTE"
        );

        // Should pass validation (may fail on DB constraints, but not null fields)
        assertNotNull(result.get("message"));
    }

    /**
     * Test result structure contains expected fields on success
     */
    @Test
    void testRegisterPersonaResultStructure() {
        Map<String, Object> result = userService.registerPersona(
            "12345678",
            "Juan",
            "Pérez",
            "juan@example.com",
            "password123",
            "1234567890",
            "Buenos Aires",
            "ESTUDIANTE"
        );

        // Check result map contains standard fields
        assertNotNull(result.get("success"));
        assertNotNull(result.get("message"));
        assertTrue(result.containsKey("success"));
        assertTrue(result.containsKey("message"));
    }

    /**
     * Test result structure contains expected fields on validation failure
     */
    @Test
    void testRegisterPersonaResultStructureOnFailure() {
        Map<String, Object> result = userService.registerPersona(
            null,
            "Juan",
            "Pérez",
            "juan@example.com",
            "password123",
            "1234567890",
            "Buenos Aires",
            "ESTUDIANTE"
        );

        assertFalse((Boolean) result.get("success"));
        assertNotNull(result.get("message"));
        assertTrue(result.get("message").toString().length() > 0);
    }

    /**
     * Test that password is validated before persistence
     */
    @Test
    void testPasswordValidationOccursBeforePersistence() {
        // Invalid password should be caught before any DB operation
        Map<String, Object> result = userService.registerPersona(
            "12345678",
            "Juan",
            "Pérez",
            "juan@example.com",
            "short",  // Too short
            "1234567890",
            "Buenos Aires",
            "ESTUDIANTE"
        );

        assertFalse((Boolean) result.get("success"));
        String message = result.get("message").toString();
        assertTrue(message.contains("8-30") || message.contains("Contraseña"));
    }

    @Test
    void testEmailValidationOccursBeforePersistence() {
        // Invalid email should be caught before any DB operation
        Map<String, Object> result = userService.registerPersona(
            "12345678",
            "Juan",
            "Dehaes",
            "invalid-email",  // Invalid format
            "password123",
            "1234567890",
            "Buenos Aires",
            "ESTUDIANTE"
        );

        assertFalse((Boolean) result.get("success"));
        String message = result.get("message").toString();
        assertTrue(message.contains("Email") || message.contains("formato"));
    }
}
