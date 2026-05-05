package com.is1.proyecto.config;

import java.util.HashMap;
import java.util.Map;

import com.is1.proyecto.controller.DocenteController;
import com.is1.proyecto.controller.EstudianteController;
import com.is1.proyecto.controller.MainController;
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
 * Sistema de Login y Control de Acceso con roles duales (N:N).
 * El rol ADMIN ha sido eliminado. Las funciones de gestión usan permisos dinámicos.
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
        DocenteController docenteController = new DocenteController();
        EstudianteController estudianteController = new EstudianteController();

        // ========================================
        // RUTAS DE AUTENTICACIÓN (sin protección)
        // ========================================

        // GET / - Página de login
        get("/", (req, res) -> mainController.showLogin(req, res), templateEngine);

        // GET /user/new - Formulario de registro (Persona)
        get("/user/new", (req, res) -> userController.showCreateForm(req, res), templateEngine);

        // POST /user/new - Registra una nueva Persona
        post("/user/new", (req, res) -> userController.registerPersona(req, res));

        // POST /login - Autentica Persona por DNI/Email
        post("/login", (req, res) -> userController.loginPersona(req, res));

        // GET /logout - Cierra sesión
        get("/logout", (req, res) -> userController.logout(req, res));

        // ========================================
        // RUTAS PROTEGIDAS: DASHBOARD
        // ========================================

        // GET /dashboard - Dashboard general (para seleccionar rol si hay múltiples)
        get("/dashboard", (req, res) -> mainController.showDashboard(req, res), templateEngine);

        // ========================================
        // RUTAS PROTEGIDAS: DOCENTE
        // ========================================

        // GET /docente - Dashboard de Docente
        get("/docente", (req, res) -> docenteController.showDocenteDashboard(req, res), templateEngine);

        // ========================================
        // RUTAS PROTEGIDAS: ESTUDIANTE
        // ========================================

        // GET /estudiante - Dashboard de Estudiante
        get("/estudiante", (req, res) -> estudianteController.showEstudianteDashboard(req, res), templateEngine);

        // ========================================
        // APLICAR MIDDLEWARE DE PROTECCIÓN
        // ========================================

        // Dashboard requiere autenticación (cualquier rol)
        before("/dashboard", AuthMiddleware.requireAuth);

        // Docente requiere rol DOCENTE
        before("/docente", AuthMiddleware.requireRole("DOCENTE"));

        // Estudiante requiere rol ESTUDIANTE
        before("/estudiante", AuthMiddleware.requireRole("ESTUDIANTE"));

        // ========================================
        // MANEJADORES DE ERRORES
        // ========================================

        // 404 - Página no encontrada
        notFound((req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("errorMessage", "No pudimos encontrar la página que buscabas (Error 404).");
            res.status(404);
            return templateEngine.render(new ModelAndView(model, "error.mustache"));
        });

        // 500 - Error interno del servidor
        exception(Exception.class, (exception, req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("errorMessage", "Error interno del servidor: " + exception.getMessage());
            res.status(500);
            res.body(templateEngine.render(new ModelAndView(model, "error.mustache")));
        });
    }
}