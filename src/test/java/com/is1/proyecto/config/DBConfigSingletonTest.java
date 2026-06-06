package com.is1.proyecto.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DBConfigSingleton.
 * Covers: singleton pattern, database configuration loading, and property access.
 */
class DBConfigSingletonTest {

    private DBConfigSingleton config;

    @BeforeEach
    void setUp() {
        config = DBConfigSingleton.getInstance();
    }

    /**
     * Test singleton pattern - same instance returned
     */
    @Test
    void testSingletonPattern() {
        DBConfigSingleton config2 = DBConfigSingleton.getInstance();
        assertSame(config, config2);
    }

    /**
     * Test database configuration is loaded
     */
    @Test
    void testDatabaseConfigurationLoaded() {
        assertNotNull(config.getDriver());
        assertNotNull(config.getDbUrl());
        assertNotNull(config.getUser());
        assertNotNull(config.getPass());
    }

    /**
     * Test driver is sqlite
     */
    @Test
    void testDriverIsConfigured() {
        String driver = config.getDriver();
        assertTrue(!driver.isEmpty());
    }

    /**
     * Test database URL contains expected path
     */
    @Test
    void testDatabaseUrlValid() {
        String dbUrl = config.getDbUrl();
        assertTrue(!dbUrl.isEmpty());
        assertTrue(dbUrl.contains("jdbc:"));
    }
}
