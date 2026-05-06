package com.is1.proyecto.service;

import java.util.List;

import com.is1.proyecto.models.Permiso;
import com.is1.proyecto.models.Persona;

/**
 * Servicio de permisos - Gestión de accesos basada en:
 * 1. Roles dinámicos (DOCENTE, ESTUDIANTE, ADMINISTRADOR ) asignados a Persona
 * 2. Permisos específicos asignados a Persona
 * 
 */
public class PermissionService {

    /**
     * Verifica si una persona tiene un rol específico
     */
    public static boolean hasRole(Persona persona, String rol) {
        if (persona == null) return false;
        return persona.getRoleNames().contains(rol);
    }

    /**
     * Verifica si una persona es DOCENTE
     */
    public static boolean isDocente(Persona persona) {
        return hasRole(persona, "DOCENTE");
    }

    /**
     * Verifica si una persona es ESTUDIANTE
     */
    public static boolean isEstudiante(Persona persona) {
        return hasRole(persona, "ESTUDIANTE");
    }

    /**
     * Verifica si una persona es ADMINISTRADOR
    */
    public static boolean isAdministrador(Persona persona) {
        return hasRole(persona, "ADMINISTRADOR");
    }

    /**
     * Verifica si una persona tiene múltiples roles (Docente Y Estudiante)
     */
    public static boolean hasMultipleRoles(Persona persona) {
        if (persona == null) return false;
        return persona.getRoleNames().size() > 1;
    }

    /**
     * Verifica si una persona tiene un permiso específico
     * Busca en la tabla persona_permisos
     */
    public static boolean hasPermission(Persona persona, String permissionName) {
        if (persona == null) return false;
        
        // Buscar permisos asignados a esta persona
        List<Permiso> assignedPermisos = Permiso.where(
            "id IN (SELECT permiso_id FROM persona_permisos WHERE persona_id = ?)", 
            persona.getId()
        );
        
        return assignedPermisos.stream()
            .anyMatch(p -> permissionName.equals(p.getNombre()));
    }

    /**
     * Verifica si una persona puede gestionar estudiantes
     * Requiere permiso "gestionar_estudiantes"
     */
    public static boolean canManageStudents(Persona persona) {
        return hasPermission(persona, "gestionar_estudiantes");
    }

    /**
     * Verifica si una persona puede gestionar docentes
     * Requiere permiso "gestionar_docentes"
     */
    public static boolean canManageProfessors(Persona persona) {
        return hasPermission(persona, "gestionar_docentes");
    }

    /**
     * Verifica si una persona puede ver reportes
     * Requiere permiso "ver_reportes"
     */
    public static boolean canViewReports(Persona persona) {
        return hasPermission(persona, "ver_reportes");
    }

    /**
     * Verifica si una persona puede gestionar calificaciones
     * DOCENTES: requiere permiso "gestionar_calificaciones"
     */
    public static boolean canManageGrades(Persona persona) {
        if (persona == null) return false;
        
        // Si es docente Y tiene permiso específico
        return isDocente(persona) && hasPermission(persona, "gestionar_calificaciones");
    }

    /**
     * Verifica si una persona puede ver estudiantes de su cátedra
     * DOCENTES: requiere permiso "ver_estudiantes_catedra"
     */
    public static boolean canViewCourseStudents(Persona persona) {
        if (persona == null) return false;
        
        return isDocente(persona) && hasPermission(persona, "ver_estudiantes_catedra");
    }

    /**
     * Verifica si una persona puede crear evaluaciones
     * DOCENTES: requiere permiso "crear_evaluaciones"
     */
    public static boolean canCreateEvaluations(Persona persona) {
        if (persona == null) return false;
        
        return isDocente(persona) && hasPermission(persona, "crear_evaluaciones");
    }

    /**
     * Verifica si una persona puede ver sus propios datos
     * Siempre permitido para datos propios
     */
    public static boolean canViewOwnData(Persona persona, int personaId) {
        return persona != null && ((Number) persona.getId()).intValue() == personaId;
    }

    /**
     * Verifica si una persona puede editar sus propios datos
     * Siempre permitido para datos propios
     */
    public static boolean canEditOwnData(Persona persona, int personaId) {
        return persona != null && ((Number) persona.getId()).intValue() == personaId;
    }

    /**
     * Valida si un rol es válido en el nuevo esquema
     */
    public static boolean isValidRole(String rol) {
        return "DOCENTE".equals(rol) || "ESTUDIANTE".equals(rol) || "ADMINISTRADOR".equals(rol);
    }
}
