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

### C. Módulo de Inscripciones y Motor de Reglas

- **Inscripción a Cursadas:** Validación en tiempo real del cumplimiento de correlativas regulares antes de confirmar el alta en una comisión.
- **Inscripción a Exámenes Finales:** Validación automática de la condición de "Regular" en la materia y correlativas aprobadas según el plan.

### D. Módulo de Gestión Docente y Calificaciones

- **Asignación de Roles de Cátedra:** Vinculación de uno o más docentes a una materia con cargos específicos (Titular, JTP, Ayudante) por período lectivo.
- **Carga de Notas y Actas:** Interfaz para que el docente registre calificaciones de parciales, trabajos prácticos y exámenes finales.
- **Cierre de Condición:** Proceso automático/manual para determinar si un alumno queda en condición de "Libre", "Regular" o "Promocionado".

### E. Módulo de Seguimiento y Valor Agregado (Propuesta del Equipo)

- **Dashboard de Progreso Estudiantil:** Visualización gráfica para el alumno sobre su avance porcentual en la carrera.
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
---

## 9. Estimación de Esfuerzo

---

### 9.0 Guía de Técnicas Utilizadas

Se combinan seis técnicas complementarias para triangular la estimación y reducir el sesgo de cualquier método individual.

| Técnica | Paradigma | Qué mide | Input | Output |
|---------|-----------|----------|-------|--------|
| **Story Points** | Ágil | Esfuerzo relativo al equipo | Backlog + equipo | SP por historia |
| **FPA — IFPUG** | Funcional | Tamaño funcional del sistema | Requerimientos funcionales | UFP / AFP |
| **LOC** | Tradicional | Tamaño físico del código | AFP + factor por lenguaje | Líneas de código |
| **UCP** | Semi-formal | Complejidad vía casos de uso | CU + actores + factores | Use Case Points |
| **COCOMO Básico** | Algorítmico | Esfuerzo total por tamaño | KLOC | PM, meses, devs |
| **COCOMO II** | Algorítmico | Esfuerzo ajustado multi-factor | KLOC + 22 parámetros | PM, meses, devs |

#### Flujo de Dependencias entre Técnicas

```
Requerimientos Funcionales
        │
        ├─────────────────────────────► Story Points  (89 SP, planning ágil)
        │
        ▼
  FPA / IFPUG ──► 247 UFP × VAF(1.04) ──► 257 AFP
                                               │
                                        × 46 LOC/AFP
                                               │
                                          12 KLOC
                                         /        \
                                  COCOMO Básico   COCOMO II
                                    32.6 PM        53.4 PM
        │
        ▼
  UCP = 109 ──► × 20 h/UCP ──► 2,180 h (13.6 PM)
```

```
Rango de estimaciones resultante
──────────────────────────────────────────────────────────────
UCP / FPA    ████████░░░░░░░░░░░░░░░░░░░░░░░   ~14 PM  (optimista)
COCOMO Bás.  ████████████████████░░░░░░░░░░░    33 PM  (medio)
COCOMO II    ████████████████████████████████    53 PM  (conservador)
             |──────────────────────────────|
             0                             55 PM
```

---

### 9.1 Story Points — Planning Poker

#### ¿Qué es y cómo funciona?

Un Story Point (SP) es un número que representa el **esfuerzo relativo** que le cuesta al equipo completar una historia de usuario. No es horas, no es días. Es una apuesta colectiva sobre dificultad comparada.

Engloba tres dimensiones a la vez: complejidad lógica, volumen de trabajo e incertidumbre. La escala Fibonacci (1, 2, 3, 5, 8, 13, 21) se usa porque sus brechas crecen con el número, reflejando que cuanto más grande es una tarea, menos precisión real tenés:

```
1→2   diferencia 1  (tarea chica, podés ser preciso)
3→5   diferencia 2
5→8   diferencia 3
8→13  diferencia 5  (tarea grande, la diferencia entre 13 y 14 no es real)
13→21 diferencia 8
```

Con escala lineal el equipo discutiría si algo es 6 o 7 — pérdida de tiempo. Con Fibonacci la pregunta útil es solo: ¿es un 8 o un 13?

#### Proceso paso a paso

```
Paso 1 — Tomás una historia de usuario del backlog:
         "Como estudiante quiero inscribirme a una cursada"

Paso 2 — Cada miembro piensa su estimación en silencio

Paso 3 — Todos revelan a la vez (Planning Poker: cartas o app)
         Samuel: 8   Alan: 13   Juan: 8   Santiago: 5

Paso 4 — Los extremos explican su razonamiento:
         Santiago: "solo un INSERT a la BD"
         Alan: "hay que recorrer el grafo de correlativas, es complejo"

Paso 5 — El equipo vota de nuevo hasta converger → 13 ✓

Paso 6 — Repetís para cada historia. Al final sumás todos los SP.

Paso 7 — Velocidad del equipo surge de los primeros 2-3 sprints:
         89 SP ÷ velocidad media ≈ número de sprints necesarios
```

**Escala Fibonacci aplicada:** 1 · 2 · 3 · 5 · 8 · 13 · 21

#### Backlog del Proyecto

| ID | Ítem de Backlog | Módulo | Prioridad | Complejidad | SP |
|----|----------------|--------|-----------|-------------|:--:|
| A1 | Autenticación y Perfiles (login, BCrypt, redirección por rol) | Seguridad | Alta | Media | 5 |
| A2 | CRUD Legajos Estudiantes y Profesores + validaciones DNI/Email/Tel | Seguridad | Alta | Alta | 8 |
| A3 | Gestión de Sesiones y control de acceso por rol | Seguridad | Alta | Baja | 3 |
| B1 | Oferta Académica: Carreras y Planes de Estudio | Académico | Alta | Alta | 8 |
| B2 | Grafo de Correlatividades (Regular / Aprobada, recursivo) | Académico | Alta | Muy Alta | 13 |
| C1 | Inscripción a Cursadas + Motor de validación de correlativas | Motor | Alta | Muy Alta | 13 |
| C2 | Inscripción a Exámenes Finales + validación condición regular | Motor | Alta | Alta | 8 |
| D1 | Asignación Roles de Cátedra (Titular / JTP / Ayudante) | Docente | Media | Media | 5 |
| D2 | Carga de Notas y Actas (parciales, TPs, finales) | Docente | Alta | Alta | 8 |
| D3 | Cierre de Condición (Libre / Regular / Promocionado) | Docente | Alta | Alta | 8 |
| E1 | Dashboard de Progreso Estudiantil (visualización gráfica %) | Valor Agr. | Media | Media | 5 |
| E2 | Exportación Documental a PDF (listas, actas, analíticos) | Valor Agr. | Media | Media | 5 |
| | **TOTAL** | | | | **89 SP** |

#### Gráfico: Story Points por Ítem

```
Story Points por Ítem de Backlog
─────────────────────────────────────────────────────────────────
A1  Autenticación       ▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░   5 SP
A2  CRUD Legajos        ▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░   8 SP
A3  Sesiones            ▓▓▓░░░░░░░░░░░░░░░░░░░░░░░   3 SP
B1  Oferta Académica    ▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░   8 SP
B2  Correlativas        ▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░  13 SP  ◄ max
C1  Insc. Cursadas      ▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░  13 SP  ◄ max
C2  Insc. Finales       ▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░   8 SP
D1  Roles Cátedra       ▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░   5 SP
D2  Carga de Notas      ▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░   8 SP
D3  Cierre Condición    ▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░   8 SP
E1  Dashboard           ▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░   5 SP
E2  Export PDF          ▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░   5 SP
                        |────────────────────────|
                        0    3    6    9   12   15

Total: 89 SP  ·  Velocidad estimada: ~18 SP/sprint (2 semanas)  ·  5-6 sprints
```

#### Distribución en Sprints

```
Sprint   Ítems                              SP    Acum.   Avance
─────────────────────────────────────────────────────────────────
Sprint 1 A1 + A3                           8 SP    8 SP     9%
Sprint 2 A2 + D1                          13 SP   21 SP    24%
Sprint 3 B1 + B2 (parcial)               13 SP   34 SP    38%
Sprint 4 B2 (resto) + C1 (parcial)       18 SP   52 SP    58%
Sprint 5 C1 (resto) + C2 + D3            18 SP   70 SP    79%
Sprint 6 D2 + E1 + E2                    19 SP   89 SP   100%
─────────────────────────────────────────────────────────────────
TOTAL    6 sprints × 2 semanas           89 SP          ~12 semanas
```

---

### 9.2 Análisis de Puntos de Función — IFPUG FPA

#### ¿Qué es y cómo funciona?

El FPA (Albrecht, 1979) mide el **tamaño funcional** del sistema desde la perspectiva del usuario, independiente del lenguaje o tecnología. Esto permite comparar proyectos entre sí aunque estén en distintos lenguajes.

#### Proceso paso a paso

```
Paso 1 — Recorrés los requerimientos y clasificás cada elemento en 5 tipos:

  ¿El sistema guarda o mantiene estos datos internamente?
    → ILF  (Archivo Lógico Interno)
    Ejemplo: tabla users, tabla correlatives, tabla grades

  ¿El usuario envía datos al sistema que modifican su estado?
    → EI   (Entrada Externa)
    Ejemplo: formulario de inscripción, cargar nota

  ¿El sistema genera un reporte o dato derivado con lógica?
    → EO   (Salida Externa)
    Ejemplo: exportar PDF con acta de examen

  ¿El usuario solo consulta datos sin que haya cálculo derivado?
    → EQ   (Consulta Externa)
    Ejemplo: ver lista de profesores, ver historial de notas

  ¿El sistema usa datos que vienen de otro sistema externo?
    → EIF  (Archivo de Interfaz Externa)
    Ejemplo: en este proyecto no aplica

Paso 2 — Asignás complejidad a cada elemento:
  Simple / Media / Compleja
  según cantidad de campos (DETs) y tablas referenciadas (FTRs)

Paso 3 — Multiplicás por la tabla de pesos IFPUG estándar:
  ILF:  Simple=7  Media=10  Compleja=15
  EI:   Simple=3  Media=4   Compleja=6
  EO:   Simple=4  Media=5   Compleja=7
  EQ:   Simple=3  Media=4   Compleja=6

Paso 4 — Sumás todo → UFP (Puntos de Función No Ajustados)

Paso 5 — Respondés 14 preguntas sobre el sistema (0 a 5 cada una):
  ¿Tiene comunicaciones de datos? ¿Performance crítica?
  ¿Seguridad especial? ¿Múltiples sitios?...
  Sumás los 14 valores → aplicás: VAF = 0.65 + (0.01 × suma)

Paso 6 — AFP = UFP × VAF  (Puntos de Función Ajustados)
  Estos AFP son tu medida de tamaño funcional final.
```

#### ILF — Archivos Lógicos Internos

| Archivo Lógico Interno | Complejidad | UFP |
|------------------------|:-----------:|:---:|
| users | Media | 10 |
| professors | Media | 10 |
| students | Media | 10 |
| careers | Simple | 7 |
| study_plans | Media | 10 |
| subjects | Media | 10 |
| correlatives | Compleja | 15 |
| enrollments_cursada | Media | 10 |
| enrollments_exam | Media | 10 |
| chair_roles | Media | 10 |
| grades | Media | 10 |
| conditions | Simple | 7 |
| **Subtotal ILF** | 12 elementos | **119** |

#### EI — Entradas Externas

| Transacción EI | Complejidad | UFP |
|----------------|:-----------:|:---:|
| Login | Simple | 3 |
| Crear/Editar Usuario | Media | 4 |
| CRUD Profesor | Media | 4 |
| CRUD Estudiante | Media | 4 |
| Crear Carrera | Simple | 3 |
| Crear Plan de Estudio | Media | 4 |
| Agregar Materia | Media | 4 |
| Agregar Correlativa | Compleja | 6 |
| Inscribir a Cursada | Compleja | 6 |
| Inscribir a Examen | Compleja | 6 |
| Asignar Rol de Cátedra | Media | 4 |
| Cargar Nota Parcial | Media | 4 |
| Cargar Nota TP | Media | 4 |
| Cargar Nota Final | Media | 4 |
| Cerrar Condición Académica | Compleja | 6 |
| **Subtotal EI** | 15 elementos | **66** |

#### EO — Salidas Externas

| Transacción EO | Complejidad | UFP |
|----------------|:-----------:|:---:|
| Dashboard Progreso Estudiantil | Media | 5 |
| PDF Lista de Inscriptos | Compleja | 7 |
| PDF Acta de Examen | Compleja | 7 |
| PDF Certificado Analítico Parcial | Compleja | 7 |
| **Subtotal EO** | 4 elementos | **26** |

#### EQ — Consultas Externas

| Transacción EQ | Complejidad | UFP |
|----------------|:-----------:|:---:|
| Formulario de Login | Simple | 3 |
| Vista Dashboard | Media | 4 |
| Lista de Profesores (paginada) | Media | 4 |
| Perfil Académico Estudiante | Media | 4 |
| Lista de Carreras | Simple | 3 |
| Vista Plan de Estudio | Media | 4 |
| Vista Grafo Correlativas | Compleja | 6 |
| Historial de Inscripciones | Media | 4 |
| Historial de Notas | Media | 4 |
| **Subtotal EQ** | 9 elementos | **36** |

#### Resumen UFP y Distribución

| Categoría | Elementos | UFP | % |
|-----------|:---------:|:---:|:-:|
| ILF | 12 | 119 | 48% |
| EI | 15 | 66 | 27% |
| EQ | 9 | 36 | 15% |
| EO | 4 | 26 | 10% |
| **Total UFP** | **40** | **247** | **100%** |

```
Distribución de 247 UFP
───────────────────────────────────────────────────────────────
ILF  ████████████████████████████████████░░░░░░  119 UFP  48%
EI   █████████████████████░░░░░░░░░░░░░░░░░░░░░   66 UFP  27%
EQ   ████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░   36 UFP  15%
EO   ████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░   26 UFP  10%
     |───────────────────────────────────────|
     0                                      130

ILF domina (48%) por la riqueza del modelo de dominio:
correlativas, inscripciones, condiciones y roles de cátedra.
```

#### Factor de Ajuste de Valor (VAF)

| # | Característica General del Sistema | Nivel (0–5) |
|---|-------------------------------------|:-----------:|
| 1 | Comunicaciones de datos | 4 |
| 2 | Procesamiento distribuido | 1 |
| 3 | Objetivos de performance | 3 |
| 4 | Configuración de uso intensivo | 2 |
| 5 | Tasa de transacciones | 2 |
| 6 | Entrada de datos en línea | 4 |
| 7 | Eficiencia del usuario final | 3 |
| 8 | Actualización en línea | 4 |
| 9 | Procesamiento interno complejo | 4 |
| 10 | Reusabilidad del código | 2 |
| 11 | Facilidad de instalación | 3 |
| 12 | Facilidad de operación | 3 |
| 13 | Múltiples sitios de instalación | 1 |
| 14 | Facilidad de cambio | 3 |
| **Σ** | | **39** |

```
VAF = 0.65 + (0.01 × 39) = 1.04
AFP = UFP × VAF = 247 × 1.04 ≈ 257 AFP  (Puntos de Función Ajustados)
```

---

### 9.3 Estimación por Líneas de Código (LOC)

#### ¿Qué es y cómo funciona?

Las LOC no se cuentan desde cero — se **derivan de los AFP** usando tablas de productividad por lenguaje (Capers Jones). La idea es: dado que ya mediste el tamaño funcional, podés convertirlo al tamaño físico esperado según el lenguaje elegido.

El rol de las LOC aquí es ser el **input de COCOMO**. No son un fin en sí mismo.

```
Paso 1 — Tomás los AFP del análisis anterior: 257 AFP

Paso 2 — Buscás el factor para tu lenguaje en las tablas Capers Jones:
         Java     =  46 LOC / AFP
         Python   =  21 LOC / AFP
         C        = 128 LOC / AFP
         SQL puro =  12 LOC / AFP

         Mismo sistema en Python tendría: 257 × 21 = 5,397 LOC
         Mismo sistema en C tendría:      257 × 128 = 32,896 LOC
         → comparar proyectos por LOC entre lenguajes es engañoso

Paso 3 — Calculás las LOC esperadas:
         257 AFP × 46 LOC/AFP = 11,822 LOC ≈ 12 KLOC

Paso 4 — Usás ese número como entrada para COCOMO Básico y COCOMO II
```

Conversión estándar Capers Jones para Java: **46 LOC / AFP**

| Módulo | AFP Estimados | LOC Estimadas |
|--------|:------------:|:-------------:|
| A: Usuarios y Seguridad | 42 AFP | ~1,930 LOC |
| B: Módulo Académico | 58 AFP | ~2,670 LOC |
| C: Inscripciones y Motor | 60 AFP | ~2,760 LOC |
| D: Docentes y Calificaciones | 62 AFP | ~2,850 LOC |
| E: Seguimiento / Valor Agregado | 35 AFP | ~1,610 LOC |
| **Total** | **257 AFP** | **~11,820 LOC ≈ 12 KLOC** |

```
LOC por Módulo (12 KLOC total)
───────────────────────────────────────────────────────────────
A  Usuarios/Seg.   ██████░░░░░░░░░░░░░░░░░░░░░░░  1,930 LOC  16%
B  Módulo Acad.    ████████░░░░░░░░░░░░░░░░░░░░░  2,670 LOC  23%
C  Motor Inscr.    █████████░░░░░░░░░░░░░░░░░░░░  2,760 LOC  23%
D  Docentes/Notas  █████████░░░░░░░░░░░░░░░░░░░░  2,850 LOC  24%
E  Seguimiento     █████░░░░░░░░░░░░░░░░░░░░░░░░  1,610 LOC  14%
                   |────────────────────────────|
                   0                           3,000 LOC
```

---

### 9.4 Puntos de Caso de Uso (UCP)

#### ¿Qué es y cómo funciona?

Creado por Gustav Karner (1993) como extensión de los Function Points para proyectos orientados a objetos con modelo de casos de uso. La idea es pesar la complejidad del sistema a través de los actores y los casos de uso, y luego ajustar por factores del equipo y del proyecto.

#### Proceso paso a paso

```
Paso 1 — Clasificás cada actor por el tipo de interfaz que usa:
         ¿Persona usando GUI web?          → Complejo  = 3 pts
         ¿Otro sistema con protocolo?      → Promedio  = 2 pts
         ¿API sin interfaz, línea cmd?     → Simple    = 1 pt
         Sumás todos → UAW

Paso 2 — Clasificás cada caso de uso por cantidad de pasos internos:
         ≤3 transacciones internas         → Simple    = 5 pts
         4 a 7 transacciones               → Promedio  = 10 pts
         más de 7 transacciones            → Complejo  = 15 pts
         Sumás todos → UUCW

Paso 3 — UUCP = UAW + UUCW  (puntos sin ajustar)

Paso 4 — Calculás el TCF (Factor Técnico):
         Respondés 13 preguntas técnicas (¿tiene seguridad especial?
         ¿requiere performance crítica? ¿es portable? etc.)
         Cada una: 0 a 5. Aplicás: TCF = 0.6 + (0.01 × suma)

Paso 5 — Calculás el ECF (Factor Ambiental):
         Respondés 8 preguntas del equipo (¿trabajan part-time?
         ¿tienen experiencia? ¿están motivados? ¿los reqs son estables?)
         Factores negativos (part-time, lenguaje difícil) aumentan el costo.
         Factores positivos (motivación, estabilidad) lo reducen.
         ECF = 1.4 + (−0.03 × suma)

Paso 6 — UCP = UUCP × TCF × ECF

Paso 7 — Multiplicás por la productividad estándar:
         Esfuerzo = UCP × 20 horas/UCP
```

#### Peso de Actores No Ajustado (UAW)

| Actor | Tipo | Peso | Justificación |
|-------|------|:----:|---------------|
| Administrador | Complejo | 3 | Acceso total; gestiona usuarios, carreras, correlativas, condiciones |
| Estudiante | Complejo | 3 | Inscripciones, historial, dashboard, certificados |
| Docente | Promedio | 2 | Carga notas, consulta listas, cierre condición |
| **UAW** | | **8** | |

#### Peso de Casos de Uso No Ajustado (UUCW)

| ID | Caso de Uso | Transacciones | Tipo | Peso |
|----|-------------|:-------------:|------|:----:|
| UC01 | Login / Logout | 3 | Simple | 5 |
| UC02 | Gestionar Estudiante (CRUD) | 5 | Promedio | 10 |
| UC03 | Gestionar Profesor (CRUD) | 5 | Promedio | 10 |
| UC04 | Gestionar Carreras y Planes | 4 | Promedio | 10 |
| UC05 | Definir Oferta Académica (materias) | 6 | Promedio | 10 |
| UC06 | Configurar Grafo de Correlativas | 8 | Complejo | 15 |
| UC07 | Inscribirse a Cursada | 7 | Complejo | 15 |
| UC08 | Inscribirse a Examen Final | 7 | Complejo | 15 |
| UC09 | Asignar Roles de Cátedra | 4 | Promedio | 10 |
| UC10 | Registrar Calificaciones | 5 | Promedio | 10 |
| UC11 | Cerrar Condición Académica | 6 | Complejo | 15 |
| UC12 | Consultar Dashboard de Progreso | 3 | Simple | 5 |
| UC13 | Exportar Documentos PDF | 5 | Promedio | 10 |
| **UUCW** | | | | **145** |

```
UUCP = UAW + UUCW = 8 + 145 = 153
```

#### Factores Técnicos (TCF)

| Factor | Descripción | Peso | Valor | Resultado |
|--------|-------------|:----:|:-----:|:---------:|
| T1 | Sistema distribuido | 2 | 0 | 0.0 |
| T2 | Objetivos de rendimiento | 1 | 2 | 2.0 |
| T3 | Eficiencia usuario final | 1 | 3 | 3.0 |
| T4 | Procesamiento interno complejo | 1 | 3 | 3.0 |
| T5 | Código reusable | 1 | 2 | 2.0 |
| T6 | Facilidad de instalación | 0.5 | 3 | 1.5 |
| T7 | Facilidad de uso | 0.5 | 3 | 1.5 |
| T8 | Portabilidad | 2 | 1 | 2.0 |
| T9 | Facilidad de cambio | 1 | 2 | 2.0 |
| T10 | Concurrencia | 1 | 1 | 1.0 |
| T11 | Seguridad especial | 1 | 4 | 4.0 |
| T12 | Acceso directo de terceros | 1 | 0 | 0.0 |
| T13 | Facilidades de capacitación | 1 | 1 | 1.0 |
| **Σ TF** | | | | **23** |

```
TCF = 0.6 + (0.01 × 23) = 0.83
```

#### Factores Ambientales (ECF)

| Factor | Descripción | Peso | Valor | Resultado |
|--------|-------------|:----:|:-----:|:---------:|
| E1 | Familiaridad con el proceso | 1.5 | 3 | 4.5 |
| E2 | Trabajo a tiempo parcial | -1 | 2 | -2.0 |
| E3 | Capacidad de análisis del equipo | 0.5 | 3 | 1.5 |
| E4 | Experiencia previa en la aplicación | 0.5 | 2 | 1.0 |
| E5 | Experiencia en programación OO | 1 | 3 | 3.0 |
| E6 | Motivación del equipo | 1 | 4 | 4.0 |
| E7 | Lenguaje de programación difícil | -1 | 2 | -2.0 |
| E8 | Estabilidad de los requerimientos | 2 | 4 | 8.0 |
| **Σ EF** | | | | **18** |

```
ECF = 1.4 + (−0.03 × 18) = 0.86

UCP    = UUCP × TCF × ECF = 153 × 0.83 × 0.86 ≈ 109 UCP
Esfuerzo = 109 × 20 h/UCP = 2,180 horas ≈ 13.6 PM
Con 6 desarrolladores (160 h/mes): 13.6 PM ÷ 6 ≈ 2.3 meses calendario
```

---

### 9.5 Modelo COCOMO Básico

#### ¿Qué es y cómo funciona?

COCOMO (Boehm, 1981) es un modelo **algorítmico**: dado el tamaño en KLOC, produce esfuerzo, duración y personal usando fórmulas calibradas con datos de 63 proyectos reales.

Define tres modos según el contexto del proyecto:

| Modo | Cuándo usarlo | Coef. a | Exp. b |
|------|--------------|:-------:|:------:|
| **Orgánico** | Equipo ≤6, dominio familiar, pocas restricciones | 2.4 | 1.05 |
| Semi-detached | Equipo mixto, algo de experiencia, algunas restricciones | 3.0 | 1.12 |
| Embebido | Restricciones rígidas de hardware/tiempo real | 3.6 | 1.20 |

El exponente b > 1 significa que el esfuerzo **crece más que linealmente** con el tamaño: duplicar el código multiplica el esfuerzo por más de 2, porque la coordinación e integración se vuelven más costosas. Para este proyecto se usa modo **Orgánico** (equipo de 6, dominio académico conocido).

#### Proceso paso a paso

```
Paso 1 — Tomás las KLOC: 12 KLOC

Paso 2 — Elegís modo Orgánico (equipo chico, dominio familiar)
         Coeficientes: a=2.4, b=1.05, c=2.5, d=0.38

Paso 3 — Calculás esfuerzo:
         E = a × KLOC^b = 2.4 × (12)^1.05
           = 2.4 × 13.59
           = 32.6 persona-meses

Paso 4 — Calculás duración mínima:
         D = c × E^d = 2.5 × (32.6)^0.38
           = 2.5 × 3.76
           = 9.4 meses

         Nota: D es la duración MÍNIMA. Comprimirlo más aumenta el costo.

Paso 5 — Calculás personal necesario:
         P = E / D = 32.6 / 9.4 ≈ 3.5 → 4 desarrolladores

Paso 6 — Distribuís el esfuerzo por fase usando % estándar COCOMO
```

**Base:** 12 KLOC · **Modo:** Orgánico

```
Esfuerzo  E = 2.4 × (12)^1.05 = 2.4 × 13.59 = 32.6 PM
Duración  D = 2.5 × (32.6)^0.38 = 2.5 × 3.76 = 9.4 meses
Personal  P = E / D = 32.6 / 9.4 ≈ 3.5 → 4 desarrolladores
```

#### Distribución por Fase (COCOMO Básico)

```
Distribución del Esfuerzo por Fase — 32.6 PM total
──────────────────────────────────────────────────────────────────
Requerimientos  ██░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░   2.0 PM   6%
Diseño          ████████░░░░░░░░░░░░░░░░░░░░░░░░░   5.2 PM  16%
Codificación    █████████████░░░░░░░░░░░░░░░░░░░░   8.1 PM  25%
Integración     ████████████░░░░░░░░░░░░░░░░░░░░░   7.8 PM  24%
Pruebas         ████████████░░░░░░░░░░░░░░░░░░░░░   7.8 PM  24%
Gestión         ██░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░   1.6 PM   5%
                |───────────────────────────────|
                0                               9 PM
```

---

### 9.6 Modelo COCOMO II — Post-Arquitectura

#### ¿Qué es y cómo funciona?

COCOMO II (Boehm, 2000) reemplaza los tres modos fijos de COCOMO 81 por **22 parámetros calibrables** divididos en dos grupos:

**Factores de Escala (SF) — 5 factores — determinan el exponente B:**
Controlan cuánto escala el esfuerzo con el tamaño del proyecto. Un B alto (proceso inmaduro, sistema sin precedentes) encarece desproporcionadamente los proyectos grandes.

**Multiplicadores de Esfuerzo (EM) — 17 factores — ajustan el esfuerzo base:**
Cada EM > 1 aumenta el esfuerzo estimado, cada EM < 1 lo reduce.

#### Proceso paso a paso

```
Paso 1 — Respondés los 5 Factores de Escala:

  PREC: ¿el equipo hizo algo similar antes?
        No → Low = 5.07  (penaliza mucho, el equipo tiene que aprender)
        Sí → High = 1.24 (casi no penaliza)

  FLEX: ¿los requerimientos y el proceso son rígidos?
  RESL: ¿la arquitectura y los riesgos ya están resueltos?
  TEAM: ¿el equipo trabaja bien junto y está cohesionado?
  PMAT: ¿el proceso es maduro? (CI/CD, reviews, testing sistemático)

  Calculás B = 0.91 + (0.01 × suma de los 5 SF)
  → B alto = crece más rápido con el tamaño

Paso 2 — Respondés los 17 Multiplicadores de Esfuerzo:

  Producto:   RELY, DATA, CPLX, RUSE, DOCU
              (¿qué tan confiable? ¿qué tan complejo? ¿cuánta BD?)

  Plataforma: TIME, STOR, PVOL
              (¿restricciones de CPU o RAM? ¿la plataforma cambia mucho?)

  Personal:   ACAP, PCAP, PCON, APEX, PLEX, LTEX
              (¿qué tan capaces son? ¿tienen experiencia en esto?)

  Proyecto:   TOOL, SITE, SCED
              (¿usan buenas herramientas? ¿trabajan en distintos lugares?)

  Multiplicás todos los EM entre sí → Π EM

Paso 3 — Calculás esfuerzo:
  E = 2.94 × (KLOC)^B × Π EM

Paso 4 — Calculás duración mínima:
  D = 3.67 × E^0.28

Paso 5 — Personal = E / D
```

En este proyecto, **PMAT=Low** (equipo académico sin proceso maduro) y **PREC=Low** (nunca hicieron un sistema académico de este tipo) elevan B a 1.133 (vs 1.05 de COCOMO Básico), lo que explica que COCOMO II dé mayor esfuerzo.

#### Factores de Escala (SF)

| Factor | Descripción | Nivel | SF |
|--------|-------------|-------|:--:|
| PREC | Precedentedness — sistema nuevo para el equipo | Low | 5.07 |
| FLEX | Flexibilidad del desarrollo | Nominal | 3.04 |
| RESL | Resolución de arquitectura y riesgos | Nominal | 4.24 |
| TEAM | Cohesión del equipo | High | 2.19 |
| PMAT | Madurez del proceso (equipo académico) | Low | 7.80 |
| **Σ SF** | | | **22.34** |

```
B = 0.91 + (0.01 × 22.34) = 1.133
```

#### Multiplicadores de Esfuerzo (EM)

| Driver | Descripción | Nivel | EM |
|--------|-------------|-------|:--:|
| RELY | Confiabilidad requerida | High | 1.10 |
| DATA | Tamaño de base de datos | Nominal | 1.00 |
| CPLX | Complejidad del producto | High | 1.17 |
| RUSE | Reusabilidad requerida | Nominal | 1.00 |
| DOCU | Adecuación de documentación | Nominal | 1.00 |
| TIME | Restricción de tiempo de ejecución | Nominal | 1.00 |
| STOR | Restricción de almacenamiento | Nominal | 1.00 |
| PVOL | Volatilidad de la plataforma | Low | 0.87 |
| ACAP | Capacidad de los analistas | Nominal | 1.00 |
| PCAP | Capacidad de los programadores | Nominal | 1.00 |
| PCON | Continuidad del personal | High | 0.90 |
| APEX | Experiencia en la aplicación | Low | 1.10 |
| PLEX | Experiencia en la plataforma | Low | 1.09 |
| LTEX | Experiencia con lenguaje/herramientas | Nominal | 1.00 |
| TOOL | Uso de herramientas SW (GitHub, IDE) | High | 0.90 |
| SITE | Desarrollo en múltiples sitios | Nominal | 1.00 |
| SCED | Calendario requerido | Nominal | 1.00 |
| **Π EM** | | | **≈ 1.087** |

```
Esfuerzo  E = 2.94 × (12)^1.133 × 1.087 = 2.94 × 16.71 × 1.087 = 53.4 PM
Duración  D = 3.67 × (53.4)^0.28         = 3.67 × 3.046          = 11.2 meses
Personal  P = E / D                       = 53.4 / 11.2           ≈ 5 desarrolladores
```

#### Distribución por Fase — COCOMO II (RUP)

```
Distribución del Esfuerzo por Fase — 53.4 PM total
──────────────────────────────────────────────────────────────────
Inception      ████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░   5.3 PM  10%
Elaboration    ████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░  10.7 PM  20%
Construction   █████████████████████████████░░░░░░░  29.4 PM  55%
Transition     ████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░   8.0 PM  15%
               |──────────────────────────────────|
               0                                 30 PM
```

---

### 9.7 Esfuerzo por Ítem de Backlog

Distribución proporcional a Story Points sobre el total COCOMO II.
**Ratio:** 53.4 PM / 89 SP → **96 horas / SP** (incluye todas las fases del SDLC)

Las horas representan esfuerzo total (diseño + codificación + pruebas + documentación + gestión). El tiempo puro de codificación es aproximadamente el 25% de ese total.

| ID | Ítem de Backlog | SP | Horas (SDLC) | Persona-Meses |
|----|----------------|:--:|:------------:|:-------------:|
| A1 | Autenticación y Perfiles | 5 | 480 h | 3.0 PM |
| A2 | CRUD Legajos + Validaciones | 8 | 768 h | 4.8 PM |
| A3 | Gestión de Sesiones | 3 | 288 h | 1.8 PM |
| B1 | Oferta Académica (carreras, planes) | 8 | 768 h | 4.8 PM |
| B2 | Grafo de Correlatividades | 13 | 1,248 h | 7.8 PM |
| C1 | Inscripción Cursadas + Motor | 13 | 1,248 h | 7.8 PM |
| C2 | Inscripción Exámenes Finales | 8 | 768 h | 4.8 PM |
| D1 | Roles de Cátedra | 5 | 480 h | 3.0 PM |
| D2 | Carga de Notas y Actas | 8 | 768 h | 4.8 PM |
| D3 | Cierre de Condición | 8 | 768 h | 4.8 PM |
| E1 | Dashboard de Progreso | 5 | 480 h | 3.0 PM |
| E2 | Exportación PDF | 5 | 480 h | 3.0 PM |
| | **TOTAL** | **89** | **8,544 h** | **53.4 PM** |

#### Gráfico: Esfuerzo por Ítem (horas-persona totales)

```
Esfuerzo por Ítem de Backlog — horas-persona (SDLC completo)
──────────────────────────────────────────────────────────────────────
A1  Autenticación       ████████████░░░░░░░░░░░░░░░░░░░░░░   480 h
A2  CRUD Legajos        ████████████████████░░░░░░░░░░░░░░   768 h
A3  Sesiones            ███████░░░░░░░░░░░░░░░░░░░░░░░░░░░   288 h
B1  Oferta Académica    ████████████████████░░░░░░░░░░░░░░   768 h
B2  Correlativas        ████████████████████████████████░░  1,248 h  ◄ mayor
C1  Insc. Cursadas      ████████████████████████████████░░  1,248 h  ◄ mayor
C2  Insc. Finales       ████████████████████░░░░░░░░░░░░░░   768 h
D1  Roles Cátedra       ████████████░░░░░░░░░░░░░░░░░░░░░░   480 h
D2  Carga de Notas      ████████████████████░░░░░░░░░░░░░░   768 h
D3  Cierre Condición    ████████████████████░░░░░░░░░░░░░░   768 h
E1  Dashboard           ████████████░░░░░░░░░░░░░░░░░░░░░░   480 h
E2  Export PDF          ████████████░░░░░░░░░░░░░░░░░░░░░░   480 h
                        |──────────────────────────────────|
                        0                               1,300 h
```

---

### 9.8 Cuadro Comparativo Final

| Método | Unidad Base | Esfuerzo Total | Duración | Equipo |
|--------|-------------|:--------------:|:--------:|:------:|
| Story Points | 89 SP | — | ~12 semanas (impl.) | 6 dev |
| FPA directo (10 h/AFP) | 257 AFP | ~2,570 h / **16.1 PM** | ~4.3 meses | 4 dev |
| UCP (20 h/UCP) | 109 UCP | 2,180 h / **13.6 PM** | ~2.3 meses | 6 dev |
| COCOMO Básico | 12 KLOC | **32.6 PM** | 9.4 meses | 4 dev |
| COCOMO II | 12 KLOC | **53.4 PM** | 11.2 meses | 5 dev |

#### Gráfico: Comparativa de Esfuerzo

```
Esfuerzo estimado (Persona-Meses)
──────────────────────────────────────────────────────────────────────
UCP           ████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  13.6 PM
FPA Directo   ████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  16.1 PM
COCOMO Básico ████████████████████░░░░░░░░░░░░░░░░░░  32.6 PM
COCOMO II     ██████████████████████████████████████  53.4 PM
              |──────────────────────────────────────|
              0              27                      55 PM

              ◄── Optimista ──────────── Conservador ──►
```

```
Duración estimada (meses calendario)
──────────────────────────────────────────────────────────────────────
UCP (6 devs)    ███░░░░░░░░░░░░░░░░░░░░░░░░░░░░   2.3 m
Story Points    ███████░░░░░░░░░░░░░░░░░░░░░░░░   3.0 m  (6 sprints)
FPA (6 devs)    ████████░░░░░░░░░░░░░░░░░░░░░░░   4.3 m
COCOMO Básico   ████████████████████████░░░░░░░   9.4 m
COCOMO II       ██████████████████████████████   11.2 m
                |─────────────────────────────|
                0                            12 meses
```

#### Análisis e Interpretación

```
┌──────────────────────────────────────────────────────────────────────────┐
│                        INTERPRETACIÓN DE RESULTADOS                      │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  UCP / FPA convergen en 13-16 PM  →  estimación optimista.              │
│  Suponen dedicación full-time y equipo experimentado.                    │
│  Subestiman overhead de aprendizaje y coordinación.                      │
│                                                                          │
│  COCOMO Básico (32.6 PM)  →  estimación media.                          │
│  Captura overhead real del SDLC pero no penaliza inexperiencia.          │
│                                                                          │
│  COCOMO II (53.4 PM)  →  límite superior / presupuesto de holgura.      │
│  Penaliza: PMAT=Low (proceso académico), PREC=Low (sistema nuevo),       │
│  APEX=Low y PLEX=Low (poca experiencia previa en el dominio).            │
│                                                                          │
│  RANGO RECOMENDADO PARA ESTE PROYECTO:                                   │
│  ┌──────────────────────────────────────────────────────────────┐       │
│  │  20 PM (optimista) ──── 35 PM (probable) ──── 53 PM (máximo) │       │
│  └──────────────────────────────────────────────────────────────┘       │
│                                                                          │
│  Con 6 integrantes y dedicación parcial (cursada universitaria):         │
│  Duración real estimada: 5 a 7 meses calendario.                         │
│                                                                          │
│  USO RECOMENDADO POR MÉTODO:                                             │
│  - Story Points  →  planificación sprint a sprint, velocidad del equipo  │
│  - UCP           →  estimación inicial rápida en fase de inception       │
│  - COCOMO II     →  presupuesto de holgura, gestión de riesgos           │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```


