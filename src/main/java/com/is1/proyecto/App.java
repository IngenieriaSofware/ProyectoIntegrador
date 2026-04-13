package com.is1.proyecto;

import org.javalite.activejdbc.Base;
import com.is1.proyecto.config.DBConfigSingleton;
import com.is1.proyecto.config.Routes;

import static spark.Spark.*;

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

        // --- Filtro 'before': Abre conexión a BD ---
        before((req, res) -> {
            try {
                Base.open(dbConfig.getDriver(), dbConfig.getDbUrl(), dbConfig.getUser(), dbConfig.getPass());
                System.out.println("[REQUEST] " + req.requestMethod() + " " + req.url());
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