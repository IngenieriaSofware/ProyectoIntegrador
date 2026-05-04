package com.is1.proyecto.config;

import java.util.HashMap;
import java.util.Map;

import com.is1.proyecto.controller.AdminController;
import com.is1.proyecto.controller.DocenteController;
import com.is1.proyecto.controller.EstudianteController;
import com.is1.proyecto.controller.MainController;
import com.is1.proyecto.controller.ProfessorController;
import com.is1.proyecto.controller.UserController;
import com.is1.proyecto.middleware.AuthMiddleware;

import spark.ModelAndView;
import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.notFound;
import static spark.Spark.post;
import spark.template.mustache.MustacheTemplateEngine;

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
        AdminController adminController = new AdminController();
        DocenteController docenteController = new DocenteController();
        EstudianteController estudianteController = new EstudianteController();

        // --- Rutas principales ---
        get("/", (req, res) -> mainController.showLogin(req, res), templateEngine);
        get("/dashboard", (req, res) -> mainController.showDashboard(req, res), templateEngine);
        get("/logout", (req, res) -> mainController.logout(req, res));
        get("/user/new", (req, res) -> mainController.showUserCreateForm(req, res), templateEngine);

        // --- Rutas de usuario ---
        get("/user/create", (req, res) -> userController.showCreateForm(req, res), templateEngine);
        post("/user/new", (req, res) -> userController.registerUser(req, res));
        
        // CORRECCIÓN: Se quita el templateEngine porque el controlador maneja la redirección/JSON
        post("/login", (req, res) -> userController.loginUser(req, res));
        
        post("/add_users", (req, res) -> userController.addUserAPI(req, res));

        // --- Rutas de profesor ---
        get("/professor/new", (req, res) -> professorController.showCreateForm(req, res), templateEngine);
        post("/professor/new", (req, res) -> professorController.createProfessor(req, res));
        get("/professor/list", (req, res) -> professorController.listProfessors(req, res), templateEngine);

        // --- Rutas por rol ---
        get("/admin", (req, res) -> adminController.showAdminDashboard(req, res), templateEngine);
        get("/docente", (req, res) -> docenteController.showDocenteDashboard(req, res), templateEngine);
        get("/estudiante", (req, res) -> estudianteController.showEstudianteDashboard(req, res), templateEngine);

        // --- Rutas protegidas (Middleware optimizado) ---
        before("/admin", AuthMiddleware.requireRole("ADMIN"));
        before("/docente", AuthMiddleware.requireRole("DOCENTE"));
        before("/estudiante", AuthMiddleware.requireRole("ESTUDIANTE"));

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