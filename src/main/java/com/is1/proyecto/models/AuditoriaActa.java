package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("auditoria_acta")
public class AuditoriaActa extends Model {

    public static void registrar(int actaId, String accion, int realizadoPor,
                                  String detalle, String motivo) {
        AuditoriaActa entry = new AuditoriaActa();
        entry.set("acta_id", actaId);
        entry.set("accion", accion);
        entry.set("realizado_por", realizadoPor);
        entry.set("detalle", detalle);
        entry.set("motivo", motivo);
        entry.saveIt();
    }
}
