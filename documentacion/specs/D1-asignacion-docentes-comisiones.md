# Especificación Funcional — D.1: Asignación de Docentes a Comisiones

---

## 1. Metadata

| Campo                  | Valor                                              |
|------------------------|----------------------------------------------------|
| ID de historia         | D.1                                                |
| Módulo                 | Gestión de Docentes                                |
| Fecha de especificación| 2026-05-05                                         |
| Período lectivo target | Período activo del sistema                         |
| Story points estimados | 8                                                  |
| Prioridad              | Alta                                               |
| Dependencias previas   | B.1 (Gestión de Períodos), C.1 (Gestión de Comisiones), Autenticación y Perfiles |
| Estado                 | Especificada — pendiente de desarrollo             |

---

## 2. Historia de Usuario y Objetivo

### Historia de usuario

> Como **administrador del sistema académico**, quiero **asignar docentes a materias (comisiones) indicando su cargo (Titular, JTP o Ayudante) para el período lectivo actual**, para **gestionar correctamente los permisos de edición de actas y carga de notas**.

### Objetivo / Valor de negocio

La asignación de docentes a comisiones es el mecanismo que regula qué personas pueden operar sobre un acta de cursado: quién puede cargar notas, quién puede cerrar el acta y quién solo tiene acceso de consulta. Sin esta asignación correctamente configurada, el sistema no puede aplicar controles de acceso diferenciados por cargo, lo que comprometería la integridad del proceso de evaluación académica.

Esta historia habilita al administrador a vincular docentes con comisiones activas del período lectivo en curso, asignando un cargo que determina el nivel de permisos otorgados automáticamente.

---

## 3. Entidades y Atributos

### 3.1 Entidad: AsignacionDocente

Representa el vínculo entre un docente, una comisión y un cargo, vigente para el período activo.

| Atributo          | Tipo         | Restricciones                                              |
|-------------------|--------------|------------------------------------------------------------|
| id                | UUID / Long  | PK, generado automáticamente                               |
| comision_id       | FK           | NOT NULL, referencia a Comision                            |
| docente_id        | FK           | NOT NULL, referencia a Usuario (rol = Docente)             |
| cargo             | Enum         | NOT NULL, valores permitidos: `TITULAR`, `JTP`, `AYUDANTE` |
| fecha_asignacion  | Date         | NOT NULL, generada automáticamente al momento de alta      |
| asignado_por      | FK           | NOT NULL, referencia a Usuario (rol = Administrador)       |
| activa            | Boolean      | NOT NULL, default `true`; `false` si fue eliminada         |

**Restricciones de integridad:**
- UNIQUE (comision_id, docente_id) — un docente no puede tener dos asignaciones en la misma comisión.
- Máximo una fila con cargo = `TITULAR` por `comision_id` donde `activa = true`.

---

### 3.2 Entidad de referencia: Comision (atributos relevantes)

| Atributo         | Tipo    | Descripción                                              |
|------------------|---------|----------------------------------------------------------|
| id               | UUID    | PK                                                       |
| materia_id       | FK      | Referencia a Materia                                     |
| plan_estudio_id  | FK      | Referencia a PlanDeEstudio                               |
| periodo_id       | FK      | Referencia a PeriodoLectivo                              |
| turno            | Enum    | `MAÑANA`, `TARDE`, `NOCHE`                               |
| nombre           | String  | Ej: "Comisión A — Análisis Matemático I — Turno Mañana"  |
| estado           | Enum    | `ACTIVA`, `CERRADA`                                      |

---

### 3.3 Entidad de referencia: PeriodoLectivo (atributos relevantes)

| Atributo     | Tipo    | Descripción                                               |
|--------------|---------|-----------------------------------------------------------|
| id           | UUID    | PK                                                        |
| año          | Integer | Ej: 2026                                                  |
| cuatrimestre | Integer | 1 o 2                                                     |
| activo       | Boolean | Solo un período puede tener `activo = true` en todo momento |
| descripcion  | String  | Ej: "2026 — Primer Cuatrimestre"                         |

---

### 3.4 Extensión de entidad: AuditLog

Cada acción sobre `AsignacionDocente` genera un registro de auditoría con los siguientes campos mínimos:

| Atributo     | Tipo      | Descripción                                                               |
|--------------|-----------|---------------------------------------------------------------------------|
| id           | UUID      | PK                                                                        |
| entidad      | String    | Valor fijo: `"AsignacionDocente"`                                         |
| entidad_id   | UUID/Long | ID del registro de AsignacionDocente afectado                             |
| accion       | Enum      | `ALTA`, `MODIFICACION_CARGO`, `BAJA`                                      |
| usuario_id   | FK        | Administrador que ejecutó la acción                                       |
| fecha_hora   | Timestamp | Fecha y hora exacta de la acción (zona horaria del servidor)              |
| detalle      | String    | Descripción textual: cargo anterior/nuevo, docente, comisión involucrada  |

---

## 4. Reglas de Negocio

**RN-01 — Período activo requerido**
Solo se pueden realizar asignaciones si el sistema tiene exactamente un período lectivo marcado como activo. Si no existe período activo configurado, el sistema debe bloquear el acceso a la funcionalidad de asignación e informar al administrador que debe configurar un período activo antes de continuar.

**RN-02 — Solo comisiones del período activo**
Las comisiones disponibles para asignación son únicamente las que pertenecen al período lectivo activo. No se pueden asignar docentes a comisiones de períodos pasados ni futuros.

**RN-03 — Solo usuarios con rol Docente**
Únicamente usuarios con el rol `Docente` pueden ser asignados a comisiones. Los usuarios con rol `Administrador` o `Estudiante` no aparecen en los resultados de búsqueda ni pueden ser asignados.

**RN-04 — Cargos permitidos**
Los únicos cargos válidos son: `Titular`, `JTP` y `Ayudante`. No se admite ningún otro valor.

**RN-05 — Máximo un Titular por comisión**
Cada comisión puede tener como máximo un docente asignado con cargo `Titular` de forma simultánea. El intento de asignar un segundo Titular a una comisión que ya tiene uno debe ser rechazado con un mensaje de error claro.

**RN-06 — Sin límite para JTP y Ayudantes**
Una comisión puede tener múltiples docentes con cargo `JTP` y múltiples docentes con cargo `Ayudante`, sin restricción de cantidad.

**RN-07 — Sin duplicados de docente en misma comisión**
Un docente no puede tener más de una asignación activa en la misma comisión. Si el administrador intenta asignar un docente que ya se encuentra asignado a esa comisión (con cualquier cargo), el sistema debe rechazarlo con un mensaje de error que identifique el duplicado.

**RN-08 — Docente en múltiples comisiones**
Un mismo docente puede estar asignado a múltiples comisiones dentro del mismo período lectivo, pudiendo tener el mismo cargo u otro distinto en cada una.

**RN-09 — Permisos derivados del cargo**
El cargo asignado determina automáticamente los permisos del docente sobre el acta de la comisión:
- `Titular`: puede cargar notas y cerrar el acta.
- `JTP`: puede cargar notas, no puede cerrar el acta.
- `Ayudante`: solo tiene acceso de consulta; no puede cargar notas ni cerrar el acta.

**RN-10 — Cambio de cargo sin recrear asignación**
El administrador puede modificar el cargo de un docente ya asignado a una comisión (ej: de `JTP` a `Titular`) sin necesidad de eliminar y volver a crear la asignación. El sistema actualiza el cargo directamente y registra el cambio en el log de auditoría.

**RN-11 — Baja de asignación con advertencia si hay notas**
El administrador puede eliminar una asignación. Si el docente tiene notas cargadas en el acta de esa comisión, el sistema debe mostrar una advertencia explícita antes de confirmar la eliminación. La eliminación procede solo si el administrador confirma.

**RN-12 — Baja bloqueada si el acta está cerrada**
Si el acta de la comisión ya fue cerrada, no se puede eliminar ninguna asignación de docente en esa comisión. El sistema debe informar que la operación no está permitida porque el acta se encuentra cerrada.

**RN-13 — Docente no puede modificar su propia asignación**
El docente puede consultar sus asignaciones en su panel, pero no puede modificar, eliminar ni solicitar cambios sobre ellas desde la interfaz.

**RN-14 — Auditoría de todas las acciones**
Toda operación de alta, modificación de cargo y baja de asignación debe generar un registro en el log de auditoría con: usuario administrador responsable, fecha y hora exacta, y descripción de la acción realizada.

---

## 5. Flujos Principales

### Flujo 5.1 — Asignar docente a comisión (alta)

**Precondición:** El administrador ha iniciado sesión. Existe un período lectivo activo. La comisión ya fue creada previamente.

| Paso | Actor         | Acción                                                                                                                                 |
|------|---------------|----------------------------------------------------------------------------------------------------------------------------------------|
| 1    | Administrador | Accede al módulo de Gestión de Docentes y selecciona "Asignar docente a comisión".                                                     |
| 2    | Sistema       | Verifica que existe un período lectivo activo. Si no existe, muestra mensaje de error y detiene el flujo (ver FA-01).                  |
| 3    | Administrador | Selecciona la comisión de destino desde un listado filtrado por período activo. El listado muestra: materia, plan, turno y período.     |
| 4    | Administrador | Ingresa nombre/apellido o número de legajo del docente en el campo de búsqueda.                                                         |
| 5    | Sistema       | Retorna coincidencias de usuarios con rol `Docente`. Excluye roles Administrador y Estudiante.                                          |
| 6    | Administrador | Selecciona el docente del resultado de búsqueda.                                                                                       |
| 7    | Administrador | Selecciona el cargo: `Titular`, `JTP` o `Ayudante`.                                                                                   |
| 8    | Administrador | Confirma la asignación.                                                                                                                |
| 9    | Sistema       | Valida: (a) docente no duplicado en comisión (RN-07), (b) si cargo=Titular, que no exista ya un Titular (RN-05).                       |
| 10   | Sistema       | Si las validaciones pasan: crea registro en `AsignacionDocente`, genera registro en `AuditLog` con acción `ALTA`.                      |
| 11   | Sistema       | Muestra confirmación de éxito con resumen: nombre del docente, comisión, cargo asignado.                                               |

**Flujo alternativo FA-01 — Sin período activo:**
En el paso 2, si no hay período activo configurado, el sistema muestra: *"No hay un período lectivo activo configurado. Debe activar un período antes de realizar asignaciones."* El flujo termina sin opción de continuar.

**Flujo alternativo FA-02 — Docente ya asignado a la misma comisión:**
En el paso 9a, si el docente ya tiene una asignación activa en la comisión seleccionada, el sistema muestra: *"El docente [Nombre Apellido] ya se encuentra asignado a esta comisión con cargo [Cargo actual]. No se puede duplicar la asignación."* El administrador puede corregir la selección.

**Flujo alternativo FA-03 — Titular duplicado:**
En el paso 9b, si se intenta asignar un `Titular` a una comisión que ya tiene uno, el sistema muestra: *"La comisión ya tiene un docente Titular asignado: [Nombre Apellido]. Solo puede haber un Titular por comisión."* El administrador puede elegir un cargo diferente o cancelar.

**Flujo alternativo FA-04 — Sin resultados en búsqueda:**
En el paso 5, si la búsqueda no retorna resultados, el sistema muestra: *"No se encontraron docentes con ese nombre, apellido o legajo."* El administrador puede intentar otro término.

---

### Flujo 5.2 — Modificar cargo de una asignación existente

**Precondición:** Existe una asignación activa del docente en la comisión.

| Paso | Actor         | Acción                                                                                                                                                    |
|------|---------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1    | Administrador | Accede al detalle de una comisión y visualiza el listado de docentes asignados con su cargo actual.                                                        |
| 2    | Administrador | Selecciona la opción "Modificar cargo" sobre la asignación de un docente.                                                                                  |
| 3    | Sistema       | Muestra el cargo actual y los cargos alternativos disponibles.                                                                                             |
| 4    | Administrador | Selecciona el nuevo cargo y confirma.                                                                                                                      |
| 5    | Sistema       | Valida: si el nuevo cargo es `Titular`, verifica que no exista otro Titular activo en la comisión.                                                         |
| 6    | Sistema       | Si la validación pasa: actualiza el campo `cargo` en `AsignacionDocente`, registra en `AuditLog` con acción `MODIFICACION_CARGO` indicando cargo anterior y nuevo cargo. |
| 7    | Sistema       | Muestra confirmación: *"Cargo actualizado correctamente para [Nombre Apellido] en [Comisión]."*                                                            |

**Flujo alternativo FA-05 — Cambio a Titular con Titular existente:**
En el paso 5, si ya existe un Titular en la comisión, el sistema muestra: *"Ya existe un Titular asignado en esta comisión. Debe cambiar su cargo antes de asignar un nuevo Titular."* El flujo no avanza.

---

### Flujo 5.3 — Eliminar asignación de docente

**Precondición:** Existe una asignación activa del docente en la comisión. El acta de la comisión no está cerrada.

| Paso | Actor         | Acción                                                                                                                                                       |
|------|---------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1    | Administrador | Accede al detalle de la comisión y selecciona "Eliminar asignación" sobre un docente.                                                                        |
| 2    | Sistema       | Verifica si el acta de la comisión está cerrada. Si está cerrada, muestra mensaje de bloqueo y detiene el flujo (ver FA-06).                                  |
| 3    | Sistema       | Verifica si el docente tiene notas cargadas en el acta de la comisión.                                                                                       |
| 4    | Sistema       | Si hay notas cargadas: muestra advertencia explícita — *"El docente [Nombre] tiene notas cargadas en esta comisión. ¿Confirma igualmente la eliminación?"*    |
| 5    | Administrador | Confirma o cancela la eliminación.                                                                                                                           |
| 6    | Sistema       | Si confirma: marca la asignación como `activa = false`, registra en `AuditLog` con acción `BAJA`.                                                            |
| 7    | Sistema       | Muestra confirmación: *"La asignación de [Nombre Apellido] en [Comisión] fue eliminada correctamente."*                                                      |

**Flujo alternativo FA-06 — Acta cerrada:**
En el paso 2, si el acta está cerrada, el sistema muestra: *"No es posible eliminar la asignación porque el acta de esta comisión ya fue cerrada."* El botón de eliminación queda deshabilitado para todas las asignaciones de esa comisión.

**Flujo alternativo FA-07 — Administrador cancela la eliminación:**
En el paso 5, si el administrador cancela, no se realiza ninguna modificación y el flujo termina sin cambios.

---

### Flujo 5.4 — Docente consulta sus comisiones asignadas

**Precondición:** El docente ha iniciado sesión.

| Paso | Actor   | Acción                                                                                                             |
|------|---------|--------------------------------------------------------------------------------------------------------------------|
| 1    | Docente | Accede a su panel personal.                                                                                        |
| 2    | Sistema | Muestra listado de comisiones asignadas al docente en el período activo, incluyendo: nombre de materia, turno, comisión y cargo actual. |
| 3    | Docente | Puede consultar el detalle de cada comisión.                                                                       |
| 4    | Docente | No dispone de opciones de modificación ni eliminación sobre sus propias asignaciones.                              |

---

## 6. Criterios de Aceptación

**CA-01 — Bloqueo sin período activo**
- **Dado** que no hay un período lectivo marcado como activo en el sistema,
- **Cuando** el administrador accede al módulo de asignación de docentes,
- **Entonces** el sistema muestra el mensaje *"No hay un período lectivo activo configurado"* y no permite realizar ninguna asignación.

**CA-02 — Alta exitosa de asignación**
- **Dado** que existe un período activo, una comisión en ese período y un docente con rol Docente no asignado previamente a esa comisión,
- **Cuando** el administrador selecciona la comisión, el docente y el cargo, y confirma,
- **Entonces** el sistema crea la asignación, la muestra en el listado de la comisión y genera un registro de auditoría con acción `ALTA`.

**CA-03 — Rechazo por Titular duplicado**
- **Dado** que una comisión ya tiene un docente asignado con cargo `Titular`,
- **Cuando** el administrador intenta asignar otro docente con cargo `Titular` a esa misma comisión,
- **Entonces** el sistema rechaza la operación y muestra el mensaje indicando que ya existe un Titular asignado, sin crear la asignación.

**CA-04 — Rechazo por docente duplicado en comisión**
- **Dado** que un docente ya tiene una asignación activa en una comisión determinada,
- **Cuando** el administrador intenta asignar ese mismo docente a la misma comisión (con cualquier cargo),
- **Entonces** el sistema rechaza la operación y muestra el nombre del docente y su cargo actual.

**CA-05 — Docente asignado a múltiples comisiones**
- **Dado** que un docente está asignado a la comisión A con cargo `JTP`,
- **Cuando** el administrador lo asigna a la comisión B con cargo `Titular`,
- **Entonces** el sistema acepta ambas asignaciones y el docente aparece en el listado de ambas comisiones con sus cargos respectivos.

**CA-06 — Búsqueda excluye roles no-Docente**
- **Dado** que existen usuarios con roles Administrador y Estudiante en el sistema,
- **Cuando** el administrador realiza una búsqueda en el módulo de asignación,
- **Entonces** los resultados solo contienen usuarios con rol `Docente`.

**CA-07 — Modificación de cargo sin recrear asignación**
- **Dado** que un docente está asignado a una comisión con cargo `JTP`,
- **Cuando** el administrador cambia su cargo a `Titular` (y no hay otro Titular en esa comisión),
- **Entonces** el sistema actualiza el cargo, genera un registro de auditoría con acción `MODIFICACION_CARGO`, y el docente no pierde su vínculo con la comisión.

**CA-08 — Advertencia por notas antes de eliminar**
- **Dado** que un docente tiene notas cargadas en el acta de una comisión y el acta no está cerrada,
- **Cuando** el administrador intenta eliminar su asignación,
- **Entonces** el sistema muestra una advertencia explícita antes de permitir la confirmación de la eliminación.

**CA-09 — Bloqueo de baja con acta cerrada**
- **Dado** que el acta de una comisión fue cerrada,
- **Cuando** el administrador intenta eliminar cualquier asignación de docente en esa comisión,
- **Entonces** el sistema bloquea la operación y muestra el mensaje *"No es posible eliminar la asignación porque el acta ya fue cerrada"*.

**CA-10 — Auditoría de toda acción**
- **Dado** que un administrador ejecuta cualquier operación de alta, modificación o baja sobre una asignación,
- **Cuando** la operación es completada exitosamente,
- **Entonces** el sistema registra en el log de auditoría: entidad `AsignacionDocente`, ID del registro, acción realizada, ID del administrador y fecha/hora exacta.

**CA-11 — Panel del docente muestra asignaciones del período activo**
- **Dado** que un docente tiene asignaciones en el período activo y también en períodos anteriores,
- **Cuando** el docente accede a su panel personal,
- **Entonces** el sistema muestra únicamente las asignaciones correspondientes al período lectivo activo.

**CA-12 — Docente no puede modificar su asignación**
- **Dado** que un docente está asignado a una comisión,
- **Cuando** el docente accede a su panel,
- **Entonces** no aparece ningún control de edición, modificación ni eliminación sobre sus asignaciones.

**CA-13 — Permisos derivados del cargo se aplican automáticamente**
- **Dado** que un docente es asignado como `JTP` a una comisión,
- **Cuando** accede al acta de esa comisión,
- **Entonces** puede cargar notas pero no dispone de la opción de cerrar el acta; si es `Ayudante`, solo puede consultar.

---

## 7. Casos Borde

**CB-01 — Comisión sin docentes asignados**
Una comisión válida puede existir sin ningún docente asignado. El sistema no bloquea la visualización de la comisión; simplemente muestra el listado de asignaciones vacío. Las operaciones sobre el acta quedan sin usuarios habilitados hasta que se realice al menos una asignación.

**CB-02 — Docente sin asignaciones en el período activo**
Un docente puede existir en el sistema sin ninguna asignación en el período activo. Su panel muestra un listado vacío con un mensaje informativo, sin errores.

**CB-03 — Cambio de cargo de Titular a JTP cuando hay notas ya cargadas**
Si el Titular tiene notas cargadas y el administrador cambia su cargo a `JTP`, el cargo se actualiza correctamente. Las notas previamente cargadas no son eliminadas. Los permisos del docente quedan determinados por el nuevo cargo (`JTP`).

**CB-04 — El único Titular de una comisión es eliminado con notas cargadas**
Si el único Titular de una comisión es eliminado y tenía notas cargadas, el sistema muestra la advertencia (RN-11), permite la confirmación y procede. La comisión queda sin Titular; el acta no se cierra automáticamente y puede quedarse en estado incompleto hasta que se asigne un nuevo Titular.

**CB-05 — Búsqueda de docente con resultado ambiguo (mismo nombre y apellido)**
Si dos docentes tienen el mismo nombre y apellido, el sistema los muestra como dos resultados distintos, diferenciándolos por número de legajo. El administrador debe seleccionar explícitamente uno de los dos.

**CB-06 — Docente como Titular en comisión A y JTP en comisión B**
Es un caso válido. El docente puede ser Titular en la comisión A y JTP en la comisión B. El sistema acepta ambas asignaciones sin conflicto.

**CB-07 — Cambio de período activo mientras hay asignaciones en curso**
Si el administrador activa un nuevo período lectivo, las asignaciones del período anterior quedan históricas y no visibles en el módulo de asignación activo. El docente aún puede verlas en su historial si el sistema lo provee, pero no en el panel del período activo.

**CB-08 — Eliminación de asignación sin notas cargadas**
El sistema no muestra advertencia alguna y procede directamente a solicitar confirmación estándar antes de eliminar.

---

## 8. Fuera de Alcance

Las siguientes funcionalidades están explícitamente excluidas de esta historia:

- **Creación de comisiones:** las comisiones deben existir previamente. Su creación corresponde a otra historia (C.1 o equivalente).
- **Gestión de períodos lectivos:** la activación y configuración del período activo no forma parte de esta historia.
- **Asignación masiva o por lote:** esta historia cubre únicamente asignaciones individuales, una por vez.
- **Asignación a períodos futuros o pasados:** solo se opera sobre el período activo.
- **Permisos independientes al cargo:** los permisos son consecuencia directa del cargo y no pueden configurarse de forma personalizada por docente.
- **Notificaciones al docente:** el sistema no envía correos ni notificaciones push al docente cuando es asignado, modificado o dado de baja.
- **Autoasignación por parte del docente:** el docente no puede solicitarse a sí mismo en ninguna comisión.
- **Historial de asignaciones de períodos anteriores:** el panel del docente muestra únicamente el período activo.
- **Restricciones por cantidad máxima de comisiones por docente:** no existe tope sobre cuántas comisiones puede tener un docente en el período activo.

---

## 9. Dependencias y Siguiente Historia

### Dependencias previas (deben estar completadas)

| Historia | Descripción                                                              |
|----------|--------------------------------------------------------------------------|
| B.1      | Gestión de Períodos Lectivos — debe existir un período marcado como activo |
| C.1      | Gestión de Comisiones — las comisiones deben estar creadas en el período  |
| Auth     | Autenticación y Perfiles — deben existir usuarios con rol `Docente`       |

### Historias habilitadas por D.1

| Historia | Descripción                                                                          |
|----------|--------------------------------------------------------------------------------------|
| D.2      | Carga de notas por docente — requiere saber qué docente tiene permiso en qué comisión|
| D.3      | Cierre de actas — requiere identificar al Titular de la comisión                     |
| D.4      | Auditoría y trazabilidad — se amplía sobre el AuditLog definido aquí                 |

---

## 10. Asunciones Validadas

| N.° | Asunción validada                                                                                                  |
|-----|--------------------------------------------------------------------------------------------------------------------|
| 1   | Comisión = combinación de Materia + Plan de Estudio + Período Lectivo + Turno.                                     |
| 2   | El turno (Mañana/Tarde/Noche) diferencia comisiones de la misma materia en el mismo período.                       |
| 3   | Las comisiones se crean en un paso previo, fuera del alcance de esta historia.                                     |
| 4   | Una materia puede tener entre 1 y N comisiones activas en el mismo período.                                        |
| 5   | Solo se asigna al período lectivo activo; no se permite operar sobre períodos pasados ni futuros.                  |
| 6   | El período lectivo se identifica por año + cuatrimestre (ej: 2026-1), y el sistema tiene uno marcado como activo.  |
| 7   | Si no hay período activo configurado, la funcionalidad de asignación queda bloqueada con mensaje informativo.      |
| 8   | Los únicos cargos posibles son exactamente: Titular, JTP y Ayudante.                                               |
| 9   | Máximo un Titular activo por comisión.                                                                             |
| 10  | JTP y Ayudantes pueden ser múltiples por comisión, sin límite fijo.                                               |
| 11  | Titular: puede cargar notas y cerrar actas. JTP: puede cargar notas, no cerrar. Ayudante: solo consulta.           |
| 12  | Un docente puede asignarse a múltiples comisiones en el mismo período, con igual o distinto cargo.                 |
| 13  | El intento de asignar un docente ya asignado a la misma comisión es rechazado con mensaje de error claro.          |
| 14  | El intento de asignar un segundo Titular a una comisión que ya tiene uno es rechazado.                             |
| 15  | Solo usuarios con rol Docente pueden ser asignados; Administrador y Estudiante quedan excluidos.                   |
| 16  | La búsqueda de docente se realiza por nombre, apellido o número de legajo.                                         |
| 17  | El docente visualiza en su panel las comisiones asignadas en el período activo junto a su cargo.                   |
| 18  | El docente no puede modificar ni eliminar su propia asignación.                                                    |
| 19  | El administrador puede modificar el cargo de una asignación existente sin eliminarla y recrearla.                  |
| 20  | Al eliminar una asignación con notas cargadas, el sistema muestra advertencia antes de confirmar.                  |
| 21  | Si el acta de la comisión ya fue cerrada, no es posible eliminar ninguna asignación.                               |
| 22  | Toda acción de alta, modificación y baja queda registrada en el log de auditoría con usuario y fecha/hora exacta.  |
