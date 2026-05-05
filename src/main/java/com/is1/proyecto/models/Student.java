package com.is1.proyecto.models;

import java.util.Optional;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

/**
 * Modelo de Estudiante - Perfil académico del estudiante.
 * Referencia a Persona mediante persona_id.
 */
@Table("estudiantes")
public class Student extends Model {

    public int getPersonaId() {
        return getInteger("persona_id");
    }

    public void setPersonaId(int personaId) {
        set("persona_id", personaId);
    }

    public String getCarrera() {
        return getString("carrera");
    }

    public void setCarrera(String carrera) {
        set("carrera", carrera);
    }

    public String getPlanEstudio() {
        return getString("plan_estudio");
    }

    public void setPlanEstudio(String planEstudio) {
        set("plan_estudio", planEstudio);
    }

    public int getEstadoId() {
        return getInteger("estado_id");
    }

    public void setEstadoId(int estadoId) {
        set("estado_id", estadoId);
    }

    // ============ Relaciones ============

    /**
     * Obtiene la Persona asociada a este estudiante
     */
    public Optional<Persona> getPersona() {
        int personaId = getPersonaId();
        Persona persona = Persona.findById(personaId);
        return Optional.ofNullable(persona);
    }

    /**
     * Busca un estudiante por persona_id
     */
    public static Optional<Student> findByPersonaId(int personaId) {
        Student student = Student.findFirst("persona_id = ?", personaId);
        return Optional.ofNullable(student);
    }
}
