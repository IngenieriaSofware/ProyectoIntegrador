package com.is1.proyecto.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.javalite.activejdbc.Base;

import com.is1.proyecto.models.Carrera;
import com.is1.proyecto.models.Docente;
import com.is1.proyecto.models.Estudiante;
import com.is1.proyecto.models.Persona;
import com.is1.proyecto.models.PersonaRole;

/**
 * Servicio de usuarios - Gestiona registro y autenticación de Personas.
 * Usa Persona como modelo principal con relación N:N para roles.
 * Las contraseñas se hashean con Bcrypt.
 */
public class UserService {

    private AuthService authService;

    public UserService() {
        this.authService = new AuthService();
    }

    /**
     * Registra una nueva Persona en el sistema.
     * Crea entrada en tabla 'personas' y asigna rol inicial en 'persona_roles'.
     * 
     * @param dni DNI único
     * @param nombre Nombre
     * @param apellido Apellido
     * @param email Email único
     * @param password Contraseña en texto plano
     * @param telefono Teléfono (opcional)
     * @param localidad Localidad (opcional)
     * @param rolInicial Rol inicial (DOCENTE o ESTUDIANTE)
     * @return Mapa con: {success: boolean, message: String, personaId: Long}
     */
    public Map<String, Object> registerPersona(String dni, String nombre, String apellido, 
                                               String email, String password, String telefono, 
                                               String localidad, String rolInicial) {
        return registerPersona(dni, nombre, apellido, email, password, telefono, localidad, rolInicial, 0);
    }

    public Map<String, Object> registerPersona(String dni, String nombre, String apellido, 
                                               String email, String password, String telefono, 
                                               String localidad, String rolInicial, int carreraId) {
        Map<String, Object> result = new HashMap<>();

        // === VALIDACIONES ===
        if (dni == null || dni.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "DNI es requerido.");
            return result;
        }

        if (nombre == null || nombre.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Nombre es requerido.");
            return result;
        }

        if (apellido == null || apellido.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Apellido es requerido.");
            return result;
        }

        if (email == null || email.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Email es requerido.");
            return result;
        }
        // Validar formato email
        Pattern emailPattern = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
        if (!emailPattern.matcher(email).matches()) {
            result.put("success", false);
            result.put("message", "Email inválido.");
            return result;
        }

        if (password == null || password.isEmpty()) {
            result.put("success", false);
            result.put("message", "Contraseña es requerida.");
            return result;
        }
        // Validar contraseña: 8-30 caracteres
        if (password.length() < 8 || password.length() > 30) {
            result.put("success", false);
            result.put("message", "Contraseña debe tener 8-30 caracteres.");
            return result;
        }

        if (rolInicial == null || rolInicial.isEmpty()) {
            result.put("success", false);
            result.put("message", "Rol inicial es requerido.");
            return result;
        }
        if (!PermissionService.isValidRole(rolInicial)) {
            result.put("success", false);
            result.put("message", "Rol inválido. Debe ser DOCENTE, ESTUDIANTE o ADMINISTRADOR.");
            return result;
        }

        try {
            // Verificar si DNI ya existe
            Optional<Persona> existingByDni = Persona.findByDni(dni);
            if (existingByDni.isPresent()) {
                result.put("success", false);
                result.put("message", "DNI ya está registrado.");
                return result;
            }

            // Verificar si Email ya existe
            Optional<Persona> existingByEmail = Persona.findByEmail(email);
            if (existingByEmail.isPresent()) {
                result.put("success", false);
                result.put("message", "Email ya está registrado.");
                return result;
            }

            // Crear nueva Persona
            Persona persona = new Persona();
            persona.set("dni", dni);
            persona.set("nombre", nombre);
            persona.set("apellido", apellido);
            persona.set("email", email);
            persona.set("password", authService.hashPassword(password));  // Bcrypt
            persona.set("telefono", telefono != null ? telefono : "");
            persona.set("localidad", localidad != null ? localidad : "");

            persona.saveIt();

            // Crear perfil académico según el rol
            if ("ESTUDIANTE".equals(rolInicial)) {
                Estudiante student = new Estudiante();
                student.set("persona_id", persona.getId());
                
                // Si carreraId fue proporcionado, obtener datos de carrera
                if (carreraId > 0) {
                    Carrera carrera = Carrera.findById(carreraId);
                    if (carrera != null) {
                        // Guardar nombre de carrera
                        student.set("carrera", carrera.getString("nombre"));
                        
                        // Obtener plan vigente de esta carrera
                        List<Map> planResult = Base.findAll(
                            "SELECT * FROM planes_estudio WHERE carrera_id = ? AND activo = 1 ORDER BY anio_vigencia DESC LIMIT 1",
                            carreraId
                        );
                        
                        if (!planResult.isEmpty()) {
                            Map planMap = planResult.get(0);
                            int anioVigencia = ((Number) planMap.get("anio_vigencia")).intValue();
                            student.set("plan_estudio", "Plan " + anioVigencia);
                        } else {
                            student.set("plan_estudio", "Sin asignar");
                        }
                    } else {
                        student.set("carrera", "Sin asignar");
                        student.set("plan_estudio", "Sin asignar");
                    }
                } else {
                    student.set("carrera", "Sin asignar");
                    student.set("plan_estudio", "Sin asignar");
                }
                
                student.set("estado_id", 1); // 1 = Activo en testado
                student.saveIt();

            } else if ("DOCENTE".equals(rolInicial)) {
                Docente professor = new Docente();
                professor.set("persona_id", persona.getId());
                professor.set("cargo_id", 1); // 1 = Profesor Titular en tcargo
                professor.saveIt();
                
            } else if ("ADMINISTRADOR".equals(rolInicial)) {
                // Crear perfil de administrador
                com.is1.proyecto.models.Administrador admin = new com.is1.proyecto.models.Administrador();
                admin.set("persona_id", persona.getId());
                admin.saveIt();
            }

            // Asignar rol inicial
            PersonaRole personaRole = new PersonaRole();
            personaRole.set("persona_id", persona.getId());
            personaRole.set("rol", rolInicial);
            personaRole.saveIt();

            result.put("success", true);
            result.put("message", "Persona registrada exitosamente.");
            result.put("personaId", persona.getId());
            result.put("email", email);
            result.put("rol", rolInicial);
            return result;

        } catch (Exception e) {
            System.err.println("Error al registrar Persona: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Error interno al crear Persona. Intente de nuevo.");
            return result;
        }
    }

    /**
     * Autentica una Persona por DNI o Email y contraseña.
     * Retorna JWT token si autenticación es exitosa.
     * 
     * @param identifier DNI o Email
     * @param password Contraseña en texto plano
     * @return Mapa con: {success: boolean, message: String, persona: Persona, token: String, roles: List<String>}
     */
    public Map<String, Object> authenticatePersona(String identifier, String password) {
        Map<String, Object> result = new HashMap<>();

        // VALIDACIONES
        if (identifier == null || identifier.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Credenciales inválidas");
            return result;
        }

        if (password == null || password.isEmpty()) {
            result.put("success", false);
            result.put("message", "Credenciales inválidas");
            return result;
        }

        try {
            // Verificar bloqueo por intentos fallidos
            if (!authService.isLoginAllowed(identifier)) {
                String lockoutTime = authService.getLockoutTimeLeft(identifier);
                result.put("success", false);
                result.put("message", "Cuenta temporalmente bloqueada. Intente en " + lockoutTime);
                return result;
            }

            // Buscar Persona por DNI o Email
            Optional<Persona> personaOpt = Persona.findByDniOrEmail(identifier);

            if (!personaOpt.isPresent()) {
                authService.recordFailedAttempt(identifier);
                result.put("success", false);
                result.put("message", "Credenciales inválidas");
                return result;
            }

            Persona persona = personaOpt.get();

            // Verificar si el usuario está habilitado (no baneado)
            Integer enabled = (Integer) persona.get("enabled");
            if (enabled != null && enabled == 0) {
                String banReason = persona.getString("ban_reason");
                authService.recordFailedAttempt(identifier);
                result.put("success", false);
                result.put("message", "Cuenta deshabilitada. Razón: " + (banReason != null ? banReason : "Sin especificar"));
                return result;
            }

            // Verificar contraseña con Bcrypt
            if (!authService.verifyPassword(password, persona.getPassword())) {
                authService.recordFailedAttempt(identifier);
                result.put("success", false);
                result.put("message", "Credenciales inválidas");
                return result;
            }

            // Autenticación exitosa
            authService.resetFailedAttempts(identifier);

            // Generar JWT token
            String token = authService.generateToken(persona);
            List<String> roles = persona.getRoleNames();

            result.put("success", true);
            result.put("message", "Autenticación exitosa");
            result.put("persona", persona);
            result.put("token", token);
            result.put("roles", roles);
            result.put("personaId", persona.getId());
            return result;

        } catch (Exception e) {
            System.err.println("Error al autenticar Persona: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Error interno de autenticación");
            return result;
        }
    }

    /**
     * Asigna un rol adicional a una Persona.
     * Permite que una Persona sea DOCENTE Y ESTUDIANTE simultáneamente.
     * 
     * @param personaId ID de la Persona
     * @param rol Rol a asignar (DOCENTE o ESTUDIANTE)
     * @return Mapa con: {success: boolean, message: String}
     */
    public Map<String, Object> assignRoleToPersona(int personaId, String rol) {
        Map<String, Object> result = new HashMap<>();

        if (!PermissionService.isValidRole(rol)) {
            result.put("success", false);
            result.put("message", "Rol inválido.");
            return result;
        }

        try {
            // Verificar que Persona existe
            Persona persona = Persona.findById(personaId);
            if (persona == null) {
                result.put("success", false);
                result.put("message", "Persona no encontrada.");
                return result;
            }

            // Verificar que el rol no esté ya asignado
            List<String> rolesActuales = persona.getRoleNames();
            if (rolesActuales.contains(rol)) {
                result.put("success", false);
                result.put("message", "Persona ya tiene este rol.");
                return result;
            }

            // Asignar nuevo rol
            PersonaRole personaRole = new PersonaRole();
            personaRole.set("persona_id", personaId);
            personaRole.set("rol", rol);
            personaRole.saveIt();

            result.put("success", true);
            result.put("message", "Rol asignado exitosamente.");
            return result;

        } catch (Exception e) {
            System.err.println("Error al asignar rol: " + e.getMessage());
            result.put("success", false);
            result.put("message", "Error al asignar rol.");
            return result;
        }
    }

    public Map<String, Object> registerPersonaConRoles(String dni, String nombre, String apellido,
                                                    String email, String password, String telefono,
                                                    String localidad, List<String> roles) {
        return registerPersonaConRoles(dni, nombre, apellido, email, password, telefono, localidad, roles, null);
    }

    public Map<String, Object> registerPersonaConRoles(String dni, String nombre, String apellido,
                                                    String email, String password, String telefono,
                                                    String localidad, List<String> roles, String departamento) {
        Map<String, Object> result = new HashMap<>();

        // Validaciones básicas (igual que registerPersona)
        if (dni == null || dni.trim().isEmpty()) {
            result.put("success", false); result.put("message", "DNI es requerido."); return result;
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            result.put("success", false); result.put("message", "Nombre es requerido."); return result;
        }
        if (apellido == null || apellido.trim().isEmpty()) {
            result.put("success", false); result.put("message", "Apellido es requerido."); return result;
        }
        if (email == null || email.trim().isEmpty()) {
            result.put("success", false); result.put("message", "Email es requerido."); return result;
        }
        if (password == null || password.length() < 8 || password.length() > 30) {
            result.put("success", false); result.put("message", "Contraseña inválida."); return result;
        }
        if (roles == null || roles.isEmpty()) {
            result.put("success", false); result.put("message", "Debe asignar al menos un rol."); return result;
        }
        for (String rol : roles) {
            if (!"DOCENTE".equals(rol) && !"ESTUDIANTE".equals(rol)) {
                result.put("success", false);
                result.put("message", "Rol inválido: " + rol + ". Solo se permite DOCENTE o ESTUDIANTE.");
                return result;
            }
        }

        try {
            if (Persona.findByDni(dni).isPresent()) {
                result.put("success", false); result.put("message", "DNI ya registrado."); return result;
            }
            if (Persona.findByEmail(email).isPresent()) {
                result.put("success", false); result.put("message", "Email ya registrado."); return result;
            }

            Persona persona = new Persona();
            persona.set("dni", dni);
            persona.set("nombre", nombre);
            persona.set("apellido", apellido);
            persona.set("email", email);
            persona.set("password", authService.hashPassword(password));
            persona.set("telefono", telefono != null ? telefono : "");
            persona.set("localidad", localidad != null ? localidad : "");
            persona.saveIt();

            for (String rol : roles) {
                if ("ESTUDIANTE".equals(rol)) {
                    Estudiante student = new Estudiante();
                    student.set("persona_id", persona.getId());
                    student.set("carrera", "Sin asignar");
                    student.set("plan_estudio", "Sin asignar");
                    student.set("estado_id", 1);
                    student.saveIt();
                } else if ("DOCENTE".equals(rol)) {
                    Docente professor = new Docente();
                    professor.set("persona_id", persona.getId());
                    professor.set("cargo_id", 1);
                    if (departamento != null && !departamento.trim().isEmpty()) {
                        professor.set("departamento", departamento.trim());
                    }
                    professor.saveIt();
                }

                PersonaRole personaRole = new PersonaRole();
                personaRole.set("persona_id", persona.getId());
                personaRole.set("rol", rol);
                personaRole.saveIt();
            }

            result.put("success", true);
            result.put("message", "Persona registrada exitosamente.");
            result.put("personaId", persona.getId());
            return result;

        } catch (Exception e) {
            System.err.println("Error al registrar Persona: " + e.getMessage());
            result.put("success", false);
            result.put("message", "Error interno. Intente de nuevo.");
            return result;
        }
    }
}
