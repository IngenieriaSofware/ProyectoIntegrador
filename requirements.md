# Proyecto Integrador: Sistema de Gestión Estudiantil (UNRC)

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

## 3. Funcionalidades Detalladas (Desglose por Módulos)

Para cumplir con los objetivos de centralización y automatización, el sistema se divide en los siguientes alcances específicos:

### A. Módulo de Gestión de Usuarios y Seguridad
* **Autenticación y Perfiles:** Sistema de login con cifrado de credenciales y redirección basada en roles (Admin, Estudiante, Docente).
* **Administración de Legajos:** CRUD completo de Estudiantes y Profesores, incluyendo validación de formatos de DNI, Email y Teléfono.
* **Gestión de Sesiones:** Control de acceso para asegurar que cada actor solo visualice y edite los datos permitidos por su rol.

### B. Módulo Académico (Configuración)
* **Gestión de Oferta Académica:** Definición de Carreras y sus respectivos Planes de Estudio por año de vigencia.
* **Grafo de Correlatividades:** Carga de requisitos de pre-requisito (materia anterior) y posterior, distinguiendo entre condición de "Regular" o "Aprobada" para habilitar la siguiente.
* **Administración de Comisiones:** Creación de espacios de cursada vinculando materias con horarios y aulas (físicas o virtuales).

### C. Módulo de Inscripciones y Motor de Reglas
* **Inscripción a Cursadas:** Validación en tiempo real del cumplimiento de correlativas regulares antes de confirmar el alta en una comisión.
* **Inscripción a Exámenes Finales:** Validación automática de la condición de "Regular" en la materia y correlativas aprobadas según el plan.
* **Gestión de Cupos:** Control de capacidad máxima por comisión para evitar la sobrepoblación de las aulas.

### D. Módulo de Gestión Docente y Calificaciones
* **Asignación de Roles de Cátedra:** Vinculación de uno o más docentes a una materia con cargos específicos (Titular, JTP, Ayudante) por período lectivo.
* **Carga de Notas y Actas:** Interfaz para que el docente registre calificaciones de parciales, trabajos prácticos y exámenes finales.
* **Cierre de Condición:** Proceso automático/manual para determinar si un alumno queda en condición de "Libre", "Regular" o "Promocionado".

### E. Módulo de Seguimiento y Valor Agregado (Propuesta del Equipo)
* **Dashboard de Progreso Estudiantil:** Visualización gráfica para el alumno sobre su avance porcentual en la carrera.
* **Sistema de Alertas Tempranas:** Notificación automática a la Oficina de Alumnos sobre estudiantes con más de dos aplazos consecutivos o inactividad prolongada.
* **Exportación Documental:** Generación de archivos PDF para listas de inscriptos, actas de examen y certificados analíticos parciales.

---

## 4. Restricciones Técnicas (Requerimientos No Funcionales)
Para asegurar la calidad del software, el sistema debe cumplir con:

* **Seguridad:** Protección de datos almacenados y control de acceso por roles para evitar manipulaciones indebidas.
* **Mantenibilidad:** Arquitectura modular y extensible que facilite la incorporación de nuevas funciones.
* **Escalabilidad:** Capacidad para soportar un mayor número de carreras, planes y alumnos sin perder rendimiento.
* **Performance:** Procesamiento rápido de inscripciones y validaciones masivas.
* **Usabilidad:** Interfaz clara e intuitiva que facilite la experiencia del usuario.

---

## 5. Información del Proyecto

### Tamaño del Equipo
El equipo está conformado por 6 integrantes: Cruseño Alvaro, Dehaes Juan, Destefanis Adrian, Dominguez Alan, Garais Santiago y Narvaja Samuel.

### Tecnologías Elegidas y Justificación
* **Java:** Lenguaje principal por su robustez y soporte de Programación Orientada a Objetos.
* **Spark Java:** Framework web ligero para la gestión de rutas y peticiones.
* **SQLite / ActiveJDBC:** Motor de base de datos relacional y ORM para facilitar la portabilidad y gestión eficiente de consultas SQL.
* **Mustache:** Sistema de plantillas para el renderizado dinámico de vistas.

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