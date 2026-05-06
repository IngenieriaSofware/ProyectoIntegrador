-- ========================================
-- TABLAS DE IDENTIDAD Y AUTENTICACIÓN
-- ========================================

-- Tabla base: PERSONAS (Núcleo de identidad unificada)
CREATE TABLE IF NOT EXISTS personas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    dni TEXT NOT NULL UNIQUE,                  -- DNI único (identificador)
    nombre TEXT NOT NULL,
    apellido TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,                -- Email único (también identificador para login)
    password TEXT NOT NULL,                    -- Contraseña hasheada con Bcrypt
    telefono TEXT,
    localidad TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de relación N:N: PERSONA_ROLES (Soporta roles duales)
CREATE TABLE IF NOT EXISTS persona_roles (
    persona_id INTEGER NOT NULL,
    rol TEXT NOT NULL CHECK(rol IN ('DOCENTE', 'ESTUDIANTE', 'ADMINISTRADOR')),
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (persona_id, rol),
    FOREIGN KEY (persona_id) REFERENCES personas(id) ON DELETE CASCADE
);

-- ========================================
-- ENUMERACIONES: TCARGO Y TESTADO
-- ========================================

-- Tabla: TCARGO (Tipos de cargo docente)
CREATE TABLE IF NOT EXISTS tcargo (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL UNIQUE,               -- Ej: "Profesor Titular", "Asistente", "JTP"
    descripcion TEXT
);

-- Tabla: TESTADO (Estados de estudiante)
CREATE TABLE IF NOT EXISTS testado (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL UNIQUE,               -- Ej: "Activo", "Inactivo", "Suspendido"
    descripcion TEXT
);

-- Insert valores por defecto para TCARGO
INSERT OR IGNORE INTO tcargo (nombre, descripcion) VALUES 
    ('Profesor Titular', 'Responsable de la cátedra'),
    ('Profesor Asociado', 'Profesor asociado'),
    ('Asistente', 'Docente asistente'),
    ('JTP', 'Jefe de Trabajos Prácticos');

-- Insert valores por defecto para TESTADO
INSERT OR IGNORE INTO testado (nombre, descripcion) VALUES 
    ('Activo', 'Estudiante activo'),
    ('Inactivo', 'Estudiante inactivo'),
    ('Suspendido', 'Estudiante suspendido');

-- ========================================
-- TABLAS ACADÉMICAS: ESTUDIANTES Y DOCENTES
-- ========================================

-- Tabla: ESTUDIANTES (Perfil académico del estudiante)
CREATE TABLE IF NOT EXISTS estudiantes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    persona_id INTEGER NOT NULL UNIQUE,        -- FK a personas (1:1)
    carrera TEXT NOT NULL,                     -- Ej: "Ingeniería en Sistemas"
    plan_estudio TEXT NOT NULL,                -- Ej: "Plan 2020", "Plan 2023"
    estado_id INTEGER NOT NULL,                -- FK a testado
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (persona_id) REFERENCES personas(id) ON DELETE CASCADE,
    FOREIGN KEY (estado_id) REFERENCES testado(id)
);

-- Tabla: DOCENTES (Perfil académico del docente)
CREATE TABLE IF NOT EXISTS docentes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    persona_id INTEGER NOT NULL UNIQUE,        -- FK a personas (1:1)
    cargo_id INTEGER NOT NULL,                 -- FK a tcargo
    responsable TEXT,                          -- Ej: nombre/email del responsable a cargo
    periodo TEXT,                              -- Ej: "2025-Q1", "2025-Q2"
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (persona_id) REFERENCES personas(id) ON DELETE CASCADE,
    FOREIGN KEY (cargo_id) REFERENCES tcargo(id)
);

-- Tabla: ADMINISTRADORES (Perfil académico del administrador)
CREATE TABLE IF NOT EXISTS administradores (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    persona_id INTEGER NOT NULL UNIQUE,        -- FK a personas (1:1)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (persona_id) REFERENCES personas(id) ON DELETE CASCADE

);

-- ========================================
-- TABLAS DE PERMISOS (Gestión de accesos dinámicos)
-- ========================================

-- Tabla: PERMISOS (Catálogo de permisos)
CREATE TABLE IF NOT EXISTS permisos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL UNIQUE,               -- Ej: "gestionar_estudiantes", "ver_calificaciones"
    descripcion TEXT,
    categoria TEXT                             -- Ej: "admin", "docente", "estudiante"
);

-- Tabla: PERSONA_PERMISOS (Asignación N:N de permisos a personas)
CREATE TABLE IF NOT EXISTS persona_permisos (
    persona_id INTEGER NOT NULL,
    permiso_id INTEGER NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (persona_id, permiso_id),
    FOREIGN KEY (persona_id) REFERENCES personas(id) ON DELETE CASCADE,
    FOREIGN KEY (permiso_id) REFERENCES permisos(id) ON DELETE CASCADE
);

-- Insert permisos iniciales (Oficina de Alumnos / Gestión)
INSERT OR IGNORE INTO permisos (nombre, descripcion, categoria) VALUES 
    ('gestionar_estudiantes', 'Crear, editar, eliminar estudiantes', 'admin'),
    ('gestionar_docentes', 'Crear, editar, eliminar docentes', 'admin'),
    ('ver_reportes', 'Visualizar reportes académicos', 'admin'),
    ('gestionar_calificaciones', 'Registrar y editar calificaciones', 'docente'),
    ('ver_estudiantes_catedra', 'Ver estudiantes inscritos en la cátedra', 'docente'),
    ('crear_evaluaciones', 'Crear evaluaciones', 'docente'),
    ('gestionar_administradores', 'Crear, editar, eliminar administradores', 'admin');

-- ========================================
-- TABLA DE AUDITORÍA
-- ========================================

-- Tabla: AUDIT_LOGS (Registro de acciones de administrador)
CREATE TABLE IF NOT EXISTS audit_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    admin_id INTEGER NOT NULL,
    accion TEXT NOT NULL,                      -- Ej: "ASSIGN_ROLE", "DISABLE_USER", "VIEW_USER"
    target_id INTEGER,                         -- ID de la persona afectada (nullable)
    detalles TEXT,                             -- Detalles adicionales
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (admin_id) REFERENCES personas(id) ON DELETE CASCADE
);

-- Agregar campos de auditoría a personas (para ban)
ALTER TABLE personas ADD COLUMN enabled INTEGER DEFAULT 1;
ALTER TABLE personas ADD COLUMN ban_reason TEXT;

