package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("asignaciones_docentes")
public class AsignacionDocente extends Model {

    public int getComisionId() { return getInteger("comision_id"); }
    public int getDocenteId() { return getInteger("docente_id"); }
    public String getCargo() { return getString("cargo"); }
    public int getAsignadoPor() { return getInteger("asignado_por"); }
    public boolean isActiva() {
        Object v = get("activa");
        if (v instanceof Boolean) return (Boolean) v;
        return v != null && ((Number) v).intValue() == 1;
    }

    public void setComisionId(int id) { set("comision_id", id); }
    public void setDocenteId(int id) { set("docente_id", id); }
    public void setCargo(String c) { set("cargo", c); }
    public void setAsignadoPor(int id) { set("asignado_por", id); }
    public void setActiva(boolean activa) { set("activa", activa ? 1 : 0); }
}
