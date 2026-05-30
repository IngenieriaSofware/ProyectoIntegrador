package com.is1.proyecto.models;

import java.util.Optional;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

/**
 * Modelo de Profesor/Docente - Perfil académico del docente.
 * Referencia a Persona mediante persona_id.
 */
@Table("docentes")
public class Docente extends Model {

    public int getPersonaId() {
        return getInteger("persona_id");
    }

    public void setPersonaId(int personaId) {
        set("persona_id", personaId);
    }

    public int getCargoId() {
        return getInteger("cargo_id");
    }

    public void setCargoId(int cargoId) {
        set("cargo_id", cargoId);
    }

    public String getResponsable() {
        return getString("responsable");
    }

    public void setResponsable(String responsable) {
        set("responsable", responsable);
    }

    public String getPeriodo() {
        return getString("periodo");
    }

    public void setPeriodo(String periodo) {
        set("periodo", periodo);
    }

    // ============ Relaciones ============

    /**
     * Obtiene la Persona asociada a este docente
     */
    public Optional<Persona> getPersona() {
        int personaId = getPersonaId();
        Persona persona = Persona.findById(personaId);
        return Optional.ofNullable(persona);
    }

    /**
     * Busca un profesor por persona_id
     */
    public static Optional<Docente> findByPersonaId(int personaId) {
        Docente professor = Docente.findFirst("persona_id = ?", personaId);
        return Optional.ofNullable(professor);
    }
}
