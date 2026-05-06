package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

/**
 * Modelo de Administrador - Perfil académico del administrador.
 * Referencia a Persona mediante persona_id.
 */
@Table("administradores")
public class Administrador extends Model {

    public int getPersonaId() {
        return getInteger("persona_id");
    }

    public void setPersonaId(int personaId) {
        set("persona_id", personaId);
    }

    
}