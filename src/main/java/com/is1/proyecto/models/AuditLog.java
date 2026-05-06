package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

/**
 * Modelo de Auditoría - Registra acciones de administrador
 */
@Table("audit_logs")
public class AuditLog extends Model {

    public int getAdminId() {
        return getInteger("admin_id");
    }

    public void setAdminId(int adminId) {
        set("admin_id", adminId);
    }

    public String getAccion() {
        return getString("accion");
    }

    public void setAccion(String accion) {
        set("accion", accion);
    }

    public Integer getTargetId() {
        return getInteger("target_id");
    }

    public void setTargetId(Integer targetId) {
        set("target_id", targetId);
    }

    public String getDetalles() {
        return getString("detalles");
    }

    public void setDetalles(String detalles) {
        set("detalles", detalles);
    }
}
