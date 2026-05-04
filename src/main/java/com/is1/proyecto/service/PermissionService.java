package com.is1.proyecto.service;

public class PermissionService {

    public enum Role {
        ADMIN, DOCENTE, ESTUDIANTE
    }

    public static boolean isAdmin(String role) {
        return Role.ADMIN.name().equals(role);
    }

    public static boolean isDocente(String role) {
        return Role.DOCENTE.name().equals(role);
    }

    public static boolean isEstudiante(String role) {
        return Role.ESTUDIANTE.name().equals(role);
    }

    public static boolean canViewAllData(String role) {
        return isAdmin(role);
    }

    public static boolean canEditAllData(String role) {
        return isAdmin(role);
    }

    public static boolean canCreateUser(String role) {
        return isAdmin(role);
    }

    public static boolean canCreateProfessor(String role) {
        return isAdmin(role) || isDocente(role);
    }

    public static boolean canViewProfessor(String role, String professorOwnerId, String currentUserId) {
        if (isAdmin(role)) return true;
        if (isDocente(role) && professorOwnerId.equals(currentUserId)) return true;
        return false;
    }

    public static boolean canEditProfessor(String role, String professorOwnerId, String currentUserId) {
        if (isAdmin(role)) return true;
        if (isDocente(role) && professorOwnerId.equals(currentUserId)) return true;
        return false;
    }

    public static boolean canViewUserData(String role, String dataOwnerId, String currentUserId) {
        if (isAdmin(role)) return true;
        if (dataOwnerId.equals(currentUserId)) return true;
        return false;
    }

    public static boolean canEditUserData(String role, String dataOwnerId, String currentUserId) {
        if (isAdmin(role)) return true;
        if (isEstudiante(role)) return false;
        if (isDocente(role)) return dataOwnerId.equals(currentUserId);
        return false;
    }

    public static boolean isValidRole(String role) {
        try {
            Role.valueOf(role.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
