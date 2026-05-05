package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

/**
 * Modelo TCargo - Catálogo de tipos de cargo docente.
 * Ej: Profesor Titular, Asistente, JTP
 */
@Table("tcargo")
public class TCargo extends Model {

    static {
        validatePresenceOf("nombre");
    }

    public String getNombre() {
        return getString("nombre");
    }

    public void setNombre(String nombre) {
        set("nombre", nombre);
    }

    public String getDescripcion() {
        return getString("descripcion");
    }

    public void setDescripcion(String descripcion) {
        set("descripcion", descripcion);
    }
}
