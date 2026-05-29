package com.is1.proyecto.config;

import java.util.HashMap;
import java.util.Map;

import com.is1.proyecto.controller.AdministradorController;
import com.is1.proyecto.controller.CarreraController;
import com.is1.proyecto.controller.MateriaController;
import com.is1.proyecto.controller.PeriodoController;
import com.is1.proyecto.controller.ComisionController;
import com.is1.proyecto.controller.DocenteController;
import com.is1.proyecto.controller.DocenteCrudController;
import com.is1.proyecto.controller.EnConstruccionController;
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
        DocenteCrudController docenteCrudController = new DocenteCrudController();
        EstudianteController estudianteController = new EstudianteController();
        AdministradorController administradorController = new AdministradorController();
        EnConstruccionController enConstruccionController = new EnConstruccionController();
        CarreraController carreraController = new CarreraController();
        MateriaController materiaController = new MateriaController();
        PeriodoController periodoController = new PeriodoController();
        ComisionController comisionController = new ComisionController();
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

        // CRUD de Docentes
        get("/docente/new", (req, res) -> docenteCrudController.showCreateForm(req, res), templateEngine);
        post("/docente/new", (req, res) -> docenteCrudController.createProfessor(req, res));
        get("/docente/list", (req, res) -> docenteCrudController.listProfessors(req, res), templateEngine);

        // ========================================
        // RUTAS PROTEGIDAS: ESTUDIANTE
        // ========================================

        // GET /estudiante - Dashboard de Estudiante
        get("/estudiante", (req, res) -> estudianteController.showEstudianteDashboard(req, res), templateEngine);
        get("/estudiante/inscripcion/carreras", (req, res) -> estudianteController.showInscripcionCarreras(req, res), templateEngine);
        get("/estudiante/inscripcion/carreras/:id", (req, res) -> estudianteController.showInscripcionCarreraDetalle(req, res), templateEngine);
        post("/estudiante/inscripcion/carreras/:id", (req, res) -> estudianteController.enrollCarrera(req, res));

        // Inscripción a materias
        get("/estudiante/materias", (req, res) -> estudianteController.showMisMaticas(req, res), templateEngine);
        post("/estudiante/inscribirse-comision", (req, res) -> estudianteController.inscribirseAComision(req, res));
        get("/estudiante/mis-cursos", (req, res) -> estudianteController.showMisCursos(req, res), templateEngine);

        // ========================================
        // RUTAS PROTEGIDAS: ADMINISTRADOR (si se mantiene el rol)
        // GET /admin - Dashboard de Administrador
        get("/admin", (req, res) -> administradorController.showAdministradorDashboard(req, res), templateEngine);

        // CRUD de Personas (Usuarios)
        get("/persona/new",  (req, res) -> administradorController.showPersonaForm(req, res), templateEngine);
        post("/persona/new", (req, res) -> administradorController.createPersona(req, res));
        before("/persona/new", AuthMiddleware.requireRole("ADMINISTRADOR"));

        // API - Gestión de Usuarios
        get("/api/admin/usuarios", (req, res) -> administradorController.listUsuarios(req, res));
        get("/api/admin/usuarios/search", (req, res) -> administradorController.searchUsuarios(req, res));
        get("/api/admin/usuarios/:id", (req, res) -> administradorController.getUsuarioDetails(req, res));
        post("/api/admin/usuarios/:id/role/assign", (req, res) -> administradorController.assignRole(req, res));
        post("/api/admin/usuarios/:id/role/revoke", (req, res) -> administradorController.revokeRole(req, res));
        post("/api/admin/usuarios/:id/disable", (req, res) -> administradorController.disableUser(req, res));
        post("/api/admin/usuarios/:id/enable", (req, res) -> administradorController.enableUser(req, res));
        post("/api/admin/usuarios/:id/password", (req, res) -> administradorController.changeUserPassword(req, res));

        // GET /en_construccion - Vista en construcción
        get("/en_construccion", (req, res) -> enConstruccionController.showEnConstruccion(req, res), templateEngine);

        // ========================================
        // RUTAS: GESTIÓN DE CARRERAS (B.1)
        // ========================================
        get("/admin/carreras",                                          (req, res) -> carreraController.listCarreras(req, res), templateEngine);
        get("/admin/carreras/nueva",                                    (req, res) -> carreraController.showCarreraForm(req, res), templateEngine);
        post("/admin/carreras/nueva",                                   (req, res) -> carreraController.createCarrera(req, res));
        get("/admin/carreras/:id",                                      (req, res) -> carreraController.showCarreraDetalle(req, res), templateEngine);
        get("/admin/carreras/:id/editar",                               (req, res) -> carreraController.showEditCarreraForm(req, res), templateEngine);
        post("/admin/carreras/:id/editar",                              (req, res) -> carreraController.updateCarrera(req, res));
        post("/admin/carreras/:id/baja",                                (req, res) -> carreraController.darDeBajaCarrera(req, res));
        get("/admin/carreras/:id/planes/nuevo",                         (req, res) -> carreraController.showPlanForm(req, res), templateEngine);
        post("/admin/carreras/:id/planes/nuevo",                        (req, res) -> carreraController.createPlan(req, res));
        get("/admin/carreras/:id/planes/:planId/editar",                (req, res) -> carreraController.showEditPlanForm(req, res), templateEngine);
        post("/admin/carreras/:id/planes/:planId/editar",               (req, res) -> carreraController.updatePlan(req, res));
        post("/admin/carreras/:id/planes/:planId/activar",              (req, res) -> carreraController.activarPlan(req, res));
        post("/admin/carreras/:id/planes/:planId/desactivar",           (req, res) -> carreraController.desactivarPlan(req, res));

        // ========================================
        // RUTAS: GESTIÓN DE MATERIAS (B.2)
        // ========================================
        get("/admin/materias",                                                   (req, res) -> materiaController.listMaterias(req, res), templateEngine);
        get("/admin/materias/nueva",                                             (req, res) -> materiaController.showMateriaForm(req, res), templateEngine);
        post("/admin/materias/nueva",                                            (req, res) -> materiaController.createMateria(req, res));
        get("/admin/materias/:id",                                               (req, res) -> materiaController.showMateriaDetalle(req, res), templateEngine);
        get("/admin/materias/:id/editar",                                        (req, res) -> materiaController.showEditMateriaForm(req, res), templateEngine);
        post("/admin/materias/:id/editar",                                       (req, res) -> materiaController.updateMateria(req, res));
        post("/admin/materias/:id/desactivar",                                   (req, res) -> materiaController.desactivarMateria(req, res));
        post("/admin/materias/:id/reactivar",                                    (req, res) -> materiaController.reactivarMateria(req, res));
        get("/admin/materias/:id/planes/nueva",                                  (req, res) -> materiaController.showAsociarPlanForm(req, res), templateEngine);
        post("/admin/materias/:id/planes/nueva",                                 (req, res) -> materiaController.asociarPlan(req, res));
        get("/admin/materias/:id/planes/:mpId/editar",                           (req, res) -> materiaController.showEditAsociacionForm(req, res), templateEngine);
        post("/admin/materias/:id/planes/:mpId/editar",                          (req, res) -> materiaController.updateAsociacion(req, res));
        post("/admin/materias/:id/planes/:mpId/desactivar",                      (req, res) -> materiaController.desactivarAsociacion(req, res));

        // ========================================
        // RUTAS: PERÍODOS LECTIVOS (B.1 ext.)
        // ========================================
        get("/admin/periodos",                            (req, res) -> periodoController.listPeriodos(req, res), templateEngine);
        get("/admin/periodos/nuevo",                      (req, res) -> periodoController.showPeriodoForm(req, res), templateEngine);
        post("/admin/periodos/nuevo",                     (req, res) -> periodoController.createPeriodo(req, res));
        post("/admin/periodos/:id/activar",               (req, res) -> periodoController.activarPeriodo(req, res));
        post("/admin/periodos/:id/desactivar",            (req, res) -> periodoController.desactivarPeriodo(req, res));

        // ========================================
        // RUTAS: COMISIONES (C.1) + ASIGNACIONES (D.1)
        // ========================================
        get("/admin/comisiones",                                                      (req, res) -> comisionController.listComisiones(req, res), templateEngine);
        get("/admin/comisiones/nueva",                                                (req, res) -> comisionController.showComisionForm(req, res), templateEngine);
        post("/admin/comisiones/nueva",                                               (req, res) -> comisionController.createComision(req, res));
        get("/admin/comisiones/:id",                                                  (req, res) -> comisionController.showComisionDetalle(req, res), templateEngine);
        post("/admin/comisiones/:id/cerrar",                                          (req, res) -> comisionController.cerrarComision(req, res));
        get("/admin/comisiones/:id/asignaciones/nueva",                               (req, res) -> comisionController.showAsignacionForm(req, res), templateEngine);
        post("/admin/comisiones/:id/asignaciones/nueva",                              (req, res) -> comisionController.createAsignacion(req, res));
        get("/admin/comisiones/:id/asignaciones/:asId/cargo",                         (req, res) -> comisionController.showCambiarCargoForm(req, res), templateEngine);
        post("/admin/comisiones/:id/asignaciones/:asId/cargo",                        (req, res) -> comisionController.cambiarCargo(req, res));
        post("/admin/comisiones/:id/asignaciones/:asId/baja",                         (req, res) -> comisionController.bajaAsignacion(req, res));

        // ========================================
        // RUTAS: DOCENTE — MIS COMISIONES (D.1 Flujo 5.4)
        // ========================================
        get("/docente/comisiones",  (req, res) -> docenteController.showMisComisiones(req, res), templateEngine);

        // ========================================
        // APLICAR MIDDLEWARE DE PROTECCIÓN
        // ========================================

        // Dashboard requiere autenticación (cualquier rol)
        before("/dashboard", AuthMiddleware.requireAuth);

        // Docente requiere rol DOCENTE
        before("/docente", AuthMiddleware.requireRole("DOCENTE"));

        // Estudiante requiere rol ESTUDIANTE
        before("/estudiante", AuthMiddleware.requireRole("ESTUDIANTE"));
        before("/estudiante/*", AuthMiddleware.requireRole("ESTUDIANTE"));

        // Administrador requiere rol ADMINISTRADOR (si se mantiene el rol)
        before("/admin", AuthMiddleware.requireRole("ADMINISTRADOR"));
        before("/api/admin/*", AuthMiddleware.requireRole("ADMINISTRADOR"));
        before("/admin/carreras", AuthMiddleware.requireRole("ADMINISTRADOR"));
        before("/admin/carreras/*", AuthMiddleware.requireRole("ADMINISTRADOR"));
        before("/admin/materias", AuthMiddleware.requireRole("ADMINISTRADOR"));
        before("/admin/materias/*", AuthMiddleware.requireRole("ADMINISTRADOR"));
        before("/admin/periodos", AuthMiddleware.requireRole("ADMINISTRADOR"));
        before("/admin/periodos/*", AuthMiddleware.requireRole("ADMINISTRADOR"));
        before("/admin/comisiones", AuthMiddleware.requireRole("ADMINISTRADOR"));
        before("/admin/comisiones/*", AuthMiddleware.requireRole("ADMINISTRADOR"));
        before("/docente/comisiones", AuthMiddleware.requireRole("DOCENTE"));

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