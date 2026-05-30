package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("carreras")
public class Carrera extends Model {

    public String getCodigo() { return getString("codigo"); }
    public String getNombre() { return getString("nombre"); }
    public String getDescripcion() { return getString("descripcion"); }
    public boolean isActiva() {
        Object v = get("activa");
        if (v instanceof Boolean) return (Boolean) v;
        return v != null && ((Number) v).intValue() == 1;
    }

    public void setCodigo(String codigo) { set("codigo", codigo.toUpperCase().replaceAll("\\s+", "")); }
    public void setNombre(String nombre) { set("nombre", nombre.trim()); }
    public void setDescripcion(String descripcion) { set("descripcion", descripcion != null ? descripcion.trim() : null); }
    public void setActiva(boolean activa) { set("activa", activa ? 1 : 0); }
    public void touch() { set("actualizada_en", new java.sql.Timestamp(System.currentTimeMillis())); }
}
