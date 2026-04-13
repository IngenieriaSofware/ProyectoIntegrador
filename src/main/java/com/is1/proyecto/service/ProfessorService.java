package com.is1.proyecto.service;

import com.is1.proyecto.models.Professor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Capa de servicio para la lógica relacionada con profesores.
 * Maneja la creación, listado y validación de profesores.
 */
public class ProfessorService {

    /**
     * Crea un nuevo profesor en la base de datos.
     * Valida que los campos obligatorios estén presentes.
     *
     * @param name       Nombre del profesor
     * @param email      Email del profesor
     * @param dni        DNI del profesor
     * @param department Departamento del profesor
     * @param phone      Teléfono del profesor
     * @return Un mapa con los resultados: {success: boolean, message: String, professorId: Long}
     */
    public Map<String, Object> createProfessor(String name, String email, String dni, String department, String phone) {
        Map<String, Object> result = new HashMap<>();

        // Validación de campo obligatorio
        if (name == null || name.isEmpty()) {
            result.put("success", false);
            result.put("message", "El nombre es obligatorio.");
            return result;
        }

        try {
            Professor professor = new Professor();
            professor.set("name", name);
            professor.set("email", email);
            professor.set("DNI", dni);
            professor.set("department", department);
            professor.set("phone", phone);
            professor.saveIt();

            result.put("success", true);
            result.put("message", "Profesor agregado correctamente.");
            result.put("professorId", professor.getId());
            return result;

        } catch (Exception e) {
            String errorMsg = e.getMessage().toLowerCase();
            String mensaje;

            if (errorMsg.contains("professors.email")) {
                mensaje = "El email ya está registrado en la base de datos.";
            } else if (errorMsg.contains("professors.dni")) {
                mensaje = "El DNI ya está registrado en la base de datos.";
            } else {
                mensaje = "Error interno al agregar profesor.";
            }

            System.err.println("Error al agregar profesor: " + e.getMessage());
            result.put("success", false);
            result.put("message", mensaje);
            return result;
        }
    }

    /**
     * Obtiene una página de profesores con paginación.
     *
     * @param pageNumber Número de página (comienza en 1)
     * @param pageSize   Cantidad de registros por página
     * @return Un mapa con: {professors: List, totalCount: Long, totalPages: Integer, currentPage: Integer}
     */
    public Map<String, Object> getProfessorsPagedList(int pageNumber, int pageSize) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Validar número de página
            if (pageNumber < 1) {
                pageNumber = 1;
            }

            // Obtener el total de profesores
            long totalCount = Professor.count();
            int totalPages = (int) Math.ceil((double) totalCount / pageSize);

            // Validar que la página no exceda el total
            if (pageNumber > totalPages && totalPages > 0) {
                pageNumber = totalPages;
            }

            // Calcular offset
            int offset = (pageNumber - 1) * pageSize;

            // Obtener profesores de esta página
            List<Map<String, Object>> professors = Professor.findAll()
                    .limit(pageSize)
                    .offset(offset)
                    .toMaps();

            result.put("professors", professors);
            result.put("totalCount", totalCount);
            result.put("totalPages", totalPages);
            result.put("currentPage", pageNumber);
            result.put("pageSize", pageSize);

            return result;

        } catch (Exception e) {
            System.err.println("Error al obtener listado de profesores: " + e.getMessage());
            result.put("professors", List.of());
            result.put("totalCount", 0L);
            result.put("totalPages", 0);
            return result;
        }
    }

    /**
     * Obtiene un profesor por su ID.
     *
     * @param id ID del profesor
     * @return El profesor encontrado o null si no existe
     */
    public Professor getProfessorById(long id) {
        return Professor.findById(id);
    }
}
