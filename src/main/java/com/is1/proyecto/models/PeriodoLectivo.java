package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("periodos_lectivos")
public class PeriodoLectivo extends Model {

    public int getAnio() { return getInteger("anio"); }
    public int getCuatrimestre() { return getInteger("cuatrimestre"); }
    public String getDescripcion() { return getString("descripcion"); }
    public boolean isActivo() {
        Object v = get("activo");
        if (v instanceof Boolean) return (Boolean) v;
        return v != null && ((Number) v).intValue() == 1;
    }

    public void setAnio(int anio) { set("anio", anio); }
    public void setCuatrimestre(int c) { set("cuatrimestre", c); }
    public void setDescripcion(String d) { set("descripcion", d != null ? d.trim() : null); }
    public void setActivo(boolean activo) { set("activo", activo ? 1 : 0); }

    public String getLabel() {
        int c = getCuatrimestre();
        return getAnio() + " — " + (c == 1 ? "Primer" : "Segundo") + " Cuatrimestre";
    }
}
