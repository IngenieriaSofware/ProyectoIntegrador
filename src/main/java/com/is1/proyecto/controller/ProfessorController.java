package com.is1.proyecto.controller;

import com.is1.proyecto.service.ProfessorService;
import spark.Request;
import spark.Response;
import spark.ModelAndView;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador para manejar las rutas relacionadas con profesores.
 * Delega la lógica de negocio al ProfessorService.
 */
public class ProfessorController {

    private ProfessorService professorService;

    public ProfessorController() {
        this.professorService = new ProfessorService();
    }

    /**
     * GET /professor/new - Muestra el formulario de creación de profesor
     */
    public ModelAndView showCreateForm(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();

        String successMessage = req.queryParams("message");
        if (successMessage != null && !successMessage.isEmpty()) {
            model.put("successMessage", successMessage);
        }

        String errorMessage = req.queryParams("error");
        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.put("errorMessage", errorMessage);
        }

        // Verificar si el usuario está logueado
        Boolean loggedIn = req.session().attribute("loggedIn");
        if (loggedIn == null || !loggedIn) {
            res.redirect("/login?error=Debes iniciar sesión para agregar profesores.");
            return null;
        }

        return new ModelAndView(model, "professor_form.mustache");
    }

    /**
     * POST /professor/new - Procesa la creación de un nuevo profesor
     */
    public Object createProfessor(Request req, Response res) {
        String name = req.queryParams("name");
        String email = req.queryParams("email");
        String dni = req.queryParams("DNI");
        String department = req.queryParams("department");
        String phone = req.queryParams("phone");

        // Usar el servicio para crear el profesor
        Map<String, Object> result = professorService.createProfessor(name, email, dni, department, phone);

        if ((Boolean) result.get("success")) {
            String message = (String) result.get("message");
            String encodedMsg = URLEncoder.encode(message, StandardCharsets.UTF_8);
            res.redirect("/professor/new?message=" + encodedMsg);
        } else {
            String message = (String) result.get("message");
            String encodedMsg = URLEncoder.encode(message, StandardCharsets.UTF_8);
            res.redirect("/professor/new?error=" + encodedMsg);
        }

        return null;
    }

    /**
     * GET /professor/list - Muestra el listado paginado de profesores
     */
    public ModelAndView listProfessors(Request req, Response res) {
        // Definir tamaño de página
        final int PAGE_SIZE = 5;

        // Obtener el número de página desde la URL
        int currentPage = 1;
        try {
            if (req.queryParams("page") != null) {
                currentPage = Integer.parseInt(req.queryParams("page"));
            }
        } catch (NumberFormatException e) {
            currentPage = 1;
        }

        // Usar el servicio para obtener la lista paginada
        Map<String, Object> pagedResult = professorService.getProfessorsPagedList(currentPage, PAGE_SIZE);

        // Preparar el modelo para Mustache
        Map<String, Object> model = new HashMap<>();
        model.put("professors", pagedResult.get("professors"));

        // Construir la lista de páginas para Mustache
        int totalPages = (Integer) pagedResult.get("totalPages");
        List<Map<String, Object>> pages = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            Map<String, Object> page = new HashMap<>();
            page.put("number", i);
            if (i == currentPage) {
                page.put("isCurrent", true);
            }
            pages.add(page);
        }

        model.put("pages", pages);

        // Lógica para "Anterior" y "Siguiente"
        if (currentPage > 1) {
            model.put("hasPrevious", true);
            model.put("previousPage", currentPage - 1);
        }
        if (currentPage < totalPages) {
            model.put("hasNext", true);
            model.put("nextPage", currentPage + 1);
        }

        return new ModelAndView(model, "professor_list.mustache");
    }
}
