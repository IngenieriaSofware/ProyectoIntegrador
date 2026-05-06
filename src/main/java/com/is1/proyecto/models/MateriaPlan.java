package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("materias_planes")
public class MateriaPlan extends Model {

    public int getMateriaId() { return getInteger("materia_id"); }
    public int getPlanEstudioId() { return getInteger("plan_estudio_id"); }
    public int getAnioPlan() { return getInteger("anio_plan"); }
    public String getCuatrimestre() { return getString("cuatrimestre"); }
    public int getCargaHoraria() { return getInteger("carga_horaria"); }
    public String getCaracter() { return getString("caracter"); }
    public boolean isActiva() {
        Object v = get("activa");
        if (v instanceof Boolean) return (Boolean) v;
        return v != null && ((Number) v).intValue() == 1;
    }

    public void setMateriaId(int id) { set("materia_id", id); }
    public void setPlanEstudioId(int id) { set("plan_estudio_id", id); }
    public void setAnioPlan(int anio) { set("anio_plan", anio); }
    public void setCuatrimestre(String c) { set("cuatrimestre", c); }
    public void setCargaHoraria(int h) { set("carga_horaria", h); }
    public void setCaracter(String c) { set("caracter", c); }
    public void setActiva(boolean activa) { set("activa", activa ? 1 : 0); }
    public void setCreadoPor(int adminId) { set("creado_por", adminId); }
    public void touch() { set("actualizado_en", new java.sql.Timestamp(System.currentTimeMillis())); }
}
