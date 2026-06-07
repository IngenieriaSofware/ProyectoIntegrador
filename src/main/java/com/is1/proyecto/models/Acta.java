package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("actas")
public class Acta extends Model {

    public int getComisionId() { return getInteger("comision_id"); }
    public int getPeriodoId() { return getInteger("periodo_id"); }
    public String getInstancia() { return getString("instancia"); }
    public String getEstado() { return getString("estado"); }
    public Object getFechaCierre() { return get("fecha_cierre"); }
    public Object getCerradaPorDocenteId() { return get("cerrada_por_docente_id"); }
    public Object getReabiertaPorAdminId() { return get("reabierta_por_admin_id"); }
    public Object getFechaReapertura() { return get("fecha_reapertura"); }

    public void setComisionId(int id) { set("comision_id", id); }
    public void setPeriodoId(int id) { set("periodo_id", id); }
    public void setInstancia(String i) { set("instancia", i); }
    public void setEstado(String e) { set("estado", e); }
    public void setFechaCierre(java.sql.Timestamp t) { set("fecha_cierre", t); }
    public void setCerradaPorDocenteId(int id) { set("cerrada_por_docente_id", id); }
    public void setReabiertaPorAdminId(int id) { set("reabierta_por_admin_id", id); }
    public void setFechaReapertura(java.sql.Timestamp t) { set("fecha_reapertura", t); }
    public void touch() { set("updated_at", new java.sql.Timestamp(System.currentTimeMillis())); }

    public boolean isAbierta() { return "abierta".equals(getEstado()); }
    public boolean isCerrada() { return "cerrada".equals(getEstado()); }
}
