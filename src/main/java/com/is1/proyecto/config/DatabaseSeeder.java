package com.is1.proyecto.config;

import org.javalite.activejdbc.Base;

import com.is1.proyecto.service.AuthService;

public class DatabaseSeeder {

    public static void run() {
        Long count = Base.count("persona_roles", "rol = ?", "ADMINISTRADOR");
        if (count != null && count > 0) {
            // Ya existe, solo mostrar credenciales
            printCredentials();
            return;
        }

        AuthService auth = new AuthService();
        String hashed = auth.hashPassword("Admin1234");

        try {
            Base.exec(
                "INSERT OR IGNORE INTO personas (dni, nombre, apellido, email, password, enabled) " +
                "VALUES (?, ?, ?, ?, ?, 1)",
                "00000000", "Admin", "Sistema", "admin@sistema.com", hashed
            );
            Long personaId = ((Number) Base.firstCell("SELECT id FROM personas WHERE email = ?", "admin@sistema.com")).longValue();
            Base.exec("INSERT OR IGNORE INTO persona_roles (persona_id, rol) VALUES (?, ?)", personaId, "ADMINISTRADOR");
            Base.exec("INSERT OR IGNORE INTO administradores (persona_id) VALUES (?)", personaId);

            printCredentials();
        } catch (Exception e) {
            System.err.println("[Seeder] Error: " + e.getMessage());
        }
    }

    private static void printCredentials() {
        String email = (String) Base.firstCell("SELECT email FROM personas p JOIN persona_roles pr ON p.id = pr.persona_id WHERE pr.rol = 'ADMINISTRADOR' LIMIT 1");
        System.out.println("Administrador creado o ya existente:");
        System.out.println("Email:    " + email);
        System.out.println("Password: Admin1234");
    }
}