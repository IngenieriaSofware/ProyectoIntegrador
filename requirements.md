# Documentación: Sistema de Gestión Estudiantil (Integrador)

## 1. Problema que se quiere resolver
La Universidad enfrenta dificultades debido al uso de planillas y sistemas antiguos que no están conectados entre sí. Esto genera un desafío constante en la comunicación entre docentes y alumnos. El objetivo es centralizar la información para simplificar la gestión administrativa y académica.

Uno de los mayores problemas actuales es la gestión de correlatividades, la cual se realiza de forma manual, provocando errores en las inscripciones que el sistema deberá validar automáticamente.

---

## 2. Usuarios del Sistema
Se han identificado tres actores principales con permisos diferenciados:

* **Administradores (Oficina de Alumnos):** Tienen acceso total para gestionar estudiantes, docentes, materias y carreras.
* **Estudiantes:** Consultan su información académica, historial de notas y realizan inscripciones a materias.
* **Profesores:** Encargados de cargar notas, consultar listados de alumnos y realizar el seguimiento del desempeño académico.

---

## 3. Funcionalidades Principales
El sistema integra las siguientes capacidades clave según el backlog inicial:

* **Gestión de Legajos:** Registro y actualización de datos personales y académicos de alumnos y docentes.
* **Administración Académica:** Configuración de planes de estudio, carreras y reglas de correlatividad.
* **Inscripciones y Validaciones:** Control automático de requisitos (correlativas aprobadas) para cursar o rendir materias.
* **Gestión de Cátedras:** Asignación flexible de roles para el equipo docente (Titular, JTP, Ayudante) por período.
* **Seguimiento Académico:** Visualización del progreso del alumno, detección de riesgos y generación de reportes o actas.

---

## 4. Restricciones Técnicas (Requerimientos No Funcionales)
Para asegurar la calidad del software, el sistema debe cumplir con:

* **Seguridad:** Protección de datos sensibles y control de acceso por roles.
* **Mantenibilidad:** Arquitectura modular que facilite actualizaciones.
* **Escalabilidad:** Capacidad para soportar el crecimiento en la cantidad de alumnos y carreras.
* **Performance:** Procesamiento rápido de inscripciones masivas.
* **Usabilidad:** Interfaz intuitiva y clara para facilitar la experiencia del usuario.

---

## 5. Información del Proyecto

### Tamaño del Equipo
* El equipo está conformado por **6 integrantes**: Cruseño Alvaro, Dehaes Juan, Destefanis Adrian, Dominguez Alan, Garais Santiago y Narvaja Samuel.

### Tecnologías Elegidas y Justificación
* **Java:** Lenguaje principal por su robustez y soporte de Programación Orientada a Objetos.
* **Spark:** Framework ligero para el manejo de rutas y peticiones web.
* **SQLite:** Motor de base de datos relacional que facilita la portabilidad del proyecto.
* **Mustache:** Sistema de plantillas para renderizar las vistas de forma dinámica.
* **JDBC:** Para una gestión directa y eficiente de las consultas SQL.

## 6. Seguimiento y Organización

### Plazo estimado
El proyecto se desarrolla según el calendario de Ingeniería de Software II..

### Cambios de alcance ocurridos
No se registran cambios; el alcance se mantiene fiel a los requerimientos de la Oficina de Alumnos detallados en la narrativa inicial.

### Problemas encontrados
El desafío principal fue modelar en UML la lógica de correlatividades y la asignación de múltiples roles docentes por período.

### Forma de organización del equipo
Para este práctico integrador, el equipo de 6 integrantes distribuyó las responsabilidades asignando tres responsables por cada ejercicio solicitado alternandolo.

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
│                     │  Campos: id, name, password. Getters/setters tipados.      │
├─────────────────────┼────────────────────────────────────────────────────────────┤
│  Professor (Model)  │  Modelo ActiveJDBC mapeado a la tabla 'professors'.       │
│                     │  Campos: id, dni, name, email, department, phone.          │
│                     │  Incluye validaciones de presencia y formato de email.     │
├─────────────────────┼────────────────────────────────────────────────────────────┤
│  Plantillas         │  Vistas HTML renderizadas del lado del servidor.           │
│  Mustache           │  Reciben un modelo (Map) y generan HTML dinámico.          │
├─────────────────────┼────────────────────────────────────────────────────────────┤
│  Archivos Estáticos │  CSS y JavaScript servidos por Spark desde /static.        │
│                     │  Definen estilos y comportamiento del lado del cliente.    │
├─────────────────────┼────────────────────────────────────────────────────────────┤
│  BCrypt             │  Biblioteca para hasheo seguro de contraseñas. Se usa al   │
│                     │  registrar usuarios y verificar credenciales en el login.  │
├─────────────────────┼────────────────────────────────────────────────────────────┤
│  Jackson            │  Serialización/deserialización JSON para endpoints de      │
│  ObjectMapper       │  tipo API (ej. /add_users).                               │
└─────────────────────┴────────────────────────────────────────────────────────────┘
```

### 7.4 Flujo de una Solicitud HTTP

```
  Cliente                Spark          Filtro         Ruta          Modelo        SQLite       Mustache       Filtro
 (Navegador)           Framework       before        (Handler)    (ActiveJDBC)      DB          Engine         after
     │                    │              │              │              │             │             │              │
     │  HTTP Request      │              │              │              │             │             │              │
     │ ──────────────────▶│              │              │              │             │             │              │
     │                    │  ejecutar    │              │              │             │             │              │
     │                    │─────────────▶│              │              │             │             │              │
     │                    │              │ Base.open()  │              │             │             │              │
     │                    │              │─────────────────────────────────────────▶│             │              │
     │                    │              │              │              │  conexión   │             │              │
     │                    │              │◀─────────────────────────────────────────│             │              │
     │                    │  delegar     │              │              │             │             │              │
     │                    │────────────────────────────▶│              │             │             │              │
     │                    │              │              │  consultar   │             │             │              │
     │                    │              │              │─────────────▶│             │             │              │
     │                    │              │              │              │  SQL query  │             │              │
     │                    │              │              │              │────────────▶│             │              │
     │                    │              │              │              │  resultado  │             │              │
     │                    │              │              │              │◀────────────│             │              │
     │                    │              │              │  objetos     │             │             │              │
     │                    │              │              │◀─────────────│             │             │              │
     │                    │              │              │  ModelAndView│             │             │              │
     │                    │              │              │──────────────────────────────────────── ▶│              │
     │                    │              │              │              │             │  HTML       │              │
     │  HTML renderizado  │              │              │              │             │  render     │              │
     │◀─────────────────────────────────────────────────────────────────────────────│             │              │
     │                    │  ejecutar    │              │              │             │             │              │
     │                    │──────────────────────────────────────────────────────────────────────▶│              │
     │                    │              │              │              │             │             │ Base.close() │
     │                    │              │              │              │             │◀────────────────────────── │
     │                    │              │              │              │             │             │              │
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