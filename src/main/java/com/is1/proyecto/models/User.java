package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;
import java.util.Optional;

@Table("users") // Esta anotación asocia explícitamente el modelo 'User' con la tabla 'users' en la DB.
public class User extends Model {

    public String getName() {
        return getString("name");
    }

    public void setName(String name) {
        set("name", name);
    }

    public String getPassword() {
        return getString("password");
    }

    public void setPassword(String password) {
        set("password", password);
    }

    public String getRole() {
        return getString("role");
    }

    public void setRole(String role) {
        set("role", role);
    }

    public Role getRoleEnum() {
        try {
            return Role.valueOf(getRole());
        } catch (IllegalArgumentException e) {
            return Role.ESTUDIANTE;
        }
    }

    public void setRoleEnum(Role role) {
        setRole(role.name());
    }

    public static Optional<User> findByUsername(String username) {
        User user = User.findFirst("name = ?", username);
        return Optional.ofNullable(user);
    }

}