# Especificación Funcional — Historia B.2
## Gestión de Materias y Asociación a Planes de Estudio

---

| Campo             | Valor                                                    |
|-------------------|----------------------------------------------------------|
| Identificador     | HU-B.2                                                   |
| Módulo            | B — Módulo Académico                                     |
| Fecha             | 2026-05-05                                               |
| Estado            | Listo para implementar                                   |
| Story Points est. | 13                                                       |
| Dependencias      | HU-B.1 (Carreras y Planes de Estudio), Módulo A (Roles) |
| Autor             | Analista funcional — ProyectoIntegrador UNRC             |

---

## 1. Historia de Usuario

**Como** administrador del sistema académico,
**quiero** crear materias únicas en el sistema y poder asociarlas a uno o más planes de estudio de distintas carreras,
**para** reutilizar el contenido académico definido una sola vez y configurar sus particularidades (carga horaria, carácter, cuatrimestre) dentro de cada plan sin duplicar información.

---

## 2. Valor de Negocio

Actualmente, la información de una materia que se dicta en varios planes se registra de forma redundante y desincronizada, generando inconsistencias. Esta historia permite que una materia exista como entidad global única y que sus particularidades se definan en el contexto de cada plan, reduciendo errores de mantenimiento y preparando la base para la gestión de correlatividades (HU-B.3) e inscripciones (Módulo C).

---

## 3. Entidades y Atributos

### 3.1. Entidad: Materia

Representa el contenido académico de forma global e independiente de cualquier carrera o plan.

| Atributo      | Tipo          | Requerido | Restricciones                                                     |
|---------------|---------------|-----------|-------------------------------------------------------------------|
| id            | UUID          | Si        | Generado por el sistema. Inmutable.                               |
| codigo        | VARCHAR(20)   | Si        | Unico global en el sistema. Inmutable una vez persistido.        |
| nombre        | VARCHAR(200)  | Si        | Unico global en el sistema. No vacio.                            |
| descripcion   | TEXT          | No        | Longitud maxima 1000 caracteres.                                  |
| estado        | ENUM          | Si        | Valores: ACTIVA, INACTIVA. Default: ACTIVA.                      |
| created_at    | TIMESTAMP     | Si        | Generado por el sistema al momento de creacion.                   |
| updated_at    | TIMESTAMP     | Si        | Actualizado automaticamente en cada modificacion.                 |
| created_by    | UUID (FK)     | Si        | Referencia al Usuario administrador que realizo la creacion.      |

**Indices:** codigo (UNIQUE), nombre (UNIQUE).

---

### 3.2. Entidad: MateriaPlan (tabla de asociacion)

Representa la relacion entre una Materia global y un Plan de Estudio especifico, con las particularidades propias de esa combinacion.

| Atributo          | Tipo        | Requerido | Restricciones                                                                    |
|-------------------|-------------|-----------|----------------------------------------------------------------------------------|
| id                | UUID        | Si        | Generado por el sistema. Inmutable.                                              |
| materia_id        | UUID (FK)   | Si        | Referencia a Materia. No nulo.                                                   |
| plan_estudio_id   | UUID (FK)   | Si        | Referencia a PlanDeEstudio. No nulo.                                             |
| anio_plan         | INTEGER     | Si        | Año del plan en que se cursa la materia. Minimo 1.                              |
| cuatrimestre      | ENUM        | Si        | Valores: PRIMERO, SEGUNDO, ANUAL.                                                |
| carga_horaria     | INTEGER     | Si        | Horas semanales. Debe ser mayor a 0.                                             |
| caracter          | ENUM        | Si        | Valores: OBLIGATORIA, ELECTIVA.                                                  |
| estado            | ENUM        | Si        | Valores: ACTIVA, INACTIVA. Default: ACTIVA.                                     |
| created_at        | TIMESTAMP   | Si        | Generado por el sistema al momento de creacion.                                  |
| updated_at        | TIMESTAMP   | Si        | Actualizado automaticamente en cada modificacion.                                |
| created_by        | UUID (FK)   | Si        | Referencia al Usuario administrador que realizo la creacion de la asociacion.    |

**Restriccion de unicidad:** La combinacion (materia_id, plan_estudio_id) debe ser unica. No puede existir mas de un registro activo o inactivo para el mismo par.

---

### 3.3. Extension: AuditLog

Cada operacion sobre Materia y MateriaPlan genera un registro en la tabla audit_log existente con la siguiente informacion:

| Campo       | Valor registrado                                                              |
|-------------|-------------------------------------------------------------------------------|
| entidad     | "Materia" o "MateriaPlan"                                                     |
| entidad_id  | UUID del registro afectado                                                    |
| accion      | CREATE, UPDATE, DEACTIVATE, REACTIVATE, ASSOCIATE, DISASSOCIATE               |
| usuario_id  | UUID del administrador que ejecuto la accion                                  |
| timestamp   | Fecha y hora exactas de la operacion                                          |
| detalle     | JSON con los valores anteriores y nuevos del campo o campos modificados       |

---

## 4. Reglas de Negocio

**RN-01 — Unicidad global de Materia por codigo:**
El codigo de una materia debe ser unico en todo el sistema. El sistema rechazara la creacion o edicion si el codigo ya existe en otra materia, independientemente del estado de esa materia.

**RN-02 — Unicidad global de Materia por nombre:**
El nombre de una materia debe ser unico en todo el sistema. El sistema rechazara la creacion o edicion si el nombre ya existe en otra materia, independientemente del estado de esa materia.

**RN-03 — Inmutabilidad del codigo:**
Una vez que una Materia es persistida con su codigo, dicho codigo no puede ser modificado bajo ninguna circunstancia, ni siquiera por el administrador.

**RN-04 — Unicidad de asociacion Materia-Plan:**
Una misma Materia no puede asociarse mas de una vez al mismo Plan de Estudio. Si ya existe un registro MateriaPlan para ese par (activo o inactivo), el sistema rechazara una nueva asociacion.

**RN-05 — Particularidades independientes por plan:**
Una misma Materia puede tener distintas particularidades (carga_horaria, anio_plan, cuatrimestre, caracter) en distintos planes de estudio. Modificar las particularidades en un plan no afecta las de otro plan.

**RN-06 — Baja logica de Materia con asociaciones activas:**
Si una Materia tiene al menos una asociacion activa (MateriaPlan en estado ACTIVA), el sistema no permite eliminarla fisicamente. Solo se permite la baja logica (cambio de estado a INACTIVA).

**RN-07 — Baja logica de Materia sin asociaciones:**
Si una Materia no tiene ninguna asociacion activa, puede ser desactivada logicamente. No se permite eliminacion fisica de materias en ningun caso.

**RN-08 — Restriccion para eliminar una asociacion MateriaPlan:**
Una asociacion MateriaPlan solo puede eliminarse (desactivarse) si no existe ninguna cursada activa, ni historial de cursadas registrado para esa combinacion (materia_id + plan_estudio_id). Si existe al menos un registro de cursada (pendiente, en curso o finalizada), la operacion es rechazada.

**RN-09 — Plan de Estudio como prerequisito para asociar:**
Solo pueden asociarse materias a planes de estudio que existan y se encuentren en estado ACTIVO en el sistema (segun entidad PlanDeEstudio de HU-B.1).

**RN-10 — Materia activa como prerequisito para asociar:**
Solo pueden realizarse nuevas asociaciones si la Materia se encuentra en estado ACTIVA. No se permite asociar una materia inactiva a un plan.

**RN-11 — Control de acceso por rol:**
Unicamente el rol Administrador puede crear, editar, desactivar, reactivar materias y gestionar sus asociaciones a planes. Los roles Docente y Estudiante tienen acceso de solo lectura al catalogo de materias y a sus asociaciones.

**RN-12 — Registro obligatorio en audit_log:**
Toda operacion de escritura sobre las entidades Materia y MateriaPlan (creacion, edicion, desactivacion, reactivacion, asociacion, eliminacion de asociacion) debe generar un registro en audit_log. La ausencia de este registro invalida la operacion.

**RN-13 — Reactivacion de Materia:**
Una Materia en estado INACTIVA puede ser reactivada por el administrador, retomando el estado ACTIVA. Las asociaciones previas que fueron desactivadas no se reactivan automaticamente; deben gestionarse de forma independiente.

---

## 5. Flujos Principales y Alternativos

### Flujo 5.1 — Crear Materia

**Actores:** Administrador
**Precondicion:** El usuario esta autenticado con rol Administrador.

1. El administrador accede a la seccion "Materias" del modulo academico.
2. El administrador selecciona la accion "Nueva Materia".
3. El sistema presenta el formulario de creacion con los campos: Codigo, Nombre, Descripcion (opcional).
4. El administrador completa los campos requeridos y confirma.
5. El sistema valida que el codigo no exista en ninguna materia (activa o inactiva) — RN-01.
6. El sistema valida que el nombre no exista en ninguna materia (activa o inactiva) — RN-02.
7. El sistema persiste la Materia en estado ACTIVA con los datos ingresados.
8. El sistema registra la operacion en audit_log (accion: CREATE) — RN-12.
9. El sistema muestra un mensaje de exito e incluye la nueva materia en el listado.

**Flujos alternativos:**

- **5.1.A — Codigo ya existente (paso 5):** El sistema muestra un mensaje de error indicando que el codigo ya esta en uso. El formulario permanece abierto con los datos ingresados. La materia no es creada.
- **5.1.B — Nombre ya existente (paso 6):** El sistema muestra un mensaje de error indicando que el nombre ya esta en uso. El formulario permanece abierto. La materia no es creada.
- **5.1.C — Cancelar creacion:** El administrador cancela. El sistema descarta los datos y no genera ningun registro en audit_log.

---

### Flujo 5.2 — Editar Materia

**Actores:** Administrador
**Precondicion:** El usuario esta autenticado con rol Administrador. Existe al menos una materia en el sistema.

1. El administrador accede al listado de materias y selecciona una materia existente.
2. El administrador selecciona la accion "Editar".
3. El sistema presenta el formulario de edicion con los campos editables: Nombre, Descripcion. El campo Codigo se muestra como solo lectura — RN-03.
4. El administrador modifica los campos deseados y confirma.
5. El sistema valida que el nuevo nombre no exista en ninguna otra materia — RN-02.
6. El sistema persiste los cambios y actualiza el campo updated_at.
7. El sistema registra la operacion en audit_log (accion: UPDATE) con los valores anteriores y nuevos — RN-12.
8. El sistema muestra un mensaje de exito.

**Flujos alternativos:**

- **5.2.A — Nombre ya existente en otra materia (paso 5):** El sistema muestra un mensaje de error. El formulario permanece abierto. Los cambios no son persistidos.
- **5.2.B — Sin modificaciones:** El administrador confirma sin cambiar ningun campo. El sistema no genera persistencia ni registro en audit_log, y muestra un mensaje informativo.

---

### Flujo 5.3 — Desactivar Materia (baja logica)

**Actores:** Administrador
**Precondicion:** El usuario esta autenticado con rol Administrador. La materia seleccionada esta en estado ACTIVA.

1. El administrador selecciona una materia del listado y elige la accion "Desactivar".
2. El sistema muestra un dialogo de confirmacion indicando las consecuencias de la accion.
3. El administrador confirma la desactivacion.
4. El sistema verifica si la materia tiene asociaciones activas (MateriaPlan en estado ACTIVA) — RN-06.
5. El sistema cambia el estado de la materia a INACTIVA.
6. El sistema registra la operacion en audit_log (accion: DEACTIVATE) — RN-12.
7. El sistema muestra un mensaje de exito.

**Flujos alternativos:**

- **5.3.A — Materia con asociaciones activas (paso 4):** La desactivacion continua igualmente (baja logica), pero el sistema muestra una advertencia indicando que la materia tiene asociaciones activas vigentes que deben revisarse.
- **5.3.B — Administrador cancela:** El sistema cierra el dialogo. El estado de la materia no cambia. No se registra nada en audit_log.

---

### Flujo 5.4 — Reactivar Materia

**Actores:** Administrador
**Precondicion:** El usuario esta autenticado con rol Administrador. La materia seleccionada esta en estado INACTIVA.

1. El administrador accede al listado (incluyendo inactivas) y selecciona una materia en estado INACTIVA.
2. El administrador selecciona la accion "Reactivar".
3. El sistema muestra un dialogo de confirmacion.
4. El administrador confirma.
5. El sistema cambia el estado de la materia a ACTIVA.
6. El sistema registra la operacion en audit_log (accion: REACTIVATE) — RN-12.
7. El sistema muestra un mensaje de exito. Las asociaciones previas desactivadas no se reactivan automaticamente — RN-13.

---

### Flujo 5.5 — Asociar Materia a Plan de Estudio

**Actores:** Administrador
**Precondicion:** El usuario esta autenticado con rol Administrador. Existe al menos una Materia en estado ACTIVA y al menos un PlanDeEstudio en estado ACTIVO.

1. El administrador accede al detalle de una materia o de un plan y selecciona "Asociar a Plan" o "Agregar Materia".
2. El sistema presenta el formulario de asociacion: Plan de Estudio (selector de planes activos), Año del plan, Cuatrimestre, Carga horaria semanal, Caracter (Obligatoria / Electiva).
3. El administrador completa todos los campos y confirma.
4. El sistema valida que la Materia este en estado ACTIVA — RN-10.
5. El sistema valida que el Plan de Estudio seleccionado este en estado ACTIVO — RN-09.
6. El sistema valida que no exista ya un registro MateriaPlan para el par (materia_id, plan_estudio_id) — RN-04.
7. El sistema persiste el registro MateriaPlan en estado ACTIVA.
8. El sistema registra la operacion en audit_log (accion: ASSOCIATE) — RN-12.
9. El sistema muestra un mensaje de exito y actualiza la vista de asociaciones.

**Flujos alternativos:**

- **5.5.A — Materia inactiva (paso 4):** El sistema rechaza la operacion indicando que la materia debe estar activa.
- **5.5.B — Plan de Estudio inactivo (paso 5):** El sistema rechaza la operacion indicando que el plan no esta activo.
- **5.5.C — Asociacion ya existente (paso 6):** El sistema rechaza la operacion indicando que esa materia ya esta asociada a ese plan.
- **5.5.D — Cancelar asociacion:** El administrador cancela. El sistema descarta los datos.

---

### Flujo 5.6 — Editar particularidades de una asociacion MateriaPlan

**Actores:** Administrador
**Precondicion:** El usuario esta autenticado con rol Administrador. Existe al menos una asociacion MateriaPlan en estado ACTIVA.

1. El administrador accede al listado de asociaciones y selecciona una asociacion existente.
2. El administrador selecciona la accion "Editar asociacion".
3. El sistema presenta el formulario con los campos editables: Año del plan, Cuatrimestre, Carga horaria, Caracter.
4. El administrador modifica los campos deseados y confirma.
5. El sistema valida que los valores sean correctos (carga horaria mayor a 0, año mayor a 0).
6. El sistema persiste los cambios y actualiza el campo updated_at del registro MateriaPlan.
7. El sistema registra la operacion en audit_log (accion: UPDATE) con los valores anteriores y nuevos — RN-12.
8. El sistema muestra un mensaje de exito.

---

### Flujo 5.7 — Eliminar (desactivar) una asociacion MateriaPlan

**Actores:** Administrador
**Precondicion:** El usuario esta autenticado con rol Administrador. Existe una asociacion MateriaPlan.

1. El administrador accede al listado de asociaciones y selecciona una asociacion existente.
2. El administrador selecciona la accion "Eliminar asociacion".
3. El sistema muestra un dialogo de confirmacion advirtiendo que la accion no se puede realizar si existen cursadas registradas.
4. El administrador confirma.
5. El sistema verifica que no existan cursadas activas ni historial para esa combinacion (materia_id + plan_estudio_id) — RN-08.
6. El sistema cambia el estado del registro MateriaPlan a INACTIVA.
7. El sistema registra la operacion en audit_log (accion: DISASSOCIATE) — RN-12.
8. El sistema muestra un mensaje de exito.

**Flujos alternativos:**

- **5.7.A — Existen cursadas (paso 5):** El sistema rechaza la operacion indicando que no es posible eliminar la asociacion porque existen cursadas registradas.
- **5.7.B — Administrador cancela:** El sistema cierra el dialogo. La asociacion no cambia.

---

### Flujo 5.8 — Consultar catalogo de materias (todos los roles)

**Actores:** Administrador, Docente, Estudiante
**Precondicion:** El usuario esta autenticado con cualquier rol valido.

1. El usuario accede a la seccion "Materias".
2. El sistema muestra el listado paginado de materias activas con: Codigo, Nombre, cantidad de planes asociados.
3. El usuario puede filtrar por: Carrera, Plan de Estudio.
4. El usuario puede seleccionar una materia para ver su detalle, incluyendo descripcion y el listado de planes asociados con sus particularidades.
5. Docente y Estudiante no visualizan las acciones de creacion, edicion, desactivacion ni gestion de asociaciones — RN-11.
6. El Administrador visualiza el listado completo (activas e inactivas) con opcion de filtrar por estado.

---

## 6. Criterios de Aceptacion

**CA-01 — Creacion de materia con datos validos**
- **Dado** que el administrador esta autenticado y accede al formulario de nueva materia,
- **Cuando** ingresa un codigo unico, un nombre unico y confirma la creacion,
- **Entonces** el sistema persiste la materia en estado ACTIVA, la incluye en el listado y registra la accion CREATE en audit_log.

**CA-02 — Rechazo de creacion por codigo duplicado**
- **Dado** que ya existe una materia con el codigo "ANA-I" (activa o inactiva),
- **Cuando** el administrador intenta crear una nueva materia con el codigo "ANA-I",
- **Entonces** el sistema rechaza la operacion, muestra un mensaje de error indicando que el codigo ya esta en uso, y no persiste ningun registro nuevo.

**CA-03 — Rechazo de creacion por nombre duplicado**
- **Dado** que ya existe una materia con el nombre "Analisis Matematico I" (activa o inactiva),
- **Cuando** el administrador intenta crear una nueva materia con ese mismo nombre,
- **Entonces** el sistema rechaza la operacion, muestra un mensaje de error indicando que el nombre ya existe, y no persiste ningun registro nuevo.

**CA-04 — Inmutabilidad del codigo de materia**
- **Dado** que el administrador accede al formulario de edicion de una materia existente,
- **Cuando** visualiza el formulario,
- **Entonces** el campo Codigo se muestra como solo lectura y no puede ser modificado.

**CA-05 — Edicion del nombre de materia**
- **Dado** que el administrador edita una materia y cambia su nombre a uno que no existe en el sistema,
- **Cuando** confirma la edicion,
- **Entonces** el sistema persiste el nuevo nombre, actualiza updated_at y registra la accion UPDATE en audit_log con los valores anterior y nuevo del campo nombre.

**CA-06 — Desactivacion logica de materia**
- **Dado** que el administrador selecciona una materia en estado ACTIVA y confirma la desactivacion,
- **Cuando** la operacion es confirmada,
- **Entonces** el sistema cambia el estado de la materia a INACTIVA, registra DEACTIVATE en audit_log, y la materia ya no aparece disponible para nuevas asociaciones.

**CA-07 — Desactivacion con advertencia por asociaciones activas**
- **Dado** que una materia tiene al menos una asociacion MateriaPlan en estado ACTIVA,
- **Cuando** el administrador la desactiva,
- **Entonces** el sistema la desactiva igualmente pero muestra una advertencia indicando que existen asociaciones activas vigentes que deben revisarse.

**CA-08 — Reactivacion de materia inactiva**
- **Dado** que el administrador selecciona una materia en estado INACTIVA y confirma la reactivacion,
- **Cuando** la operacion es confirmada,
- **Entonces** el sistema cambia el estado a ACTIVA, registra REACTIVATE en audit_log, y las asociaciones previamente desactivadas permanecen en su estado INACTIVA.

**CA-09 — Asociacion de materia activa a plan activo**
- **Dado** que el administrador selecciona una materia en estado ACTIVA y un plan de estudio en estado ACTIVO,
- **Cuando** completa los campos de particularidades y confirma,
- **Entonces** el sistema persiste el registro MateriaPlan en estado ACTIVA y registra la accion ASSOCIATE en audit_log.

**CA-10 — Rechazo de asociacion duplicada**
- **Dado** que ya existe un registro MateriaPlan (activo o inactivo) para el par (materia "ANA-I", plan "Plan 2023"),
- **Cuando** el administrador intenta crear una nueva asociacion para ese mismo par,
- **Entonces** el sistema rechaza la operacion y muestra un mensaje indicando que esa asociacion ya existe.

**CA-11 — Rechazo de asociacion de materia inactiva**
- **Dado** que una materia se encuentra en estado INACTIVA,
- **Cuando** el administrador intenta asociarla a un plan de estudio,
- **Entonces** el sistema rechaza la operacion y muestra un mensaje indicando que la materia debe estar activa.

**CA-12 — Particularidades distintas en distintos planes**
- **Dado** que la materia "ANA-I" esta asociada al "Plan 2018" con carga horaria de 6 horas,
- **Cuando** el administrador la asocia al "Plan 2023" con carga horaria de 4 horas,
- **Entonces** el sistema persiste ambas asociaciones con sus respectivas cargas horarias de forma independiente, sin modificar la asociacion preexistente.

**CA-13 — Eliminacion de asociacion sin cursadas**
- **Dado** que no existen cursadas activas ni historial para la combinacion (materia_id + plan_estudio_id),
- **Cuando** el administrador confirma la eliminacion de esa asociacion MateriaPlan,
- **Entonces** el sistema cambia el estado del registro a INACTIVA y registra la accion DISASSOCIATE en audit_log.

**CA-14 — Rechazo de eliminacion de asociacion con cursadas**
- **Dado** que existen cursadas (activas o historicas) para la combinacion (materia_id + plan_estudio_id),
- **Cuando** el administrador intenta eliminar esa asociacion MateriaPlan,
- **Entonces** el sistema rechaza la operacion y muestra un mensaje informando que existen cursadas registradas para esa combinacion.

**CA-15 — Acceso de solo lectura para Docente y Estudiante**
- **Dado** que un usuario con rol Docente o Estudiante esta autenticado y accede al catalogo de materias,
- **Cuando** visualiza el listado o el detalle de una materia,
- **Entonces** no se muestran acciones de creacion, edicion, desactivacion ni gestion de asociaciones.

**CA-16 — Filtrado del listado de materias**
- **Dado** que el usuario accede al listado de materias,
- **Cuando** aplica un filtro por carrera o por plan de estudio,
- **Entonces** el sistema muestra unicamente las materias asociadas a la carrera o plan seleccionado, de forma paginada.

**CA-17 — Registro en audit_log de toda operacion de escritura**
- **Dado** que el administrador ejecuta cualquier operacion de escritura (CREATE, UPDATE, DEACTIVATE, REACTIVATE, ASSOCIATE, DISASSOCIATE) sobre Materia o MateriaPlan,
- **Cuando** la operacion es exitosa,
- **Entonces** existe un registro en audit_log con entidad, entidad_id, accion, usuario_id, timestamp y detalle JSON con valores anteriores y nuevos.

---

## 7. Casos Borde

**CB-01 — Materia sin descripcion:**
Una materia puede crearse y editarse sin ingresar descripcion. El sistema debe persistirla con descripcion nula o vacia sin error.

**CB-02 — Codigo con caracteres especiales o espacios:**
El sistema debe definir el conjunto de caracteres validos para el codigo (alfanumericos y guion medio, sin espacios). Cualquier codigo fuera de ese conjunto debe ser rechazado con un mensaje descriptivo.

**CB-03 — Nombre con variaciones de mayusculas/minusculas:**
El sistema debe tratar "Analisis Matematico I" y "analisis matematico i" como equivalentes a efectos de la validacion de unicidad. La comparacion de nombres debe ser insensible a mayusculas.

**CB-04 — Plan de estudio desactivado luego de crear la asociacion:**
Si un PlanDeEstudio es desactivado despues de que ya existe una asociacion MateriaPlan activa, la asociacion permanece activa. La desactivacion del plan no propaga automaticamente la desactivacion de sus asociaciones de materias.

**CB-05 — Materia con una sola asociacion activa que se intenta desactivar:**
El administrador desactiva una materia que tiene exactamente una asociacion activa. El sistema desactiva la materia y muestra la advertencia. No elimina ni desactiva la asociacion automaticamente.

**CB-06 — Intento de reactivar una materia ya activa:**
El sistema debe detectar que la materia ya esta en estado ACTIVA y mostrar un mensaje informativo. No se genera ningun cambio ni registro en audit_log.

**CB-07 — Intento de desactivar una materia ya inactiva:**
El sistema debe detectar que la materia ya esta en estado INACTIVA y mostrar un mensaje informativo. No se genera ningun cambio ni registro en audit_log.

**CB-08 — Carga horaria igual a cero:**
El sistema debe rechazar la creacion o edicion de una asociacion MateriaPlan con carga horaria igual a cero, mostrando un mensaje de validacion.

**CB-09 — Año del plan igual a cero o negativo:**
El sistema debe rechazar la creacion o edicion de una asociacion MateriaPlan con un valor de año del plan menor a 1, mostrando un mensaje de validacion.

**CB-10 — Materia asociada a un gran numero de planes:**
El sistema debe soportar que una materia este asociada a multiples planes de estudio sin limite predefinido arbitrario, listando todas sus asociaciones de forma paginada en la vista de detalle.

**CB-11 — Concurrencia: dos administradores crean la misma materia simultaneamente:**
El sistema debe garantizar la unicidad del codigo y nombre mediante restricciones a nivel de base de datos, de modo que solo una de las dos operaciones concurrentes sea exitosa y la otra reciba un error de conflicto.

---

## 8. Fuera de Alcance

Las siguientes funcionalidades no forman parte de esta historia:

- **Correlatividades entre materias:** La definicion de prerequisitos queda reservada para la historia HU-B.3.
- **Inscripcion de estudiantes a materias:** Pertenece al Modulo C.
- **Carga de contenidos o programas de materias:** Los documentos de programa analitico no son gestionados en esta historia.
- **Importacion masiva de materias:** La carga mediante archivos CSV u otros formatos no esta contemplada.
- **Asignacion de docentes a materias:** La vinculacion docente-materia se gestiona en el contexto de comisiones segun HU-D.1.
- **Reportes o exportacion del catalogo:** Pertenece al Modulo E.
- **Versionado de cambios en particularidades de MateriaPlan:** El historial queda cubierto por audit_log; no se implementa un sistema de versiones adicional.

---

## 9. Dependencias y Siguiente Historia

### Dependencias previas (deben estar implementadas):

| Historia | Descripcion                                 | Estado              |
|----------|---------------------------------------------|---------------------|
| HU-A     | Autenticacion y roles (Administrador, etc.) | Implementada        |
| HU-B.1   | Carreras y Planes de Estudio                | Especificada        |

### Historia siguiente sugerida:

| Historia | Descripcion                                              | Justificacion                                                                                     |
|----------|----------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| HU-B.3   | Definicion de correlatividades entre materias por plan   | Requiere que MateriaPlan exista para poder establecer relaciones de prerequisito entre materias.   |

---

## 10. Asunciones Validadas

| N.° | Asuncion validada                                                                                                                         |
|-----|-------------------------------------------------------------------------------------------------------------------------------------------|
| 1   | Una Materia es una entidad global y unica en el sistema. El mismo concepto academico no se duplica por carrera ni por plan.                |
| 2   | Una Materia puede asociarse a multiples Planes de Estudio de distintas carreras mediante la entidad MateriaPlan.                           |
| 3   | La asociacion Materia-PlanDeEstudio define particularidades propias: año del plan, cuatrimestre, carga horaria y caracter. Las correlatividades quedan para HU-B.3. |
| 4   | Una misma Materia puede tener distintas particularidades en distintos planes de estudio de forma completamente independiente.               |
| 5   | Una Materia se identifica por codigo unico global y nombre unico global, irrepetibles incluso entre activas e inactivas.                   |
| 6   | El codigo de una Materia es inmutable una vez que el registro es persistido en el sistema.                                                 |
| 7   | La combinacion (Materia + PlanDeEstudio) debe ser unica en MateriaPlan; no puede existir mas de un registro para el mismo par.             |
| 8   | Solo el rol Administrador puede crear, editar, desactivar, reactivar materias y gestionar asociaciones. Docente y Estudiante: solo lectura. |
| 9   | Si una Materia tiene asociaciones activas, se aplica baja logica. En ningun caso se elimina fisicamente una materia.                       |
| 10  | La desactivacion de una asociacion MateriaPlan solo es posible si no existen cursadas activas ni historial para esa combinacion.           |
| 11  | El listado de materias es paginado y permite filtrado por carrera y por plan de estudio.                                                   |
| 12  | Toda operacion de escritura sobre Materia y MateriaPlan debe quedar registrada en la tabla audit_log existente.                            |
| 13  | Una Materia inactiva puede ser reactivada; las asociaciones previamente desactivadas no se reactivan automaticamente.                      |
| 14  | Solo pueden realizarse nuevas asociaciones si tanto la Materia como el PlanDeEstudio se encuentran en estado ACTIVO.                       |
