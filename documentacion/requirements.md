# Proyecto Integrador: Sistema de Gestión Estudiantil (UNRC)

## 1. Problema que se quiere resolver

La Universidad enfrenta dificultades debido al uso de planillas y sistemas antiguos que no están conectados entre sí. Esto genera un desafío constante en la comunicación entre docentes y alumnos. El objetivo es centralizar la información para simplificar la gestión administrativa y académica.

Uno de los mayores problemas actuales es la gestión de correlatividades, la cual se realiza de forma manual, provocando errores en las inscripciones que el sistema deberá validar automáticamente.

---

## 2. Usuarios del Sistema

Se han identificado tres actores principales con permisos diferenciados:

- **Administradores (Oficina de Alumnos):** Tienen acceso total para gestionar estudiantes, docentes, materias y carreras.
- **Estudiantes:** Consultan su información académica, historial de notas y realizan inscripciones a materias.
- **Profesores:** Encargados de cargar notas, consultar listados de alumnos y realizar el seguimiento del desempeño académico.

---

## 3. Funcionalidades Detalladas (Desglose por Módulos)

Para cumplir con los objetivos de centralización y automatización, el sistema se divide en los siguientes alcances específicos:

### A. Módulo de Gestión de Usuarios y Seguridad

- **Autenticación y Perfiles:** Sistema de login con cifrado de credenciales y redirección basada en roles (Admin, Estudiante, Docente).
- **Administración de Legajos:** CRUD completo de Estudiantes y Profesores, incluyendo validación de formatos de DNI, Email y Teléfono.
- **Gestión de Sesiones:** Control de acceso para asegurar que cada actor solo visualice y edite los datos permitidos por su rol.

### B. Módulo Académico (Configuración)

- **Gestión de Oferta Académica:** Definición de Carreras y sus respectivos Planes de Estudio por año de vigencia.
- **Grafo de Correlatividades:** Carga de requisitos de pre-requisito (materia anterior) y posterior, distinguiendo entre condición de "Regular" o "Aprobada" para habilitar la siguiente.
- **Administración de Comisiones:** Creación de espacios de cursada vinculando materias con horarios y aulas (físicas o virtuales).

### C. Módulo de Inscripciones y Motor de Reglas

- **Inscripción a Cursadas:** Validación en tiempo real del cumplimiento de correlativas regulares antes de confirmar el alta en una comisión.
- **Inscripción a Exámenes Finales:** Validación automática de la condición de "Regular" en la materia y correlativas aprobadas según el plan.
- **Gestión de Cupos:** Control de capacidad máxima por comisión para evitar la sobrepoblación de las aulas.

### D. Módulo de Gestión Docente y Calificaciones

- **Asignación de Roles de Cátedra:** Vinculación de uno o más docentes a una materia con cargos específicos (Titular, JTP, Ayudante) por período lectivo.
- **Carga de Notas y Actas:** Interfaz para que el docente registre calificaciones de parciales, trabajos prácticos y exámenes finales.
- **Cierre de Condición:** Proceso automático/manual para determinar si un alumno queda en condición de "Libre", "Regular" o "Promocionado".

### E. Módulo de Seguimiento y Valor Agregado (Propuesta del Equipo)

- **Dashboard de Progreso Estudiantil:** Visualización gráfica para el alumno sobre su avance porcentual en la carrera.
- **Sistema de Alertas Tempranas:** Notificación automática a la Oficina de Alumnos sobre estudiantes con más de dos aplazos consecutivos o inactividad prolongada.
- **Exportación Documental:** Generación de archivos PDF para listas de inscriptos, actas de examen y certificados analíticos parciales.

---

## 4. Restricciones Técnicas (Requerimientos No Funcionales)

Para asegurar la calidad del software, el sistema debe cumplir con:

- **Seguridad:** Protección de datos almacenados y control de acceso por roles para evitar manipulaciones indebidas.
- **Mantenibilidad:** Arquitectura modular y extensible que facilite la incorporación de nuevas funciones.
- **Escalabilidad:** Capacidad para soportar un mayor número de carreras, planes y alumnos sin perder rendimiento.
- **Performance:** Procesamiento rápido de inscripciones y validaciones masivas.
- **Usabilidad:** Interfaz clara e intuitiva que facilite la experiencia del usuario.

---

## 5. Información del Proyecto

### Tamaño del Equipo

El equipo está conformado por 6 integrantes: Cruseño Alvaro, Dehaes Juan, Destefanis Adrian, Dominguez Alan, Garais Santiago y Narvaja Samuel.

### Tecnologías Elegidas y Justificación

- **Java:** Lenguaje principal por su robustez y soporte de Programación Orientada a Objetos.
- **Spark Java:** Framework web ligero para la gestión de rutas y peticiones.
- **SQLite / ActiveJDBC:** Motor de base de datos relacional y ORM para facilitar la portabilidad y gestión eficiente de consultas SQL.
- **Mustache:** Sistema de plantillas para el renderizado dinámico de vistas.

---

## 6. Seguimiento y Organización

### Plazo estimado

El proyecto se desarrolla según el calendario de Ingeniería de Software II 2026.

### Cambios de alcance ocurridos

No se registran cambios; el alcance se mantiene fiel a los requerimientos de la Oficina de Alumnos detallados en la narrativa inicial.

### Problemas encontrados

1. Complejidad en el modelado UML de la recursividad para las correlatividades de materias.
2. Definición de la clase asociativa "Periodo" para gestionar múltiples roles docentes por cátedra sin conflictos.
3. Estandarización del entorno de desarrollo (archivo dev.db) para evitar inconsistencias entre los 6 miembros del equipo.

### Forma de organización del equipo

Para este proyecto, el equipo distribuyó las responsabilidades mediante el uso de GitHub Projects, asignando responsables por cada módulo funcional y realizando revisiones de código (Code Reviews) para asegurar la calidad del desarrollo.

---

## 7. Diagrama de Arquitectura del Sistema

El sistema sigue una arquitectura **Cliente-Servidor en tres capas** (Presentación, Lógica de Negocio y Persistencia), desplegada como una aplicación web monolítica empaquetada en un JAR ejecutable.

### 7.1 Arquitectura General (Vista de Contexto)

```
     ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
     │ Administrador │   │  Estudiante  │   │   Profesor   │
     │(Of. Alumnos)  │   │              │   │              │
     └──────┬───────┘   └──────┬───────┘   └──────┬───────┘
            │                  │                   │
            │  HTTP Request    │  HTTP Request      │  HTTP Request
            ▼                  ▼                   ▼
     ┌─────────────────────────────────────────────────────┐
     │          Sistema de Gestión Estudiantil              │
     │    ┌───────────────────────────────────────────┐    │
     │    │       Aplicación Web (Java + Spark)       │    │
     │    │             Puerto 8080                    │    │
     │    └─────────────────────┬─────────────────────┘    │
     └──────────────────────────┼──────────────────────────┘
                                │
                                │  JDBC / ActiveJDBC
                                ▼
                       ┌────────────────┐
                       │    SQLite       │
                       │   (dev.db)      │
                       └────────────────┘
```

### 7.2 Arquitectura en Capas

```
┌─────────────────────────────────────────────────────────────────────┐
│                   CAPA DE PRESENTACIÓN (Cliente)                    │
│                                                                     │
│  ┌─────────────┐  ┌────────────────────┐  ┌─────────┐  ┌────────┐ │
│  │  Navegador   │  │ Plantillas Mustache│  │   CSS   │  │   JS   │ │
│  │    Web       │  │  login.mustache    │  │ login-  │  │ login- │ │
│  │              │  │  dashboard.mustache│  │ style   │  │ script │ │
│  │              │  │  professor_form    │  │  .css   │  │  .js   │ │
│  │              │  │  professor_list    │  │         │  │        │ │
│  │              │  │  user_form.mustache│  │         │  │        │ │
│  │              │  │  error.mustache    │  │         │  │        │ │
│  └──────┬──────┘  └────────────────────┘  └─────────┘  └────────┘ │
└─────────┼───────────────────────────────────────────────────────────┘
          │ HTTP GET / POST
          ▼
┌─────────────────────────────────────────────────────────────────────┐
│              CAPA DE APLICACIÓN (Servidor - Spark Framework)        │
│                                                                     │
│  ┌──────────────────┐  ┌──────────────┐  ┌───────────────────────┐ │
│  │  Rutas HTTP       │  │   Filtros    │  │  Control de Sesión    │ │
│  │  (App.java)       │  │   Spark      │  │  Autenticación        │ │
│  │  GET / POST       │  │  before:     │  │  con BCrypt           │ │
│  │  endpoints        │  │  abrir DB    │  │                       │ │
│  │                   │  │  after:      │  │                       │ │
│  │                   │  │  cerrar DB   │  │                       │ │
│  └────────┬─────────┘  └──────────────┘  └───────────────────────┘ │
│           │                                                         │
│  ┌────────┴────────────┐  ┌───────────────────────┐                │
│  │  MustacheTemplate   │  │  Jackson ObjectMapper  │                │
│  │  Engine (render)    │  │  (Serialización JSON)  │                │
│  └─────────────────────┘  └───────────────────────┘                │
└─────────┬───────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    CAPA DE MODELO / DOMINIO                         │
│                                                                     │
│  ┌───────────────────────┐    ┌───────────────────────────┐        │
│  │     User (Model)      │    │     Professor (Model)     │        │
│  │  Tabla: users          │    │  Tabla: professors         │        │
│  │  Campos: id, name,    │    │  Campos: id, dni, name,   │        │
│  │          password      │    │    email, department,      │        │
│  │                       │    │    phone, created_at,      │        │
│  │                       │    │    updated_at              │        │
│  └───────────┬───────────┘    └─────────────┬─────────────┘        │
└──────────────┼──────────────────────────────┼──────────────────────┘
               │                              │
               ▼                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     CAPA DE PERSISTENCIA                            │
│                                                                     │
│  ┌───────────────┐    ┌──────────────────┐    ┌────────────────┐   │
│  │  ActiveJDBC   │───▶│  SQLite JDBC     │───▶│    SQLite      │   │
│  │    ORM        │    │    Driver         │    │   (dev.db)     │   │
│  └───────────────┘    └──────────────────┘    └────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
               ▲
               │ Configuración
┌──────────────┴──────────────────────────────────────────────────────┐
│                    CAPA DE CONFIGURACIÓN                            │
│                                                                     │
│             ┌─────────────────────────────────┐                    │
│             │      DBConfigSingleton          │                    │
│             │      (Patrón Singleton)         │                    │
│             │  driver: org.sqlite.JDBC        │                    │
│             │  url: jdbc:sqlite:./db/dev.db   │                    │
│             │  user: (vacío)                  │                    │
│             │  pass: (vacío)                  │                    │
│             └─────────────────────────────────┘                    │
└─────────────────────────────────────────────────────────────────────┘
```

### 7.3 Diagrama de Componentes y Responsabilidades

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                              COMPONENTES DEL SISTEMA                             │
├─────────────────────┬────────────────────────────────────────────────────────────┤
│  Componente         │  Responsabilidad                                           │
├─────────────────────┼────────────────────────────────────────────────────────────┤
│  App.java           │  Punto de entrada. Configura puerto, rutas HTTP            │
│                     │  (GET/POST), filtros before/after, manejo de errores       │
│                     │  (404/500). Orquesta toda la lógica de la aplicación.      │
├─────────────────────┼────────────────────────────────────────────────────────────┤
│  DBConfigSingleton  │  Patrón Singleton. Centraliza la configuración de          │
│                     │  conexión a la base de datos (driver, URL, credenciales).  │
│                     │  Provee métodos openConnection() y closeConnection().      │
├─────────────────────┼────────────────────────────────────────────────────────────┤
│  User (Model)       │  Modelo ActiveJDBC mapeado a la tabla 'users'.            │
│                     │  Encapsula la lógica de gestión de usuarios.              │
│                     │  Contiene validaciones de credenciales (BCrypt).          │
├─────────────────────┼────────────────────────────────────────────────────────────┤
│  Professor (Model)  │  Modelo ActiveJDBC mapeado a la tabla 'professors'.       │
│                     │  Encapsula la gestión de datos de profesores.             │
│                     │  Valida formatos (email, DNI, teléfono).                  │
├─────────────────────┼────────────────────────────────────────────────────────────┤
│  Mustache          │  Motor de plantillas que renderiza vistas dinámicas        │
│                     │  (login.mustache, dashboard.mustache, etc.).              │
│                     │  Recibe objetos del modelo y genera HTML.                 │
└─────────────────────┴────────────────────────────────────────────────────────────┘
```

---

## 8. Diagrama de Diseño

### 8.1 Diagrama de Clases

```
┌─────────────────────────────────────────────┐
│                   App                        │
├─────────────────────────────────────────────┤
│ - objectMapper : ObjectMapper  [static]      │
├─────────────────────────────────────────────┤
│ + main(args: String[]) : void  [static]      │
└──────────┬──────────┬───────────┬───────────┘
           │          │           │
           │ usa      │ crea/     │ renderiza
           │          │ consulta  │
           ▼          │           ▼
┌─────────────────────────────┐  ┌──────────────────────────┐
│     DBConfigSingleton       │  │  MustacheTemplateEngine   │
├─────────────────────────────┤  ├──────────────────────────┤
│ - instance : DBConfig       │  │                          │
│   [static]                  │  ├──────────────────────────┤
│ - dbUrl    : String         │  │ + render(mv) : String    │
│ - user     : String         │  └──────────────────────────┘
│ - pass     : String         │
│ - driver   : String         │  ┌──────────────────────────┐
├─────────────────────────────┤  │      ModelAndView         │
│ - DBConfigSingleton()       │  ├──────────────────────────┤
│ + getInstance() : DBConfig  │  │ - model    : Object      │
│   [static]                  │  │ - viewName : String       │
│ + openConnection()  : void  │  ├──────────────────────────┤
│ + closeConnection() : void  │  │ + ModelAndView(model,    │
│ + getDbUrl()  : String      │  │        viewName)          │
│ + getUser()   : String      │  └──────────────────────────┘
│ + getPass()   : String      │
│ + getDriver() : String      │
└──────────────┬──────────────┘
               │ configura conexión
               ▼
┌─────────────────────────────────────────────┐
│           Model  <<ActiveJDBC>>              │
├─────────────────────────────────────────────┤
│ + set(attr, value) : Model                   │
│ + getString(attr)  : String                  │
│ + getInteger(attr) : Integer                 │
│ + getId()          : Object                  │
│ + saveIt()         : boolean                 │
│ + findFirst(query, params) : Model  [static] │
│ + findAll()  : LazyList             [static] │
│ + count()    : Long                 [static] │
└──────────┬──────────────────────┬───────────┘
           │                      │
           │  hereda              │  hereda
           ▼                      ▼
┌──────────────────────┐  ┌─────────────────────────────┐
│       User           │  │         Professor            │
├──────────────────────┤  ├─────────────────────────────┤
│ Tabla: users         │  │ Tabla: professors            │
├──────────────────────┤  ├─────────────────────────────┤
│ + getName() : String │  │ Validaciones:                │
│ + setName(name)      │  │  - email : presencia+formato │
│ + getPassword()      │  │  - name  : presencia         │
│ + setPassword(pass)  │  │  - DNI   : presencia         │
└──────────────────────┘  └─────────────────────────────┘
         ▲                          ▲
         │  crea/consulta           │  crea/consulta
         │                          │
         └────────────┬─────────────┘
                      │
                ┌─────┴─────┐
                │   App     │
                └───────────┘
```

### 8.2 Diagrama de Paquetes

```
┌─────────────────────────────────────────────────────────────────────┐
│                       com.is1.proyecto                              │
│                                                                     │
│   ┌───────────────────────────────────────┐                        │
│   │             App.java                   │                        │
│   │       (Controlador Principal)          │                        │
│   └─────────┬─────────────┬───────────────┘                        │
│             │             │                                         │
│             ▼             ▼                                         │
│   ┌─────────────────┐  ┌──────────────────────┐                   │
│   │   config/        │  │     models/           │                   │
│   │                 │  │                      │                   │
│   │ DBConfigSingle- │  │  User.java           │                   │
│   │  ton.java       │  │  Professor.java      │                   │
│   └─────────────────┘  └──────────────────────┘                   │
└─────────────────────────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         resources/                                   │
│                                                                     │
│   ┌──────────────────────────┐    ┌──────────────────────────┐     │
│   │      templates/           │    │       static/             │     │
│   │                          │    │                          │     │
│   │  login.mustache          │    │  css/                    │     │
│   │  dashboard.mustache      │    │    login-style.css       │     │
│   │  professor_form.mustache │    │  js/                     │     │
│   │  professor_list.mustache │    │    login-script.js       │     │
│   │  user_form.mustache      │    │  images/                 │     │
│   │  error.mustache          │    │                          │     │
│   └──────────────────────────┘    └──────────────────────────┘     │
│                                                                     │
│   database.properties                                               │
│   scheme.sql                                                        │
└─────────────────────────────────────────────────────────────────────┘
                      │
                      ▼
                ┌───────────┐
                │    db/     │
                │  dev.db    │
                └───────────┘
```

### 8.3 Mapa de Rutas del Sistema

```
┌─────────┬──────────────────┬───────────────┬─────────────────────────────────┬───────────────────────────────┐
│ Método  │ Ruta             │ Autenticación │ Descripción                     │ Vista                         │
├─────────┼──────────────────┼───────────────┼─────────────────────────────────┼───────────────────────────────┤
│ GET     │ /                │ No            │ Formulario de inicio de sesión  │ login.mustache                │
│ POST    │ /login           │ No            │ Procesa credenciales de login   │ login / dashboard.mustache    │
│ GET     │ /logout          │ Sí            │ Cierra sesión del usuario       │ Redirige a /                  │
│ GET     │ /dashboard       │ Sí            │ Panel de control del usuario    │ dashboard.mustache            │
│ GET     │ /user/create     │ No            │ Formulario de creación de cuenta│ user_form.mustache            │
│ GET     │ /user/new        │ No            │ Alias del formulario de creación│ user_form.mustache            │
│ POST    │ /user/new        │ No            │ Registra nuevo usuario (BCrypt) │ Redirige a /user/create       │
│ POST    │ /add_users       │ No            │ API JSON para agregar usuarios  │ Respuesta JSON                │
│ GET     │ /professor/new   │ Sí            │ Formulario para agregar profesor│ professor_form.mustache       │
│ POST    │ /professor/new   │ Sí            │ Registra nuevo profesor en DB   │ Redirige a /professor/new     │
│ GET     │ /professor/list  │ No            │ Lista paginada de profesores    │ professor_list.mustache        │
└─────────┴──────────────────┴───────────────┴─────────────────────────────────┴───────────────────────────────┘
```

### 8.4 Esquema de Base de Datos

```
┌────────────────────────────────────────────┐
│                  USERS                      │
├──────────────┬─────────┬───────────────────┤
│ Columna      │ Tipo    │ Restricciones     │
├──────────────┼─────────┼───────────────────┤
│ id           │ INTEGER │ PK, AUTOINCREMENT │
│ name         │ TEXT    │ NOT NULL, UNIQUE  │
│ password     │ TEXT    │ NOT NULL (BCrypt) │
└──────────────┴─────────┴───────────────────┘

┌────────────────────────────────────────────────────────┐
│                     PROFESSORS                          │
├──────────────┬───────────┬─────────────────────────────┤
│ Columna      │ Tipo      │ Restricciones               │
├──────────────┼───────────┼─────────────────────────────┤
│ id           │ INTEGER   │ PK, AUTOINCREMENT           │
│ dni          │ INTEGER   │ UNIQUE, NOT NULL            │
│ name         │ TEXT      │ NOT NULL                    │
│ email        │ TEXT      │ UNIQUE                      │
│ department   │ TEXT      │                             │
│ phone        │ TEXT      │                             │
│ created_at   │ TIMESTAMP │ DEFAULT CURRENT_TIMESTAMP   │
│ updated_at   │ TIMESTAMP │ DEFAULT CURRENT_TIMESTAMP   │
└──────────────┴───────────┴─────────────────────────────┘
```

### 8.5 Patrones de Diseño Utilizados

```
┌─────────────────────┬────────────────────────────────────────────────────────────┐
│ Patrón              │ Aplicación en el Sistema                                   │
├─────────────────────┼────────────────────────────────────────────────────────────┤
│ Singleton           │ DBConfigSingleton: garantiza una única instancia de        │
│                     │ configuración de base de datos en toda la aplicación.      │
├─────────────────────┼────────────────────────────────────────────────────────────┤
│ MVC (Model-View-    │ Modelo: clases User y Professor (ActiveJDBC).             │
│  Controller)        │ Vista: plantillas Mustache.                               │
│                     │ Controlador: rutas definidas en App.java.                 │
├─────────────────────┼────────────────────────────────────────────────────────────┤
│ Template Engine     │ Mustache separa la lógica de presentación (HTML) de la    │
│                     │ lógica del servidor (Java).                               │
├─────────────────────┼────────────────────────────────────────────────────────────┤
│ Filter/Interceptor  │ Filtros before y after de Spark para gestión transversal  │
│                     │ de conexiones a la base de datos.                         │
├─────────────────────┼────────────────────────────────────────────────────────────┤
│ ORM (ActiveRecord)  │ ActiveJDBC implementa el patrón Active Record: cada       │
│                     │ modelo representa una fila y encapsula el acceso a datos. │
└─────────────────────┴────────────────────────────────────────────────────────────┘
```
