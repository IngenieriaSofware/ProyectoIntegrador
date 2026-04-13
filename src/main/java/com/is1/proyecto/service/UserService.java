package com.is1.proyecto.service;

import org.mindrot.jbcrypt.BCrypt;
import com.is1.proyecto.models.User;
import java.util.HashMap;
import java.util.Map;

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
     * @return Un mapa con los resultados: {success: boolean, message: String, userId: Long}
     */
    public Map<String, Object> registerUser(String name, String password) {
        Map<String, Object> result = new HashMap<>();

        // Validaciones
        if (name == null || name.isEmpty()) {
            result.put("success", false);
            result.put("message", "El nombre de usuario es requerido.");
            return result;
        }

        if (password == null || password.isEmpty()) {
            result.put("success", false);
            result.put("message", "La contraseña es requerida.");
            return result;
        }

        try {
            // Verificar si el usuario ya existe
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
            newUser.saveIt();

            result.put("success", true);
            result.put("message", "Cuenta creada exitosamente para " + name + "!");
            result.put("userId", newUser.getId());
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
     * @return Un mapa con los resultados: {success: boolean, message: String, user: User}
     */
    public Map<String, Object> authenticateUser(String username, String password) {
        Map<String, Object> result = new HashMap<>();

        // Validaciones
        if (username == null || username.isEmpty()) {
            result.put("success", false);
            result.put("message", "El nombre de usuario es requerido.");
            return result;
        }

        if (password == null || password.isEmpty()) {
            result.put("success", false);
            result.put("message", "La contraseña es requerida.");
            return result;
        }

        try {
            // Buscar usuario en la base de datos
            User user = User.findFirst("name = ?", username);

            if (user == null) {
                result.put("success", false);
                result.put("message", "Usuario o contraseña incorrectos.");
                return result;
            }

            // Verificar contraseña con BCrypt
            String storedHashedPassword = user.getString("password");
            if (BCrypt.checkpw(password, storedHashedPassword)) {
                result.put("success", true);
                result.put("message", "Autenticación exitosa.");
                result.put("user", user);
                return result;
            } else {
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
}
