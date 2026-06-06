package com.is1.proyecto.service;

import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.MateriaPlan;
import com.is1.proyecto.service.CarreraService;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class MateriaServiceTest {

    private MateriaService materiaService;
    private CorrelatividadService correlatividadService;
    private CarreraService carreraService;

    @BeforeEach
    void setUp() throws Exception {
        openInMemoryDatabase();
        createSchema();
        materiaService = new MateriaService();
        correlatividadService = new CorrelatividadService();
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
    void testCrearMateriaConAltaEnCarreraYPlan() {
        createCarreraYPlan();

        Map<String, Object> result = materiaService.crearMateria("  mate-101  ", "  Fundamentos de Programación  ", "  Introducción a la lógica  ", 1);
        assertTrue((Boolean) result.get("success"));
        assertNotNull(result.get("id"));
        assertEquals("Materia creada correctamente.", result.get("message"));

        Long materiaId = ((Number) result.get("id")).longValue();
        Materia materia = Materia.findById(materiaId);
        assertNotNull(materia);
        assertEquals("MATE-101", materia.getCodigo());
        assertEquals("Fundamentos de Programación", materia.getNombre());
        assertEquals("Introducción a la lógica", materia.getDescripcion());
        assertTrue(materia.isActiva());

        Map<String, Object> assocResult = materiaService.asociarPlan((int) materiaId.longValue(), 1, 1, "PRIMERO", 80, "OBLIGATORIA", 1);
        assertTrue((Boolean) assocResult.get("success"));
        assertNotNull(assocResult.get("id"));
        assertEquals("Materia asociada al plan correctamente.", assocResult.get("message"));

        Long mpId = ((Number) assocResult.get("id")).longValue();
        MateriaPlan mp = MateriaPlan.findById(mpId);
        assertNotNull(mp);
        assertEquals((int) materiaId.longValue(), mp.getMateriaId());
        assertEquals(1, mp.getPlanEstudioId());
        assertEquals(1, mp.getAnioPlan());
        assertEquals("PRIMERO", mp.getCuatrimestre());
        assertEquals(80, mp.getCargaHoraria());
        assertEquals("OBLIGATORIA", mp.getCaracter());
        assertTrue(mp.isActiva());
    }

    @Test
    void testAgregarCorrelatividadEntreMateriasDeMismoPlan() {
        createCarreraYPlan();

        Long materiaAId = ((Number) materiaService.crearMateria("MATE-101", "Programación I", "", 1).get("id")).longValue();
        Long materiaBId = ((Number) materiaService.crearMateria("MATE-102", "Programación II", "", 1).get("id")).longValue();

        Long mpAId = ((Number) materiaService.asociarPlan((int) materiaAId.longValue(), 1, 1, "PRIMERO", 80, "OBLIGATORIA", 1).get("id")).longValue();
        Long mpBId = ((Number) materiaService.asociarPlan((int) materiaBId.longValue(), 1, 2, "SEGUNDO", 80, "OBLIGATORIA", 1).get("id")).longValue();

        Map<String, Object> corrResult = correlatividadService.agregarCorrelatividad((int) mpAId.longValue(), (int) mpBId.longValue(), "REGULAR");
        assertTrue((Boolean) corrResult.get("success"));
        assertEquals("Correlatividad registrada correctamente para el plan.", corrResult.get("message"));

        Map<String, Object> detalleDestino = correlatividadService.getCorrelatividadesDelPlan((int) mpBId.longValue());
        assertNotNull(detalleDestino);
        assertEquals(mpBId, ((Number) detalleDestino.get("materiaPlanId")).longValue());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> anteriores = (List<Map<String, Object>>) detalleDestino.get("anteriores");
        assertEquals(1, anteriores.size());
        assertEquals("Programación I", anteriores.get(0).get("nombre"));
        assertEquals("REGULAR", anteriores.get(0).get("condicion"));
        assertEquals("Regular", anteriores.get(0).get("condicionLabel"));
    }

    @Test
    void testAgregarCorrelatividadRechazaMismaMateria() {
        createCarreraYPlan();

        Long materiaId = ((Number) materiaService.crearMateria("MATE-101", "Programación I", "", 1).get("id")).longValue();
        Long mpId = ((Number) materiaService.asociarPlan((int) materiaId.longValue(), 1, 1, "PRIMERO", 80, "OBLIGATORIA", 1).get("id")).longValue();

        Map<String, Object> corrResult = correlatividadService.agregarCorrelatividad((int) mpId.longValue(), (int) mpId.longValue(), "REGULAR");
        assertFalse((Boolean) corrResult.get("success"));
        assertEquals("Una materia no puede ser correlativa de sí misma.", corrResult.get("message"));
    }

    @Test
    void testFlujoCompletoCrearCarreraPlanMateriaAsociarYCorrelatividad() {
        Map<String, Object> carreraResult = carreraService.crearCarrera(
            "  ing 001 ",
            "  Ingeniería en Sistemas ",
            "  Carrera prueba  "
        );
        assertTrue((Boolean) carreraResult.get("success"));
        Long carreraId = ((Number) carreraResult.get("id")).longValue();

        Map<String, Object> planResult = carreraService.crearPlan(carreraId.intValue(), 2025, "Plan 2025", 5);
        assertTrue((Boolean) planResult.get("success"));
        Long planId = ((Number) planResult.get("id")).longValue();
        assertEquals("Plan de estudio 2025 creado correctamente. Este plan aún no está activo.", planResult.get("message"));

        Map<String, Object> activateResult = carreraService.activarPlan(planId.intValue());
        assertTrue((Boolean) activateResult.get("success"));
        assertEquals("Plan 2025 activado correctamente.", activateResult.get("message"));

        Map<String, Object> materiaAResult = materiaService.crearMateria("MATE-101", "Programación I", "Introducción a la programación", 1);
        assertTrue((Boolean) materiaAResult.get("success"));
        Long materiaAId = ((Number) materiaAResult.get("id")).longValue();

        Map<String, Object> materiaBResult = materiaService.crearMateria("MATE-102", "Programación II", "Segunda materia", 1);
        assertTrue((Boolean) materiaBResult.get("success"));
        Long materiaBId = ((Number) materiaBResult.get("id")).longValue();

        Map<String, Object> mpAResult = materiaService.asociarPlan(materiaAId.intValue(), planId.intValue(), 1, "PRIMERO", 80, "OBLIGATORIA", 1);
        assertTrue((Boolean) mpAResult.get("success"));
        Long mpAId = ((Number) mpAResult.get("id")).longValue();

        Map<String, Object> mpBResult = materiaService.asociarPlan(materiaBId.intValue(), planId.intValue(), 2, "SEGUNDO", 80, "OBLIGATORIA", 1);
        assertTrue((Boolean) mpBResult.get("success"));
        Long mpBId = ((Number) mpBResult.get("id")).longValue();

        Map<String, Object> corrResult = correlatividadService.agregarCorrelatividad(mpAId.intValue(), mpBId.intValue(), "APROBADA");
        assertTrue((Boolean) corrResult.get("success"));
        assertEquals("Correlatividad registrada correctamente para el plan.", corrResult.get("message"));

        Map<String, Object> detalleDestino = correlatividadService.getCorrelatividadesDelPlan(mpBId.intValue());
        assertNotNull(detalleDestino);
        assertEquals(mpBId, ((Number) detalleDestino.get("materiaPlanId")).longValue());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> anteriores = (List<Map<String, Object>>) detalleDestino.get("anteriores");
        assertEquals(1, anteriores.size());
        assertEquals("Programación I", anteriores.get(0).get("nombre"));
        assertEquals("APROBADA", anteriores.get(0).get("condicion"));
        assertEquals("Aprobada", anteriores.get(0).get("condicionLabel"));
    }

    @Test
    void testAgregarCorrelatividadRechazaMateriaRequisitoDeAnioPosterior() {
        createCarreraYPlan();

        Long materiaAId = ((Number) materiaService.crearMateria("MATE-101", "Programación I", "", 1).get("id")).longValue();
        Long materiaBId = ((Number) materiaService.crearMateria("MATE-102", "Programación II", "", 1).get("id")).longValue();
        Long materiaCId = ((Number) materiaService.crearMateria("MATE-103", "Programación III", "", 1).get("id")).longValue();

        Long mpAId = ((Number) materiaService.asociarPlan((int) materiaAId.longValue(), 1, 1, "PRIMERO", 80, "OBLIGATORIA", 1).get("id")).longValue();
        Long mpBId = ((Number) materiaService.asociarPlan((int) materiaBId.longValue(), 1, 2, "SEGUNDO", 80, "OBLIGATORIA", 1).get("id")).longValue();
        Long mpCId = ((Number) materiaService.asociarPlan((int) materiaCId.longValue(), 1, 3, "ANUAL", 80, "OBLIGATORIA", 1).get("id")).longValue();

        Map<String, Object> first = correlatividadService.agregarCorrelatividad((int) mpAId.longValue(), (int) mpBId.longValue(), "APROBADA");
        assertTrue((Boolean) first.get("success"));

        Map<String, Object> second = correlatividadService.agregarCorrelatividad((int) mpBId.longValue(), (int) mpCId.longValue(), "APROBADA");
        assertTrue((Boolean) second.get("success"));

        Map<String, Object> invalidDestiny = correlatividadService.agregarCorrelatividad((int) mpCId.longValue(), (int) mpAId.longValue(), "APROBADA");
        assertFalse((Boolean) invalidDestiny.get("success"));
        assertEquals("La materia requisito pertenece a un año posterior.", invalidDestiny.get("message"));
    }

    @Test
    void testDetectaCicloEnGrafoDeCorrelatividades() {
        createCarreraYPlan();

        Long materiaAId = ((Number) materiaService.crearMateria("MATE-101", "Programación I", "", 1).get("id")).longValue();
        Long materiaBId = ((Number) materiaService.crearMateria("MATE-102", "Programación II", "", 1).get("id")).longValue();
        Long materiaCId = ((Number) materiaService.crearMateria("MATE-103", "Programación III", "", 1).get("id")).longValue();

        Long mpAId = ((Number) materiaService.asociarPlan((int) materiaAId.longValue(), 1, 1, "PRIMERO", 80, "OBLIGATORIA", 1).get("id")).longValue();
        Long mpBId = ((Number) materiaService.asociarPlan((int) materiaBId.longValue(), 1, 2, "SEGUNDO", 80, "OBLIGATORIA", 1).get("id")).longValue();
        Long mpCId = ((Number) materiaService.asociarPlan((int) materiaCId.longValue(), 1, 2, "SEGUNDO", 80, "OBLIGATORIA", 1).get("id")).longValue();

        Base.exec("INSERT INTO correlatividades (materia_plan_origen_id, materia_plan_destino_id, condicion) VALUES (?, ?, ?)", mpAId, mpBId, "APROBADA");
        Base.exec("INSERT INTO correlatividades (materia_plan_origen_id, materia_plan_destino_id, condicion) VALUES (?, ?, ?)", mpBId, mpCId, "APROBADA");
        Base.exec("INSERT INTO correlatividades (materia_plan_origen_id, materia_plan_destino_id, condicion) VALUES (?, ?, ?)", mpCId, mpAId, "APROBADA");

        assertTrue(correlatividadService.detectaCiclo((int) mpCId.longValue(), (int) mpAId.longValue()));
        assertTrue(correlatividadService.detectaCiclo((int) mpAId.longValue(), (int) mpCId.longValue()));
    }

    private void createCarreraYPlan() {
        Base.exec("INSERT INTO carreras (codigo, nombre, descripcion, activa) VALUES (?, ?, ?, ?)", "ING001", "Ingeniería en Sistemas", "Carrera prueba", 1);
        Base.exec("INSERT INTO planes_estudio (carrera_id, anio_vigencia, descripcion, duracion_anios, activo) VALUES (?, ?, ?, ?, ?)", 1, 2025, "Plan 2025", 5, 1);
    }
}
