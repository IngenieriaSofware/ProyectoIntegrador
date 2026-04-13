package com.is1.proyecto.config;

import com.is1.proyecto.controller.MainController;
import com.is1.proyecto.controller.UserController;
import com.is1.proyecto.controller.ProfessorController;
import spark.template.mustache.MustacheTemplateEngine;

import static spark.Spark.*;
import java.util.HashMap;
import java.util.Map;
import spark.ModelAndView;

/**
 * Configurador de rutas de la aplicación.
 * Centraliza la definición de todas las rutas y sus mapeos a controladores.
 */
public class Routes {

    private static MustacheTemplateEngine templateEngine = new MustacheTemplateEngine();

    /**
     * Configura todas las rutas de la aplicación
     */
    public static void setupRoutes() {
        // Inicializar controladores
        MainController mainController = new MainController();
        UserController userController = new UserController();
        ProfessorController professorController = new ProfessorController();

        // --- Rutas principales ---
        get("/", (req, res) -> mainController.showLogin(req, res), templateEngine);
        get("/dashboard", (req, res) -> mainController.showDashboard(req, res), templateEngine);
        get("/logout", (req, res) -> mainController.logout(req, res));
        get("/user/new", (req, res) -> mainController.showUserCreateForm(req, res), templateEngine);

        // --- Rutas de usuario ---
        get("/user/create", (req, res) -> userController.showCreateForm(req, res), templateEngine);
        post("/user/new", (req, res) -> userController.registerUser(req, res));
        post("/login", (req, res) -> userController.loginUser(req, res), templateEngine);
        post("/add_users", (req, res) -> userController.addUserAPI(req, res));

        // --- Rutas de profesor ---
        get("/professor/new", (req, res) -> professorController.showCreateForm(req, res), templateEngine);
        post("/professor/new", (req, res) -> professorController.createProfessor(req, res));
        get("/professor/list", (req, res) -> professorController.listProfessors(req, res), templateEngine);

        // --- Manejador para errores 404 ---
        notFound((req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("errorMessage", "No pudimos encontrar la página que buscabas (Error 404).");
            res.status(404);
            return templateEngine.render(new ModelAndView(model, "error.mustache"));
        });

        // --- Manejador para errores 500 ---
        exception(Exception.class, (exception, req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("errorMessage", "Error interno del servidor: " + exception.getMessage());
            res.status(500);
            res.body(templateEngine.render(new ModelAndView(model, "error.mustache")));
        });
    }
}
