package com.is1.proyecto.service;

import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.is1.proyecto.models.Carrera;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CarreraService.
 * Covers: career creation, attribute normalization, duplicate validation, and career detail retrieval.
 */
class CarreraServiceTest {

    private CarreraService carreraService;

    @BeforeEach
    void setUp() throws Exception {
        openInMemoryDatabase();
        createSchema();
        carreraService = new CarreraService();
    }

    @AfterEach
    void tearDown() {
        if (Base.hasConnection()) {
            Base.close();
        }
    }

    private void openInMemoryDatabase() {
        Base.open("org.sqlite.JDBC", "jdbc:sqlite::memory:", "", "");
    }

    private void createSchema() throws Exception {
        InputStream schemaStream = getClass().getClassLoader().getResourceAsStream("scheme.sql");
        assertNotNull(schemaStream, "scheme.sql resource should be available on the classpath");

        String sql = new BufferedReader(new InputStreamReader(schemaStream, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));

        StringBuilder statement = new StringBuilder();
        for (String line : sql.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            statement.append(line).append("\n");
            if (trimmed.endsWith(";")) {
                String sqlStatement = statement.toString();
                sqlStatement = sqlStatement.substring(0, sqlStatement.lastIndexOf(';')).trim();
                if (!sqlStatement.isEmpty()) {
                    Base.exec(sqlStatement);
                }
                statement.setLength(0);
            }
        }
        if (statement.length() > 0) {
            String sqlStatement = statement.toString().trim();
            if (!sqlStatement.isEmpty()) {
                Base.exec(sqlStatement);
            }
        }
    }

    @Test
    void testCrearCarreraValida() {
        Map<String, Object> result = carreraService.crearCarrera(
            "  ing 001 ",
            " Ingeniería en Sistemas ",
            "   Carrera de sistemas  "
        );

        assertTrue((Boolean) result.get("success"));
        assertEquals("Carrera creada correctamente.", result.get("message"));
        assertNotNull(result.get("id"));

        Long id = ((Number) result.get("id")).longValue();
        Carrera carrera = Carrera.findById(id);
        assertNotNull(carrera);
        assertEquals("ING001", carrera.getCodigo());
        assertEquals("Ingeniería en Sistemas", carrera.getNombre());
        assertEquals("Carrera de sistemas", carrera.getDescripcion());
        assertTrue(carrera.isActiva());
    }

    @Test
    void testCrearCarreraConCodigoDuplicadoFalla() {
        carreraService.crearCarrera("ING001", "Ingeniería en Sistemas", "Carrera base");

        Map<String, Object> result = carreraService.crearCarrera(" ing001 ", "Otra Carrera", "Otra descripción");

        assertFalse((Boolean) result.get("success"));
        assertEquals("codigo", result.get("field"));
        assertTrue(result.get("message").toString().contains("ya está en uso"));
    }

    @Test
    void testCrearCarreraConNombreDuplicadoFalla() {
        carreraService.crearCarrera("ING001", "Ingeniería en Sistemas", "Carrera base");

        Map<String, Object> result = carreraService.crearCarrera("ING002", " ingeniería en sistemas ", "Otra descripción");

        assertFalse((Boolean) result.get("success"));
        assertEquals("nombre", result.get("field"));
        assertTrue(result.get("message").toString().contains("Ya existe una carrera"));
    }

    @Test
    void testGetCarreraDetalleIncluyePlanesVigentes() {
        Map<String, Object> createResult = carreraService.crearCarrera("ING001", "Ingeniería en Sistemas", "Plan de estudios");
        assertTrue((Boolean) createResult.get("success"));

        Long carreraId = ((Number) createResult.get("id")).longValue();
        Base.exec(
            "INSERT INTO planes_estudio (carrera_id, anio_vigencia, descripcion, duracion_anios, activo) VALUES (?, ?, ?, ?, ?)",
            carreraId,
            2025,
            "Plan 2025",
            5,
            1
        );

        Map<String, Object> detalle = carreraService.getCarreraDetalle(carreraId.intValue());

        assertNotNull(detalle);
        assertEquals(carreraId, ((Number) detalle.get("id")).longValue());
        assertEquals("ING001", detalle.get("codigo"));
        assertEquals("Ingeniería en Sistemas", detalle.get("nombre"));
        assertEquals("Plan de estudios", detalle.get("descripcion"));
        assertTrue((Boolean) detalle.get("activa"));
        assertFalse((Boolean) detalle.get("sinPlanActivo"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> planes = (List<Map<String, Object>>) detalle.get("planes");
        assertNotNull(planes);
        assertEquals(1, planes.size());

        Map<String, Object> plan = planes.get(0);
        assertEquals(2025, ((Number) plan.get("anioVigencia")).intValue());
        assertEquals("Plan 2025", plan.get("descripcion"));
        assertEquals(5, ((Number) plan.get("duracionAnios")).intValue());
        assertTrue((Boolean) plan.get("activo"));
    }
}
