package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

/**
 * Modelo TEstado - Catálogo de estados de estudiante.
 * Ej: Activo, Inactivo, Suspendido
 */
@Table("testado")
public class TEstado extends Model {

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
