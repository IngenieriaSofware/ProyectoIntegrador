package com.is1.proyecto.models;

import java.util.List;
import java.util.Optional;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

/**
 * Modelo de Persona - Núcleo de identidad unificada.
 * Contiene datos de DNI, nombre, apellido, email, teléfono, localidad.
 * Soporta múltiples roles (DOCENTE, ESTUDIANTE) simultáneamente.
 */
@Table("personas")
@IdName("id")
public class Persona extends Model {

    // ============ Getters y Setters ============

    public String getDni() {
        return getString("dni");
    }

    public void setDni(String dni) {
        set("dni", dni);
    }

    public String getNombre() {
        return getString("nombre");
    }

    public void setNombre(String nombre) {
        set("nombre", nombre);
    }

    public String getApellido() {
        return getString("apellido");
    }

    public void setApellido(String apellido) {
        set("apellido", apellido);
    }

    public String getEmail() {
        return getString("email");
    }

    public void setEmail(String email) {
        set("email", email);
    }

    public String getPassword() {
        return getString("password");
    }

    public void setPassword(String password) {
        set("password", password);
    }

    public String getTelefono() {
        return getString("telefono");
    }

    public void setTelefono(String telefono) {
        set("telefono", telefono);
    }

    public String getLocalidad() {
        return getString("localidad");
    }

    public void setLocalidad(String localidad) {
        set("localidad", localidad);
    }

    // ============ Relaciones ============

    /**
     * Obtiene los roles asociados a esta persona (DOCENTE, ESTUDIANTE)
     */
    public List<PersonaRole> getRoles() {
        return PersonaRole.where("persona_id = ?", getId()).orderBy("assigned_at");
    }

    /**
     * Obtiene lista de nombres de roles (Strings)
     */
    public List<String> getRoleNames() {
        List<PersonaRole> roles = getRoles();
        return roles.stream()
            .map(PersonaRole::getRol)
            .toList();
    }

    /**
     * Verifica si la persona tiene un rol específico
     */
    public boolean hasRole(String rol) {
        return getRoleNames().contains(rol);
    }

    /**
     * Verifica si la persona es DOCENTE
     */
    public boolean isDocente() {
        return hasRole("DOCENTE");
    }

    /**
     * Verifica si la persona es ESTUDIANTE
     */
    public boolean isEstudiante() {
        return hasRole("ESTUDIANTE");
    }

    // ============ Búsquedas ============

    /**
     * Busca una persona por DNI
     */
    public static Optional<Persona> findByDni(String dni) {
        Persona persona = Persona.findFirst("dni = ?", dni);
        return Optional.ofNullable(persona);
    }

    /**
     * Busca una persona por Email
     */
    public static Optional<Persona> findByEmail(String email) {
        Persona persona = Persona.findFirst("email = ?", email);
        return Optional.ofNullable(persona);
    }

    /**
     * Busca una persona por DNI o Email (para login)
     */
    public static Optional<Persona> findByDniOrEmail(String identifier) {
        Persona persona = Persona.findFirst("dni = ? OR email = ?", identifier, identifier);
        return Optional.ofNullable(persona);
    }
}
