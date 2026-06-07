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

-- ========================================
-- MÓDULO B.1 - OFERTA ACADÉMICA
-- ========================================

CREATE TABLE IF NOT EXISTS carreras (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo TEXT NOT NULL UNIQUE,
    nombre TEXT NOT NULL,
    descripcion TEXT,
    activa INTEGER NOT NULL DEFAULT 1,
    creada_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actualizada_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS planes_estudio (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    carrera_id INTEGER NOT NULL,
    anio_vigencia INTEGER NOT NULL,
    descripcion TEXT,
    duracion_anios INTEGER,
    activo INTEGER NOT NULL DEFAULT 0,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (carrera_id) REFERENCES carreras(id),
    UNIQUE (carrera_id, anio_vigencia)
);

-- ========================================
-- MÓDULO B.1 EXTENSION - PERÍODOS LECTIVOS
-- ========================================

CREATE TABLE IF NOT EXISTS periodos_lectivos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    anio INTEGER NOT NULL,
    cuatrimestre INTEGER NOT NULL CHECK(cuatrimestre IN (1, 2)),
    descripcion TEXT,
    activo INTEGER NOT NULL DEFAULT 0,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (anio, cuatrimestre)
);

-- ========================================
-- MÓDULO C.1 - COMISIONES
-- ========================================

CREATE TABLE IF NOT EXISTS comisiones (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    materia_id INTEGER NOT NULL,
    plan_estudio_id INTEGER NOT NULL,
    periodo_id INTEGER NOT NULL,
    turno TEXT NOT NULL CHECK(turno IN ('MANANA', 'TARDE', 'NOCHE')),
    nombre TEXT,
    estado TEXT NOT NULL DEFAULT 'ACTIVA' CHECK(estado IN ('ACTIVA', 'CERRADA')),
    creada_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actualizada_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    creada_por INTEGER,
    UNIQUE (materia_id, plan_estudio_id, periodo_id, turno),
    FOREIGN KEY (materia_id) REFERENCES materias(id),
    FOREIGN KEY (plan_estudio_id) REFERENCES planes_estudio(id),
    FOREIGN KEY (periodo_id) REFERENCES periodos_lectivos(id),
    FOREIGN KEY (creada_por) REFERENCES personas(id)
);

-- ========================================
-- MÓDULO D.1 - ASIGNACIÓN DOCENTES
-- ========================================

CREATE TABLE IF NOT EXISTS asignaciones_docentes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    comision_id INTEGER NOT NULL,
    docente_id INTEGER NOT NULL,
    cargo TEXT NOT NULL CHECK(cargo IN ('TITULAR', 'JTP', 'AYUDANTE')),
    fecha_asignacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    asignado_por INTEGER NOT NULL,
    activa INTEGER NOT NULL DEFAULT 1,
    UNIQUE (comision_id, docente_id),
    FOREIGN KEY (comision_id) REFERENCES comisiones(id),
    FOREIGN KEY (docente_id) REFERENCES docentes(id),
    FOREIGN KEY (asignado_por) REFERENCES personas(id)
);

-- ========================================
-- MÓDULO B.2 - GESTIÓN DE MATERIAS
-- ========================================

CREATE TABLE IF NOT EXISTS materias (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo TEXT NOT NULL UNIQUE,
    nombre TEXT NOT NULL UNIQUE,
    descripcion TEXT,
    activa INTEGER NOT NULL DEFAULT 1,
    creada_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actualizada_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    creada_por INTEGER,
    FOREIGN KEY (creada_por) REFERENCES personas(id)
);

CREATE TABLE IF NOT EXISTS materias_planes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    materia_id INTEGER NOT NULL,
    plan_estudio_id INTEGER NOT NULL,
    anio_plan INTEGER NOT NULL,
    cuatrimestre TEXT NOT NULL CHECK(cuatrimestre IN ('PRIMERO', 'SEGUNDO', 'ANUAL')),
    carga_horaria INTEGER NOT NULL,
    caracter TEXT NOT NULL CHECK(caracter IN ('OBLIGATORIA', 'ELECTIVA')),
    activa INTEGER NOT NULL DEFAULT 1,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    creado_por INTEGER,
    UNIQUE (materia_id, plan_estudio_id),
    FOREIGN KEY (materia_id) REFERENCES materias(id),
    FOREIGN KEY (plan_estudio_id) REFERENCES planes_estudio(id),
    FOREIGN KEY (creado_por) REFERENCES personas(id)
);

-- ========================================
-- MÓDULO B.2 EXTENSION - CORRELATIVIDADES
-- ========================================

CREATE TABLE IF NOT EXISTS correlatividades (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    materia_plan_origen_id INTEGER NOT NULL,
    materia_plan_destino_id INTEGER NOT NULL,
    condicion TEXT NOT NULL CHECK(condicion IN ('APROBADA', 'REGULAR')),
    FOREIGN KEY (materia_plan_origen_id) REFERENCES materias_planes(id) ON DELETE CASCADE,
    FOREIGN KEY (materia_plan_destino_id) REFERENCES materias_planes(id) ON DELETE CASCADE,
    UNIQUE (materia_plan_origen_id, materia_plan_destino_id, condicion)
);

-- Agregar departamento a docentes
ALTER TABLE docentes ADD COLUMN departamento TEXT;

-- ========================================
-- MÓDULO C - INSCRIPCIÓN DE ESTUDIANTES
-- ========================================

CREATE TABLE IF NOT EXISTS inscripciones_comisiones (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    estudiante_id INTEGER NOT NULL,
    comision_id INTEGER NOT NULL,
    fecha_inscripcion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estado TEXT NOT NULL DEFAULT 'ACTIVA' CHECK(estado IN ('ACTIVA', 'RETIRADO', 'APROBADO', 'REPROBADO')),
    UNIQUE (estudiante_id, comision_id),
    FOREIGN KEY (estudiante_id) REFERENCES estudiantes(id) ON DELETE CASCADE,
    FOREIGN KEY (comision_id) REFERENCES comisiones(id) ON DELETE CASCADE
);

-- ========================================
-- MÓDULO D.2 - GESTIÓN DE ACTAS
-- ========================================

CREATE TABLE IF NOT EXISTS actas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    comision_id INTEGER NOT NULL,
    periodo_id INTEGER NOT NULL,
    instancia TEXT NOT NULL CHECK(instancia IN (
        'parcial_1','parcial_2','tp',
        'recuperatorio_1','recuperatorio_2','examen_final')),
    estado TEXT NOT NULL DEFAULT 'abierta' CHECK(estado IN ('abierta','cerrada')),
    fecha_cierre TIMESTAMP,
    cerrada_por_docente_id INTEGER,
    reabierta_por_admin_id INTEGER,
    fecha_reapertura TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (comision_id, periodo_id, instancia),
    FOREIGN KEY (comision_id) REFERENCES comisiones(id),
    FOREIGN KEY (periodo_id) REFERENCES periodos_lectivos(id),
    FOREIGN KEY (cerrada_por_docente_id) REFERENCES personas(id),
    FOREIGN KEY (reabierta_por_admin_id) REFERENCES personas(id)
);

CREATE TABLE IF NOT EXISTS notas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    acta_id INTEGER NOT NULL,
    estudiante_id INTEGER NOT NULL,
    valor REAL,
    ausente INTEGER NOT NULL DEFAULT 0,
    cargada_por_docente_id INTEGER NOT NULL,
    ultima_modificacion_por INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (acta_id, estudiante_id),
    FOREIGN KEY (acta_id) REFERENCES actas(id),
    FOREIGN KEY (estudiante_id) REFERENCES estudiantes(id),
    FOREIGN KEY (cargada_por_docente_id) REFERENCES personas(id),
    FOREIGN KEY (ultima_modificacion_por) REFERENCES personas(id)
);

CREATE TABLE IF NOT EXISTS auditoria_acta (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    acta_id INTEGER NOT NULL,
    accion TEXT NOT NULL CHECK(accion IN (
        'creacion','carga_nota','modificacion_nota','cierre','reapertura')),
    realizado_por INTEGER NOT NULL,
    detalle TEXT,
    motivo TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (acta_id) REFERENCES actas(id),
    FOREIGN KEY (realizado_por) REFERENCES personas(id)
);

