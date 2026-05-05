package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

/**
 * Modelo de relación N:N entre Persona y Roles.
 * Permite que una persona tenga múltiples roles simultáneamente.
 */
@Table("persona_roles")
public class PersonaRole extends Model {

    public int getPersonaId() {
        return getInteger("persona_id");
    }

    public void setPersonaId(int personaId) {
        set("persona_id", personaId);
    }

    public String getRol() {
        return getString("rol");
    }

    public void setRol(String rol) {
        set("rol", rol);
    }
}
