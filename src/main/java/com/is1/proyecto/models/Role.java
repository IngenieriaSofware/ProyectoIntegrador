package com.is1.proyecto.models;

/**
 * Enumeración de roles disponibles en el sistema.
 * NOTA: El rol "ADMIN" ha sido eliminado. Las funciones de gestión ahora
 * se manejan mediante permisos dinámicos asignados a personas.
 */
public enum Role {
    DOCENTE,      // Rol de docente/profesor
    ESTUDIANTE    // Rol de estudiante
}