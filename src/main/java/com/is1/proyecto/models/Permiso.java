package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

/**
 * Modelo Permiso - Catálogo de permisos dinámicos.
 * Define permisos que pueden asignarse a personas para gestión de accesos.
 */
@Table("permisos")
public class Permiso extends Model {

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

    public String getCategoria() {
        return getString("categoria");
    }

    public void setCategoria(String categoria) {
        set("categoria", categoria);
    }
}
