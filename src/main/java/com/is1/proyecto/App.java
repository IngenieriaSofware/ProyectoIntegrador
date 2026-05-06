package com.is1.proyecto;

import org.javalite.activejdbc.Base;

import com.is1.proyecto.config.DBConfigSingleton;
import com.is1.proyecto.config.Routes;

import static spark.Spark.after;
import static spark.Spark.before;
import static spark.Spark.halt;
import static spark.Spark.port;
import static spark.Spark.staticFiles;

/**
 * Clase principal de la aplicación.
 * Inicia el servidor web con Spark.
 */
public class App {

    public static void main(String[] args) {
        // Configurar archivos estáticos
        staticFiles.location("/static");

        // Configurar puerto
        port(8080);

        // Obtener configuración de la base de datos
        DBConfigSingleton dbConfig = DBConfigSingleton.getInstance();

        // --- Inicialización de Tablas (Solo si no existen) ---
        try {
            Base.open(dbConfig.getDriver(), dbConfig.getDbUrl(), dbConfig.getUser(), dbConfig.getPass());
            java.io.InputStream is = App.class.getResourceAsStream("/scheme.sql");
            if (is != null) {
                String schema = new java.util.Scanner(is).useDelimiter("\\A").next();
                int ok = 0, skip = 0;
                for (String sql : schema.split(";")) {
                    String stmt = sql.trim();
                    if (stmt.isEmpty()) continue;
                    try {
                        Base.exec(stmt);
                        ok++;
                    } catch (Exception e) {
                        // ALTER TABLE con columna ya existente u otro conflicto idempotente — ignorar
                        System.out.println("[SCHEMA SKIP] " + e.getMessage().split("\n")[0]);
                        skip++;
                    }
                }
                System.out.println("✓ Esquema aplicado: " + ok + " sentencias OK, " + skip + " omitidas.");
            }
            Base.close();
        } catch (Exception e) {
            System.err.println("Error crítico al inicializar base de datos: " + e.getMessage());
            if (Base.hasConnection()) Base.close();
        }

        // --- Filtro 'before': Abre conexión a BD ---
        before((req, res) -> {
            try {
                Base.open(dbConfig.getDriver(), dbConfig.getDbUrl(), dbConfig.getUser(), dbConfig.getPass());
                System.out.println("[REQUEST] " + req.requestMethod() + " " + req.url());
                System.out.println("[DB] url=" + dbConfig.getDbUrl());
            } catch (Exception e) {
                System.err.println("Error al abrir conexión con BD: " + e.getMessage());
                halt(500, "{\"error\": \"Error al conectar a la base de datos\"}");
            }
        });

        // --- Filtro 'after': Cierra conexión a BD ---
        after((req, res) -> {
            try {
                Base.close();
            } catch (Exception e) {
                System.err.println("Error al cerrar conexión con BD: " + e.getMessage());
            }
        });

        // --- Configurar rutas de la aplicación ---
        Routes.setupRoutes();

        System.out.println("✓ Aplicación iniciada en http://localhost:8080");
    }
}