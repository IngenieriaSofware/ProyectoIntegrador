package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("correlatividades")
public class Correlatividad extends Model {

    public int getMateriaPlanOrigenId() {
        return getInteger("materia_plan_origen_id"); 
    }
    public int getMateriaPlanDestinoId() { 
        return getInteger("materia_plan_destino_id"); 
    }
    public String getCondicion() { 
        return getString("condicion"); // "REGULAR" o "APROBADA"
    }

    public void setMateriaPlanOrigenId(int id) { 
        set("materia_plan_origen_id", id); 
    }
    public void setMateriaPlanDestinoId(int id) { 
        set("materia_plan_destino_id", id); 
    }
    public void setCondicion(String condicion) { 
        set("condicion", condicion.toUpperCase()); 
    }
}