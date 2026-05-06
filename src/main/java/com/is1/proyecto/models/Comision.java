package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("comisiones")
public class Comision extends Model {

    public int getMateriaId() { return getInteger("materia_id"); }
    public int getPlanEstudioId() { return getInteger("plan_estudio_id"); }
    public int getPeriodoId() { return getInteger("periodo_id"); }
    public String getTurno() { return getString("turno"); }
    public String getNombre() { return getString("nombre"); }
    public String getEstado() { return getString("estado"); }
    public boolean isActiva() { return "ACTIVA".equals(getEstado()); }

    public void setMateriaId(int id) { set("materia_id", id); }
    public void setPlanEstudioId(int id) { set("plan_estudio_id", id); }
    public void setPeriodoId(int id) { set("periodo_id", id); }
    public void setTurno(String t) { set("turno", t); }
    public void setNombre(String n) { set("nombre", n); }
    public void setEstado(String e) { set("estado", e); }
    public void setCreadaPor(int adminId) { set("creada_por", adminId); }
    public void touch() { set("actualizada_en", new java.sql.Timestamp(System.currentTimeMillis())); }
}
