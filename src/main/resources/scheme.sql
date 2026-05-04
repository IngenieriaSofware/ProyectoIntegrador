-- Crea la tabla 'users' con los campos originales, adaptados para SQLite
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT, -- Clave primaria autoincremental para SQLite
    name TEXT NOT NULL UNIQUE,          -- Nombre de usuario (TEXT es el tipo de cadena recomendado para SQLite), con restricción UNIQUE
    password TEXT NOT NULL,           -- Contraseña hasheada (TEXT es el tipo de cadena recomendado para SQLite)
    role TEXT NOT NULL DEFAULT 'ESTUDIANTE' CHECK(role IN ('ADMIN', 'DOCENTE', 'ESTUDIANTE')) -- Rol del usuario
);

CREATE TABLE IF NOT EXISTS professors (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    dni int unique not null,
    name TEXT NOT NULL,
    email TEXT UNIQUE,
    department TEXT,
    phone TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

