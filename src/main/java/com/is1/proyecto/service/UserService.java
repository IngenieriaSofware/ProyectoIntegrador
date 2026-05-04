package com.is1.proyecto.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.mindrot.jbcrypt.BCrypt;

import com.is1.proyecto.models.Role;
import com.is1.proyecto.models.User;

/**
 * Capa de servicio para la lógica relacionada con usuarios.
 * Maneja la creación, autenticación y validación de usuarios.
 */
public class UserService {

    /**
     * Registra un nuevo usuario en la base de datos.
     * Valida que los parámetros no sean nulos ni vacíos.
     * Hashea la contraseña antes de guardarla.
     *
     * @param name     Nombre de usuario
     * @param password Contraseña en texto plano
     * @param role     Rol del usuario (ADMIN, DOCENTE, ESTUDIANTE)
     * @return Un mapa con los resultados: {success: boolean, message: String, userId: Long}
     */
    public Map<String, Object> registerUser(String name, String password, String role) {
        Map<String, Object> result = new HashMap<>();

        // Validaciones
        if (name == null || name.isEmpty()) {
            result.put("success", false);
            result.put("message", "El nombre de usuario es requerido.");
            return result;
        }
        // Sanitizar nombre: solo letras, números, guiones bajos, 3-20 caracteres
        Pattern namePattern = Pattern.compile("^[A-Za-z0-9_]{3,20}$");
        if (!namePattern.matcher(name).matches()) {
            result.put("success", false);
            result.put("message", "Nombre de usuario inválido. Use 3-20 caracteres alfanuméricos y guiones bajos.");
            return result;
        }

        if (password == null || password.isEmpty()) {
            result.put("success", false);
            result.put("message", "La contraseña es requerida.");
            return result;
        }
        // Validar contraseña: 8-30 caracteres alfanuméricos
        Pattern passPattern = Pattern.compile("^[A-Za-z0-9]{8,30}$");
        if (!passPattern.matcher(password).matches()) {
            result.put("success", false);
            result.put("message", "Contraseña inválida. Use 8-30 caracteres alfanuméricos.");
            return result;
        }

        if (role == null || role.isEmpty()) {
            result.put("success", false);
            result.put("message", "El rol es requerido.");
            return result;
        }

        try {
            // Validar rol
            Role validRole;
            try {
                validRole = Role.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                result.put("success", false);
                result.put("message", "Rol inválido. Debe ser ADMIN, DOCENTE o ESTUDIANTE.");
                return result;
            }

            // Verificar si el usuario ya existe
            System.out.println("DEBUG UserService: contando usuarios antes: " + User.count());
            User existingUser = User.findFirst("name = ?", name);
            if (existingUser != null) {
                result.put("success", false);
                result.put("message", "El nombre de usuario ya está registrado.");
                return result;
            }

            // Crear nuevo usuario
            User newUser = new User();
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            newUser.set("name", name);
            newUser.set("password", hashedPassword);
            newUser.set("role", validRole.name());
            System.out.println("DEBUG UserService: intentando guardar usuario: " + name);
            System.out.println("DEBUG UserService: Base.hasConnection() = " + org.javalite.activejdbc.Base.hasConnection());
            try {
                newUser.saveIt();
                System.out.println("DEBUG UserService: saveIt() exitoso");
            } catch (Exception e) {
                System.out.println("DEBUG UserService: saveIt() error: " + e.getMessage());
                e.printStackTrace();
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("UNIQUE constraint failed")) {
                    result.put("success", false);
                    result.put("message", "El nombre de usuario ya está registrado.");
                } else {
                    result.put("success", false);
                    result.put("message", "Error al guardar usuario: " + e.getMessage());
                }
                return result;
            }
            System.out.println("DEBUG UserService: ID generado: " + newUser.getId());
            // Verificar que se guardó
            User verify = User.findFirst("name = ?", name);
            System.out.println("DEBUG UserService: verificación post-save: " + (verify != null ? "ENCONTRADO (id=" + verify.getId() + ")" : "NO ENCONTRADO"));
            System.out.println("DEBUG UserService: contando usuarios después: " + User.count());

            result.put("success", true);
            result.put("message", "Cuenta creada exitosamente para " + name + "!");
            result.put("userId", newUser.getId());
            result.put("role", validRole.name());
            return result;

        } catch (Exception e) {
            System.err.println("Error al registrar usuario: " + e.getMessage());
            result.put("success", false);
            result.put("message", "Error interno al crear la cuenta. Intente de nuevo.");
            return result;
        }
    }

    /**
     * Autentica un usuario verificando nombre de usuario y contraseña.
     *
     * @param username Nombre de usuario
     * @param password Contraseña en texto plano
     * @param authService AuthService para validar intentos
     * @return Un mapa con los resultados: {success: boolean, message: String, user: User, token: String}
     */
    public Map<String, Object> authenticateUser(String username, String password, AuthService authService) {
        Map<String, Object> result = new HashMap<>();

        // Validaciones
        if (username == null || username.isEmpty()) {
            result.put("success", false);
            result.put("message", "El nombre de usuario es requerido.");
            return result;
        }
        // Sanitizar username
        Pattern userPattern = Pattern.compile("^[A-Za-z0-9_]{3,20}$");
        if (!userPattern.matcher(username).matches()) {
            result.put("success", false);
            result.put("message", "Usuario inválido. Use 3-20 caracteres alfanuméricos o guiones bajos.");
            return result;
        }

        if (password == null || password.isEmpty()) {
            result.put("success", false);
            result.put("message", "La contraseña es requerida.");
            return result;
        }

        try {
            // Verificar si el login está permitido
            if (!authService.isLoginAllowed(username)) {
                String lockoutTime = authService.getLockoutTimeLeft(username);
                result.put("success", false);
                result.put("message", "Cuenta temporalmente bloqueada. Intente nuevamente en " + lockoutTime);
                return result;
            }

            // Buscar usuario en la base de datos
            User user = User.findFirst("name = ?", username);

            if (user == null) {
                authService.recordFailedAttempt(username);
                result.put("success", false);
                result.put("message", "Usuario o contraseña incorrectos.");
                return result;
            }

            // Verificar contraseña con BCrypt
            String storedHashedPassword = user.getString("password");
            if (BCrypt.checkpw(password, storedHashedPassword)) {
                // Autenticación exitosa
                authService.resetFailedAttempts(username);

                // Generar token JWT
                String token = authService.generateToken(username, user.getRole());

                result.put("success", true);
                result.put("message", "Autenticación exitosa.");
                result.put("user", user);
                result.put("token", token);
                return result;
            } else {
                authService.recordFailedAttempt(username);
                result.put("success", false);
                result.put("message", "Usuario o contraseña incorrectos.");
                return result;
            }

        } catch (Exception e) {
            System.err.println("Error al autenticar usuario: " + e.getMessage());
            result.put("success", false);
            result.put("message", "Error interno al autenticar.");
            return result;
        }
    }

    /**
     * Obtiene un usuario por su nombre de usuario.
     *
     * @param username Nombre de usuario
     * @return El usuario encontrado o null si no existe
     */
    public User getUserByUsername(String username) {
        return User.findFirst("name = ?", username);
    }

    /**
     * Obtiene un usuario por su nombre de usuario (Optional).
     *
     * @param username Nombre de usuario
     * @return Optional del usuario encontrado
     */
    public Optional<User> findUserByUsername(String username) {
        return User.findByUsername(username);
    }
}
