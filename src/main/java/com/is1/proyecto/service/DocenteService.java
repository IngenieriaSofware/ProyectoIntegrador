package com.is1.proyecto.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.is1.proyecto.models.Docente;

public class DocenteService {

    private UserService userService;

    public DocenteService() {
        this.userService = new UserService();
    }

    /**
     * Crea un nuevo docente: Persona + perfil Docente + rol DOCENTE.
     * Delega la creación a UserService para mantener consistencia.
     */
    public Map<String, Object> createProfessor(String dni, String nombre, String apellido,
                                               String email, String password, String telefono,
                                               String localidad, String departamento) {
        return userService.registerPersonaConRoles(
            dni, nombre, apellido, email, password, telefono, localidad,
            List.of("DOCENTE"), departamento
        );
    }

    public Map<String, Object> getProfessorsPagedList(int pageNumber, int pageSize) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (pageNumber < 1) pageNumber = 1;

            long totalCount = Docente.count();
            int totalPages = (int) Math.ceil((double) totalCount / pageSize);

            if (pageNumber > totalPages && totalPages > 0) pageNumber = totalPages;

            int offset = (pageNumber - 1) * pageSize;

            List<Map<String, Object>> professors = Docente.findAll()
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

    public Docente getProfessorById(long id) {
        return Docente.findById(id);
    }
}
