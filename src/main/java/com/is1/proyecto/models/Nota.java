package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("notas")
public class Nota extends Model {

    public int getActaId() { return getInteger("acta_id"); }
    public int getEstudianteId() { return getInteger("estudiante_id"); }
    public Double getValor() {
        Object v = get("valor");
        if (v == null) return null;
        return ((Number) v).doubleValue();
    }
    public boolean isAusente() {
        Object v = get("ausente");
        if (v == null) return false;
        if (v instanceof Boolean) return (Boolean) v;
        return ((Number) v).intValue() == 1;
    }
    public int getCargadaPorDocenteId() { return getInteger("cargada_por_docente_id"); }
    public Object getUltimaModificacionPor() { return get("ultima_modificacion_por"); }

    public void setActaId(int id) { set("acta_id", id); }
    public void setEstudianteId(int id) { set("estudiante_id", id); }
    public void setValor(Double valor) { set("valor", valor); }
    public void setAusente(boolean ausente) { set("ausente", ausente ? 1 : 0); }
    public void setCargadaPorDocenteId(int id) { set("cargada_por_docente_id", id); }
    public void setUltimaModificacionPor(int id) { set("ultima_modificacion_por", id); }
    public void touch() { set("updated_at", new java.sql.Timestamp(System.currentTimeMillis())); }
}
