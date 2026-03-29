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

---

## 6. Seguimiento y Organización

### Plazo estimado
### Cambios de alcance ocurridos
### Problemas encontrados
### Forma de organización del equipo