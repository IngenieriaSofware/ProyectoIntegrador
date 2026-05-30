package com.is1.proyecto.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.javalite.activejdbc.LazyList;

import com.is1.proyecto.models.Docente;
import com.is1.proyecto.models.Estudiante;
import com.is1.proyecto.models.Persona;
import com.is1.proyecto.models.PersonaRole;

/**
 * Servicio de Administrador - Funciones de gestión de sistema.
 * Manejo de usuarios, roles, catálogos y auditoría.
 */
public class AdminService {

    public List<Map<String, Object>> listPersonas(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        
        org.javalite.activejdbc.LazyList<Persona> personas = Persona.findAll()
                .offset(offset)
                .limit(pageSize)
                .orderBy("created_at desc"); 

        return personas.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("dni", p.getDni());
            map.put("nombre", p.getNombre());
            map.put("apellido", p.getApellido());
            map.put("email", p.getEmail());
            map.put("roles", p.getRoleNames());
            map.put("createdAt", p.get("created_at"));
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * Busca personas por criterio (nombre, dni, email)
     * Ideal para el panel de Administrador.
     */
    public List<Map<String, Object>> searchPersonas(String query) {
        // 1. Normalización del término de búsqueda
        String searchTerm = "%" + query + "%";
        
        // 2. Uso de LazyList para evitar el error de "illegal start" o tipos incompatibles
        org.javalite.activejdbc.LazyList<Persona> personas = Persona.where(
            "nombre LIKE ? OR apellido LIKE ? OR dni LIKE ? OR email LIKE ?",
            searchTerm, searchTerm, searchTerm, searchTerm
        ).orderBy("apellido asc, nombre asc"); // Ordenamos alfabéticamente para el Admin

        // 3. Mapeo a la estructura que consume el Frontend
        return personas.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("dni", p.getDni());
            map.put("nombre", p.getNombre());
            map.put("apellido", p.getApellido());
            map.put("email", p.getEmail());
            map.put("roles", p.getRoleNames()); // Recupera DOCENTE, ESTUDIANTE, etc.[cite: 4, 5]
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * Obtiene detalles de una persona
     */
    public Map<String, Object> getPersonaDetails(int personaId) {
        Persona persona = Persona.findById(personaId);
        if (persona == null) return null;
        
        Map<String, Object> map = new HashMap<>();
        map.put("id", persona.getId());
        map.put("dni", persona.getDni());
        map.put("nombre", persona.getNombre());
        map.put("apellido", persona.getApellido());
        map.put("email", persona.getEmail());
        map.put("telefono", persona.getTelefono());
        map.put("localidad", persona.getLocalidad());
        map.put("roles", persona.getRoleNames());
        map.put("createdAt", persona.get("created_at"));
        
        // Incluir detalles de rol específico
        if (persona.isEstudiante()) {
            Estudiante estudiante = Estudiante.findFirst("persona_id = ?", personaId);
            if (estudiante != null) {
                map.put("studentInfo", Map.of(
                    "carrera", estudiante.get("carrera"),
                    "planEstudio", estudiante.get("plan_estudio")
                ));
            }
        }
        if (persona.isDocente()) {
            Docente prof = Docente.findFirst("persona_id = ?", personaId);
            if (prof != null) {
                map.put("professorInfo", Map.of(
                    "cargoId", prof.get("cargo_id")
                ));
            }
        }
        
        return map;
    }

    /**
     * Asigna un rol a una persona (solo ADMINISTRADOR puede)
     */
    public Map<String, Object> assignRole(int personaId, String rolNuevo) {
        Map<String, Object> result = new HashMap<>();
        
        if (!PermissionService.isValidRole(rolNuevo)) {
            result.put("success", false);
            result.put("message", "Rol inválido.");
            return result;
        }
        
        try {
            Persona persona = Persona.findById(personaId);
            if (persona == null) {
                result.put("success", false);
                result.put("message", "Persona no encontrada.");
                return result;
            }
            
            // Si ya tiene el rol, no hacer nada
            if (persona.hasRole(rolNuevo)) {
                result.put("success", false);
                result.put("message", "Persona ya tiene este rol.");
                return result;
            }
            
            // Crear entrada en persona_roles
            PersonaRole personaRole = new PersonaRole();
            personaRole.set("persona_id", personaId);
            personaRole.set("rol", rolNuevo);
            personaRole.saveIt();
            
            result.put("success", true);
            result.put("message", "Rol asignado exitosamente.");
            return result;
            
        } catch (Exception e) {
            System.err.println("Error al asignar rol: " + e.getMessage());
            result.put("success", false);
            result.put("message", "Error al asignar rol.");
            return result;
        }
    }

    /**
     * Revoca un rol de una persona
     */
    public Map<String, Object> revokeRole(int personaId, String rol) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Persona persona = Persona.findById(personaId);
            if (persona == null) {
                result.put("success", false);
                result.put("message", "Persona no encontrada.");
                return result;
            }
            
            // Evitar que pierda todos los roles
            List<String> rolesActuales = persona.getRoleNames();
            if (rolesActuales.size() <= 1) {
                result.put("success", false);
                result.put("message", "No se puede revocar el único rol de una persona.");
                return result;
            }
            
            // Eliminar rol
            PersonaRole.delete("persona_id = ? AND rol = ?", personaId, rol);
            
            result.put("success", true);
            result.put("message", "Rol revocado exitosamente.");
            return result;
            
        } catch (Exception e) {
            System.err.println("Error al revocar rol: " + e.getMessage());
            result.put("success", false);
            result.put("message", "Error al revocar rol.");
            return result;
        }
    }

    /**
     * Habilita una cuenta (marca como activa)
     */
    public Map<String, Object> enableUser(int personaId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Persona persona = Persona.findById(personaId);
            if (persona == null) {
                result.put("success", false);
                result.put("message", "Persona no encontrada.");
                return result;
            }
            
            persona.set("enabled", 1);
            persona.set("ban_reason", null);
            persona.saveIt();
            
            result.put("success", true);
            result.put("message", "Usuario habilitado.");
            return result;
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error al habilitar usuario.");
            return result;
        }
    }

    /**
     * Deshabilita una cuenta (banning)
     */
    public Map<String, Object> disableUser(int personaId, String razon) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Persona persona = Persona.findById(personaId);
            if (persona == null) {
                result.put("success", false);
                result.put("message", "Persona no encontrada.");
                return result;
            }
            
            persona.set("enabled", 0);
            persona.set("ban_reason", razon);
            persona.saveIt();
            
            result.put("success", true);
            result.put("message", "Usuario deshabilitado.");
            return result;
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error al deshabilitar usuario.");
            return result;
        }
    }

    /**
     * Cambia la contraseña de un usuario (Admin override)
     */
    public Map<String, Object> changeUserPassword(int personaId, String newPassword) {
        Map<String, Object> result = new HashMap<>();
        
        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 30) {
            result.put("success", false);
            result.put("message", "Contraseña debe tener 8-30 caracteres.");
            return result;
        }
        
        try {
            Persona persona = Persona.findById(personaId);
            if (persona == null) {
                result.put("success", false);
                result.put("message", "Persona no encontrada.");
                return result;
            }
            
            AuthService authService = new AuthService();
            persona.set("password", authService.hashPassword(newPassword));
            persona.saveIt();
            
            result.put("success", true);
            result.put("message", "Contraseña actualizada.");
            return result;
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error al cambiar contraseña.");
            return result;
        }
    }

    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            LazyList<Persona> allPersonas = Persona.findAll();

            long docentesCount = allPersonas.stream()
                .filter(p -> p.isDocente())
                .count();

            long estudiantesCount = allPersonas.stream()
                .filter(p -> p.isEstudiante())
                .count();

            long administradoresCount = allPersonas.stream()
                .filter(p -> PermissionService.isAdministrador(p))
                .count();

            stats.put("totalPersonas", allPersonas.size());
            stats.put("docentesCount", docentesCount);
            stats.put("estudiantesCount", estudiantesCount);
            stats.put("administradoresCount", administradoresCount);

            return stats;

        } catch (Exception e) {
            System.err.println("Error getting system stats: " + e.getMessage());
            return Map.of("error", "Error fetching statistics");
        }
    }
}
