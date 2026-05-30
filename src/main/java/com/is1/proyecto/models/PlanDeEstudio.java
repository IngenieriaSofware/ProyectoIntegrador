package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("planes_estudio")
public class PlanDeEstudio extends Model {

    public int getCarreraId() { return getInteger("carrera_id"); }
    public int getAnioVigencia() { return getInteger("anio_vigencia"); }
    public String getDescripcion() { return getString("descripcion"); }
    public Integer getDuracionAnios() { return getInteger("duracion_anios"); }
    public boolean isActivo() {
        Object v = get("activo");
        if (v instanceof Boolean) return (Boolean) v;
        return v != null && ((Number) v).intValue() == 1;
    }

    public void setCarreraId(int carreraId) { set("carrera_id", carreraId); }
    public void setAnioVigencia(int anio) { set("anio_vigencia", anio); }
    public void setDescripcion(String desc) { set("descripcion", desc != null ? desc.trim() : null); }
    public void setDuracionAnios(Integer dur) { set("duracion_anios", dur); }
    public void setActivo(boolean activo) { set("activo", activo ? 1 : 0); }
    public void touch() { set("actualizado_en", new java.sql.Timestamp(System.currentTimeMillis())); }
}
