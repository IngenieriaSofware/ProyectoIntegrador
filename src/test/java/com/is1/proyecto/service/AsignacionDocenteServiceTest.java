package com.is1.proyecto.service;

import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AsignacionDocenteServiceTest {

    private CarreraService carreraService;
    private MateriaService materiaService;
    private ComisionService comisionService;
    private AsignacionDocenteService asignacionDocenteService;

    @BeforeEach
    void setUp() throws Exception {
        openInMemoryDatabase();
        createSchema();
        carreraService = new CarreraService();
        materiaService = new MateriaService();
        comisionService = new ComisionService();
        asignacionDocenteService = new AsignacionDocenteService();
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
    void testDocentePuedeSerAsignadoAMultiplesComisiones() {
        Map<String, Object> carreraResult = carreraService.crearCarrera(
            "ING001",
            "Ingeniería en Sistemas",
            "Carrera de prueba"
        );
        assertTrue((Boolean) carreraResult.get("success"));
        Long carreraId = ((Number) carreraResult.get("id")).longValue();

        Map<String, Object> planResult = carreraService.crearPlan(carreraId.intValue(), 2025, "Plan 2025", 5);
        assertTrue((Boolean) planResult.get("success"));
        Long planId = ((Number) planResult.get("id")).longValue();

        Map<String, Object> activateResult = carreraService.activarPlan(planId.intValue());
        assertTrue((Boolean) activateResult.get("success"));

        Map<String, Object> materiaAResult = materiaService.crearMateria("MATE-101", "Programación I", "Fundamentos", 1);
        assertTrue((Boolean) materiaAResult.get("success"));
        Long materiaAId = ((Number) materiaAResult.get("id")).longValue();

        Map<String, Object> materiaBResult = materiaService.crearMateria("MATE-102", "Programación II", "Avanzada", 1);
        assertTrue((Boolean) materiaBResult.get("success"));
        Long materiaBId = ((Number) materiaBResult.get("id")).longValue();

        Map<String, Object> mpAResult = materiaService.asociarPlan(materiaAId.intValue(), planId.intValue(), 1, "PRIMERO", 80, "OBLIGATORIA", 1);
        assertTrue((Boolean) mpAResult.get("success"));
        Long mpAId = ((Number) mpAResult.get("id")).longValue();

        Map<String, Object> mpBResult = materiaService.asociarPlan(materiaBId.intValue(), planId.intValue(), 2, "SEGUNDO", 80, "OBLIGATORIA", 1);
        assertTrue((Boolean) mpBResult.get("success"));
        Long mpBId = ((Number) mpBResult.get("id")).longValue();

        Long periodoId = createPeriodoActivo(2025, 1, "Primer cuatrimestre");

        Map<String, Object> comisionAResult = comisionService.crearComision(materiaAId.intValue(), planId.intValue(), periodoId.intValue(), "MANANA", 1);
        assertTrue((Boolean) comisionAResult.get("success"));
        Long comisionAId = ((Number) comisionAResult.get("id")).longValue();

        Map<String, Object> comisionBResult = comisionService.crearComision(materiaBId.intValue(), planId.intValue(), periodoId.intValue(), "TARDE", 1);
        assertTrue((Boolean) comisionBResult.get("success"));
        Long comisionBId = ((Number) comisionBResult.get("id")).longValue();

        Long docenteId = createDocente("12345678", "Ana", "García", "ana.garcia@example.com");

        Map<String, Object> asignacionAResult = asignacionDocenteService.asignarDocente(comisionAId.intValue(), docenteId.intValue(), "TITULAR", 1);
        assertTrue((Boolean) asignacionAResult.get("success"));
        assertEquals("Docente asignado correctamente con cargo Titular.", asignacionAResult.get("message"));

        Map<String, Object> asignacionBResult = asignacionDocenteService.asignarDocente(comisionBId.intValue(), docenteId.intValue(), "JTP", 1);
        assertTrue((Boolean) asignacionBResult.get("success"));
        assertEquals("Docente asignado correctamente con cargo JTP.", asignacionBResult.get("message"));
    }

    @Test
    void testNoPermiteAsignarMismoDocenteDosVecesAMismaComision() {
        Map<String, Object> carreraResult = carreraService.crearCarrera(
            "ING001",
            "Ingeniería en Sistemas",
            "Carrera de prueba"
        );
        assertTrue((Boolean) carreraResult.get("success"));
        Long carreraId = ((Number) carreraResult.get("id")).longValue();

        Map<String, Object> planResult = carreraService.crearPlan(carreraId.intValue(), 2025, "Plan 2025", 5);
        assertTrue((Boolean) planResult.get("success"));
        Long planId = ((Number) planResult.get("id")).longValue();
        carreraService.activarPlan(planId.intValue());

        Map<String, Object> materiaAResult = materiaService.crearMateria("MATE-101", "Programación I", "Fundamentos", 1);
        assertTrue((Boolean) materiaAResult.get("success"));
        Long materiaAId = ((Number) materiaAResult.get("id")).longValue();
        materiaService.asociarPlan(materiaAId.intValue(), planId.intValue(), 1, "PRIMERO", 80, "OBLIGATORIA", 1);

        Long periodoId = createPeriodoActivo(2025, 1, "Primer cuatrimestre");

        Map<String, Object> comisionAResult = comisionService.crearComision(materiaAId.intValue(), planId.intValue(), periodoId.intValue(), "MANANA", 1);
        assertTrue((Boolean) comisionAResult.get("success"));
        Long comisionAId = ((Number) comisionAResult.get("id")).longValue();

        Long docenteId = createDocente("12345678", "Ana", "García", "ana.garcia@example.com");

        Map<String, Object> asignacionAResult = asignacionDocenteService.asignarDocente(comisionAId.intValue(), docenteId.intValue(), "TITULAR", 1);
        assertTrue((Boolean) asignacionAResult.get("success"));

        Map<String, Object> asignacionRepetidaResult = asignacionDocenteService.asignarDocente(comisionAId.intValue(), docenteId.intValue(), "AYUDANTE", 1);
        assertFalse((Boolean) asignacionRepetidaResult.get("success"));
        assertTrue(asignacionRepetidaResult.get("message").toString().contains("ya está asignado a esta comisión"));
    }

    private Long createDocente(String dni, String nombre, String apellido, String email) {
        Base.exec("INSERT INTO personas (dni, nombre, apellido, email, password, telefono, localidad, enabled) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            dni, nombre, apellido, email, "password", "00000000", "Ciudad", 1);
        Long personaId = ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();
        Base.exec("INSERT INTO docentes (persona_id, cargo_id) VALUES (?, ?)", personaId, 1);
        Base.exec("INSERT INTO persona_roles (persona_id, rol) VALUES (?, ?)", personaId, "DOCENTE");
        return personaId;
    }

    private Long createPeriodoActivo(int anio, int cuatrimestre, String descripcion) {
        Base.exec("INSERT INTO periodos_lectivos (anio, cuatrimestre, descripcion, activo) VALUES (?, ?, ?, ?)",
            anio, cuatrimestre, descripcion, 1);
        return ((Number) Base.firstCell("SELECT last_insert_rowid()")).longValue();
    }
}
