package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

/**
 * Modelo para inscripción de estudiantes a comisiones.
 */
@Table("inscripciones_comisiones")
public class InscripcionComision extends Model {

    public int getEstudianteId() { return getInteger("estudiante_id"); }
    public int getComisionId() { return getInteger("comision_id"); }
    public String getEstado() { return getString("estado"); }

    public void setEstudianteId(int id) { set("estudiante_id", id); }
    public void setComisionId(int id) { set("comision_id", id); }
    public void setEstado(String estado) { set("estado", estado); }
}
