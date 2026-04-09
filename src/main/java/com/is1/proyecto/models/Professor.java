package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;

public class Professor extends Model {
    static {
        // Validaciones básicas
        validatePresenceOf("email");
        validatePresenceOf("name");
        validatePresenceOf("DNI");
        validateEmailOf("email").message("Debe ser un correo válido");
    }
}
