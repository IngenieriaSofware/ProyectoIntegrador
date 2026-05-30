# Especificacion Funcional — B.1 Gestion de Oferta Academica: Carreras y Planes de Estudio

---

## 1. Metadata

| Campo          | Valor                                                    |
|----------------|----------------------------------------------------------|
| Fecha          | 2026-05-05                                               |
| Modulo         | B — Modulo Academico                                     |
| Historia ID    | B.1                                                      |
| Story Points   | 8                                                        |
| Actor          | Administrador                                            |
| Estado         | Validada — lista para implementar                        |
| Autores        | Cruseño Alvaro, Dehaes Juan, Destefanis Adrian, Dominguez Alan, Garais Santiago, Narvaja Samuel |

---

## 2. Historia de Usuario

**Como** administrador del sistema,
**quiero** definir carreras y sus respectivos planes de estudio con año de vigencia,
**para poder** gestionar la oferta academica de manera organizada y consistente.

### Objetivo / Valor de negocio

El modulo reemplaza el registro manual de carreras y planes que actualmente se realiza en planillas desconectadas. Permite al personal de la Oficina de Alumnos mantener un catalogo centralizado de la oferta academica, con trazabilidad de cambios y soporte para multiples planes historicos por carrera. Este catalogo es el punto de partida necesario para las funcionalidades de inscripcion y seguimiento academico.

---

## 3. Entidades y Atributos

### 3.1 Entidad: Carrera

| Atributo     | Tipo           | Requerido | Restricciones                                                   |
|--------------|----------------|-----------|-----------------------------------------------------------------|
| id           | UUID           | Si        | Generado por el sistema. No editable.                           |
| codigo       | VARCHAR(10)    | Si        | Unico en el sistema. Solo mayusculas y sin espacios. No editable una vez creado. Ejemplo: "IS", "LCC". |
| nombre       | VARCHAR(200)   | Si        | Unico en el sistema (comparacion case-insensitive). Editable.   |
| descripcion  | TEXT           | No        | Texto libre. Editable.                                          |
| activa       | BOOLEAN        | Si        | Default: true. False implica baja logica.                       |
| creada_en    | TIMESTAMP      | Si        | Generado por el sistema al momento de creacion.                 |
| actualizada_en | TIMESTAMP    | Si        | Generado por el sistema en cada modificacion.                   |

### 3.2 Entidad: PlanDeEstudio

| Atributo        | Tipo         | Requerido | Restricciones                                                                   |
|-----------------|--------------|-----------|---------------------------------------------------------------------------------|
| id              | UUID         | Si        | Generado por el sistema. No editable.                                           |
| carrera_id      | UUID (FK)    | Si        | Referencia a Carrera. No editable una vez creado.                               |
| anio_vigencia   | INTEGER      | Si        | Año calendario de cuatro digitos (ej: 2019, 2024). Unico por carrera. No editable una vez creado. |
| descripcion     | TEXT         | No        | Texto libre. Editable.                                                          |
| duracion_anios  | INTEGER      | No        | Duracion teorica del plan expresada en años. Valor positivo mayor a cero.       |
| activo          | BOOLEAN      | Si        | Default: false al crear. Solo un plan puede estar activo por carrera a la vez para nuevas inscripciones. |
| creado_en       | TIMESTAMP    | Si        | Generado por el sistema al momento de creacion.                                 |
| actualizado_en  | TIMESTAMP    | Si        | Generado por el sistema en cada modificacion.                                   |

### 3.3 Entidad: AuditLog (existente — se extiende)

| Atributo       | Tipo         | Descripcion                                                         |
|----------------|--------------|---------------------------------------------------------------------|
| id             | UUID         | Identificador unico del registro de auditoria.                      |
| entidad        | VARCHAR(50)  | Nombre de la entidad afectada: "Carrera" o "PlanDeEstudio".        |
| entidad_id     | UUID         | Identificador del registro afectado.                                |
| accion         | VARCHAR(30)  | Tipo de accion: CREAR, EDITAR, ACTIVAR_PLAN, DESACTIVAR_PLAN, BAJA_LOGICA. |
| usuario_id     | UUID         | Referencia al Administrador que ejecuto la accion.                  |
| datos_anteriores | JSONB      | Estado del registro antes de la modificacion (null en creacion).    |
| datos_nuevos   | JSONB        | Estado del registro despues de la modificacion.                     |
| ejecutado_en   | TIMESTAMP    | Timestamp del momento exacto de la accion.                          |

---

## 4. Reglas de Negocio

**RN-01 — Unicidad del codigo de carrera.**
El codigo de carrera debe ser unico en todo el sistema. El sistema rechaza la creacion si ya existe una carrera con el mismo codigo, independientemente del estado activo/inactivo de la carrera existente. El codigo no puede modificarse una vez creada la carrera.

**RN-02 — Unicidad del nombre de carrera.**
El nombre de carrera debe ser unico en el sistema. La comparacion se realiza de manera case-insensitive, es decir, "Ingenieria en Sistemas" e "ingenieria en sistemas" se consideran el mismo nombre. Esta validacion aplica tanto al crear como al editar el nombre.

**RN-03 — Unicidad del año de vigencia por carrera.**
Dentro de una misma carrera, no pueden existir dos planes con el mismo año de vigencia. Si ya existe un plan con ese año para esa carrera, el sistema rechaza la creacion. Esta restriccion aplica independientemente del estado activo/inactivo de los planes existentes.

**RN-04 — Un solo plan activo por carrera.**
En un momento dado, solo un plan de estudio puede estar marcado como activo dentro de una carrera. El plan activo es el que se utiliza como referencia para nuevas inscripciones. Activar un nuevo plan NO desactiva automaticamente el anterior; el Administrador debe desactivar el plan anterior de forma explicita y manual si asi lo desea.

**RN-05 — Conservacion de planes inactivos.**
Los planes de estudio inactivos no se eliminan fisicamente del sistema. Se conservan con todo su historial de materias y correlatividades para garantizar la integridad academica de los estudiantes inscriptos bajo esos planes.

**RN-06 — Alcance de esta historia: contenedor del plan.**
La creacion de un plan de estudio en esta historia unicamente crea el contenedor del plan (identificado por carrera y año de vigencia). La carga de materias y correlatividades es responsabilidad de una historia posterior del modulo B.

**RN-07 — Permiso exclusivo del Administrador.**
Unicamente el usuario con rol Administrador puede crear carreras, editar datos de carreras, crear planes de estudio, activar planes, desactivar planes y dar de baja carreras. Estudiantes y Docentes no tienen acceso de escritura sobre estas entidades.

**RN-08 — Baja logica de carreras con estudiantes.**
Una carrera que tiene estudiantes inscriptos no puede eliminarse fisicamente del sistema. La operacion de baja es siempre logica: se cambia el campo activa a false. La carrera permanece en la base de datos y conserva todo su historial. Una carrera inactiva no puede recibir nuevas inscripciones.

**RN-09 — Atributos editables de la carrera.**
Solo se pueden editar el nombre y la descripcion de una carrera existente. El codigo de carrera es inmutable una vez creado, ya que puede ser referenciado por otros registros del sistema.

**RN-10 — Confirmacion obligatoria antes de desactivar un plan.**
Antes de ejecutar la desactivacion de un plan de estudio, el sistema debe mostrar al Administrador un mensaje de confirmacion que incluya la cantidad de estudiantes actualmente inscriptos bajo ese plan. El Administrador debe confirmar explicitamente para que la accion se ejecute.

**RN-11 — Registro de auditoria obligatorio.**
Toda accion de escritura sobre las entidades Carrera y PlanDeEstudio (crear, editar, activar, desactivar, dar de baja) debe quedar registrada en el audit_log existente del sistema, con el identificador del administrador que ejecuto la accion, la marca de tiempo y el estado anterior y posterior del registro.

**RN-12 — Año de vigencia del plan: formato y rango.**
El año de vigencia de un plan de estudio debe ser un numero entero de cuatro digitos que represente un año calendario valido. El sistema debe rechazar valores menores a 1900 o mayores al año calendario actual mas cinco años, como medida de validacion basica.

---

## 5. Flujos Principales

### Flujo 1 — Crear una nueva carrera

**Actor:** Administrador
**Precondicion:** El Administrador esta autenticado en el sistema con rol Administrador.

1. El Administrador accede a la seccion "Gestion Academica" > "Carreras".
2. El sistema muestra el listado paginado de carreras existentes.
3. El Administrador selecciona la accion "Nueva Carrera".
4. El sistema presenta un formulario con los campos: Codigo (requerido), Nombre (requerido), Descripcion (opcional).
5. El Administrador completa los campos y selecciona "Guardar".
6. El sistema valida:
   - Que el Codigo no este vacio y no contenga espacios.
   - Que el Codigo sea unico en el sistema (RN-01).
   - Que el Nombre no este vacio.
   - Que el Nombre sea unico en el sistema de manera case-insensitive (RN-02).
7. Si todas las validaciones pasan, el sistema crea el registro de Carrera con activa = true.
8. El sistema registra la accion en el audit_log (RN-11) con accion = CREAR.
9. El sistema redirige al Administrador al detalle de la carrera recien creada y muestra un mensaje de exito: "Carrera creada correctamente."

**Flujo alternativo 1a — Codigo duplicado:**
En el paso 6, si el codigo ya existe, el sistema muestra el mensaje de error inline sobre el campo Codigo: "El codigo '[valor ingresado]' ya esta en uso." El formulario permanece abierto con los datos ingresados.

**Flujo alternativo 1b — Nombre duplicado:**
En el paso 6, si el nombre ya existe (case-insensitive), el sistema muestra el mensaje de error inline sobre el campo Nombre: "Ya existe una carrera con este nombre." El formulario permanece abierto con los datos ingresados.

---

### Flujo 2 — Editar datos de una carrera existente

**Actor:** Administrador
**Precondicion:** Existe al menos una carrera en el sistema. El Administrador esta autenticado.

1. El Administrador accede al listado de carreras y selecciona una carrera especifica.
2. El sistema muestra el detalle de la carrera con sus datos actuales y la lista de sus planes de estudio.
3. El Administrador selecciona la accion "Editar".
4. El sistema muestra un formulario con los campos editables: Nombre y Descripcion. El campo Codigo se muestra como solo lectura.
5. El Administrador modifica uno o ambos campos editables y selecciona "Guardar".
6. El sistema valida que el nuevo Nombre sea unico en el sistema de manera case-insensitive (RN-02), excluyendo la propia carrera de la comparacion.
7. Si la validacion pasa, el sistema actualiza el registro y registra en audit_log con accion = EDITAR.
8. El sistema muestra el mensaje de exito: "Carrera actualizada correctamente."

**Flujo alternativo 2a — Nombre en uso por otra carrera:**
En el paso 6, si el nombre ingresado ya pertenece a otra carrera, el sistema muestra el error inline: "Ya existe una carrera con este nombre."

---

### Flujo 3 — Dar de baja una carrera (baja logica)

**Actor:** Administrador
**Precondicion:** La carrera existe y esta activa.

1. El Administrador accede al detalle de la carrera.
2. El Administrador selecciona la accion "Dar de baja".
3. El sistema muestra un dialogo de confirmacion: "Esta accion desactivara la carrera '[Nombre]'. Los estudiantes inscriptos conservaran su historial pero no podran realizarse nuevas inscripciones a esta carrera. ¿Confirma?"
4. El Administrador confirma.
5. El sistema cambia activa = false en el registro de Carrera (RN-08).
6. El sistema registra en audit_log con accion = BAJA_LOGICA.
7. El sistema muestra el mensaje de exito: "Carrera dada de baja correctamente." La carrera aparece en el listado marcada como inactiva.

**Flujo alternativo 3a — Administrador cancela:**
En el paso 4, si el Administrador cancela la confirmacion, no se realiza ninguna modificacion y el sistema vuelve al detalle de la carrera.

---

### Flujo 4 — Crear un nuevo plan de estudio

**Actor:** Administrador
**Precondicion:** Existe al menos una carrera activa en el sistema. El Administrador esta autenticado.

1. El Administrador accede al detalle de una carrera.
2. El Administrador selecciona la accion "Nuevo Plan de Estudio".
3. El sistema muestra un formulario con los campos: Año de Vigencia (requerido), Descripcion (opcional), Duracion en años (opcional).
4. El Administrador completa los campos y selecciona "Guardar".
5. El sistema valida:
   - Que el Año de Vigencia no este vacio y sea un entero valido (RN-12).
   - Que no exista otro plan con el mismo año de vigencia para esa carrera (RN-03).
6. Si todas las validaciones pasan, el sistema crea el registro de PlanDeEstudio con activo = false.
7. El sistema registra en audit_log con accion = CREAR.
8. El sistema muestra el mensaje de exito: "Plan de estudio [Año de Vigencia] creado correctamente. Este plan aun no esta activo."

**Flujo alternativo 4a — Año duplicado:**
En el paso 5, si ya existe un plan con ese año para la carrera, el sistema muestra el error inline: "Ya existe un plan de vigencia [año] para esta carrera."

---

### Flujo 5 — Activar un plan de estudio

**Actor:** Administrador
**Precondicion:** El plan existe y esta en estado inactivo.

1. El Administrador accede al detalle de la carrera y visualiza la lista de sus planes de estudio.
2. El Administrador selecciona el plan que desea activar y elige la accion "Activar".
3. El sistema verifica si existe actualmente otro plan activo para esa carrera.
4. Si existe un plan activo, el sistema muestra un aviso informativo (no bloqueante): "Atencion: la carrera ya tiene un plan activo ([año de vigencia del plan actual]). Activar este plan NO desactivara el anterior automaticamente. Para usar este plan en nuevas inscripciones, desactive el plan anterior manualmente. ¿Desea continuar?"
5. El Administrador confirma.
6. El sistema cambia activo = true en el plan seleccionado.
7. El sistema registra en audit_log con accion = ACTIVAR_PLAN.
8. El sistema muestra el mensaje de exito: "Plan [Año de Vigencia] activado correctamente."

**Flujo alternativo 5a — No hay plan activo previo:**
En el paso 3, si no existe plan activo previo, no se muestra el aviso del paso 4 y se procede directamente desde el paso 5 a la activacion.

---

### Flujo 6 — Desactivar un plan de estudio

**Actor:** Administrador
**Precondicion:** El plan existe y esta en estado activo.

1. El Administrador accede al detalle de la carrera y selecciona el plan activo.
2. El Administrador elige la accion "Desactivar".
3. El sistema consulta la cantidad de estudiantes actualmente inscriptos bajo ese plan.
4. El sistema muestra un dialogo de confirmacion obligatorio (RN-10): "Esta accion desactivara el plan [Año de Vigencia] de la carrera '[Nombre de Carrera]'. Hay [N] estudiante(s) inscripto(s) bajo este plan. Los estudiantes conservaran su historial. ¿Confirma la desactivacion?"
5. El Administrador confirma.
6. El sistema cambia activo = false en el plan.
7. El sistema registra en audit_log con accion = DESACTIVAR_PLAN.
8. El sistema muestra el mensaje de exito: "Plan [Año de Vigencia] desactivado correctamente."

**Flujo alternativo 6a — Administrador cancela:**
En el paso 5, si el Administrador cancela, no se realiza ninguna modificacion y el sistema regresa al detalle de la carrera.

---

### Flujo 7 — Editar datos de un plan de estudio

**Actor:** Administrador
**Precondicion:** El plan existe (puede estar activo o inactivo). El Administrador esta autenticado.

1. El Administrador accede al detalle de la carrera y selecciona un plan especifico.
2. El Administrador elige la accion "Editar plan".
3. El sistema muestra un formulario con los campos editables: Descripcion y Duracion en años. Los campos Año de Vigencia y Carrera asociada se muestran como solo lectura.
4. El Administrador modifica los campos deseados y selecciona "Guardar".
5. El sistema valida que la Duracion en años, si se ingresa, sea un numero entero positivo mayor a cero.
6. El sistema actualiza el registro y registra en audit_log con accion = EDITAR.
7. El sistema muestra el mensaje de exito: "Plan actualizado correctamente."

---

### Flujo 8 — Consultar listado de carreras

**Actor:** Administrador
**Precondicion:** El Administrador esta autenticado.

1. El Administrador accede a la seccion "Gestion Academica" > "Carreras".
2. El sistema muestra un listado paginado de todas las carreras del sistema (activas e inactivas).
3. Por cada carrera en el listado, se muestra:
   - Codigo de carrera.
   - Nombre de carrera.
   - Estado (Activa / Inactiva).
   - Año de vigencia del plan activo, si existe. Si no hay plan activo, se muestra "Sin plan activo".
4. El listado incluye controles de paginacion. El tamaño de pagina predeterminado es de 20 registros.
5. El Administrador puede seleccionar cualquier carrera del listado para ver su detalle completo y los planes de estudio asociados.

---

## 6. Criterios de Aceptacion

**CA-01 — Creacion exitosa de carrera**
- Dado que el Administrador esta autenticado y accede al formulario de nueva carrera,
- Cuando completa el campo Codigo con un valor unico y el campo Nombre con un valor unico, y selecciona "Guardar",
- Entonces el sistema crea la carrera con estado activa = true, la muestra en el listado y registra la accion en el audit_log.

**CA-02 — Rechazo por codigo de carrera duplicado**
- Dado que ya existe una carrera con el codigo "IS" en el sistema,
- Cuando el Administrador intenta crear una nueva carrera con el mismo codigo "IS",
- Entonces el sistema muestra un error inline sobre el campo Codigo y no crea el registro.

**CA-03 — Rechazo por nombre de carrera duplicado (case-insensitive)**
- Dado que ya existe una carrera con nombre "Ingenieria en Sistemas",
- Cuando el Administrador intenta crear una carrera con nombre "ingenieria en sistemas",
- Entonces el sistema muestra un error inline sobre el campo Nombre y no crea el registro.

**CA-04 — Codigo de carrera no editable**
- Dado que el Administrador accede al formulario de edicion de una carrera existente,
- Cuando visualiza el formulario,
- Entonces el campo Codigo se muestra como solo lectura y no puede ser modificado.

**CA-05 — Creacion exitosa de plan de estudio**
- Dado que el Administrador accede al detalle de una carrera y selecciona "Nuevo Plan de Estudio",
- Cuando completa el Año de Vigencia con un valor valido y unico para esa carrera y selecciona "Guardar",
- Entonces el sistema crea el plan con activo = false, muestra el mensaje indicando que el plan aun no esta activo, y registra la accion en el audit_log.

**CA-06 — Rechazo por año de vigencia duplicado en la misma carrera**
- Dado que ya existe un plan con año de vigencia 2019 para la carrera "Ingenieria en Sistemas",
- Cuando el Administrador intenta crear un nuevo plan con año de vigencia 2019 para la misma carrera,
- Entonces el sistema muestra un error inline y no crea el registro.

**CA-07 — Activar plan con aviso de plan activo existente**
- Dado que la carrera "Ingenieria en Sistemas" tiene el plan 2019 activo y el plan 2024 inactivo,
- Cuando el Administrador activa el plan 2024,
- Entonces el sistema muestra un aviso informativo mencionando el plan 2019 como ya activo, y tras la confirmacion activa el plan 2024 sin desactivar el plan 2019, registrando la accion en el audit_log.

**CA-08 — Desactivar plan muestra cantidad de estudiantes afectados**
- Dado que el plan 2019 de una carrera tiene 45 estudiantes inscriptos y esta activo,
- Cuando el Administrador elige desactivar ese plan,
- Entonces el sistema muestra un dialogo de confirmacion que incluye el texto "45 estudiante(s) inscripto(s) bajo este plan" antes de ejecutar la desactivacion.

**CA-09 — Baja logica de carrera con estudiantes**
- Dado que una carrera tiene estudiantes inscriptos,
- Cuando el Administrador selecciona "Dar de baja" sobre esa carrera y confirma,
- Entonces el sistema cambia activa = false, la carrera sigue visible en el listado marcada como inactiva, y los datos historicos de los estudiantes se conservan.

**CA-10 — Planes inactivos se conservan y son visibles**
- Dado que existe un plan en estado inactivo para una carrera,
- Cuando el Administrador accede al detalle de la carrera,
- Entonces el plan inactivo aparece listado con su año de vigencia y estado "Inactivo", y puede consultarse su informacion.

**CA-11 — Listado paginado con plan activo visible**
- Dado que existen multiples carreras en el sistema, algunas con plan activo y otras sin plan activo,
- Cuando el Administrador accede al listado de carreras,
- Entonces el listado muestra el año del plan activo junto a cada carrera o "Sin plan activo" si no existe, con paginacion de 20 registros por pagina.

**CA-12 — Registro en audit_log de toda accion de escritura**
- Dado que el Administrador ejecuta cualquier accion de escritura (crear, editar, activar, desactivar, dar de baja),
- Cuando la accion se completa exitosamente,
- Entonces el sistema crea un registro en audit_log con el tipo de accion correcto, el id del administrador, el timestamp y los datos anteriores y nuevos del registro afectado.

**CA-13 — Estudiante y Docente no pueden acceder a funciones de escritura**
- Dado que un usuario con rol Estudiante o Docente esta autenticado en el sistema,
- Cuando intenta acceder a las acciones de crear, editar, activar, desactivar o dar de baja carreras o planes,
- Entonces el sistema deniega el acceso y no muestra las opciones de escritura en la interfaz.

---

## 7. Casos Borde

**CB-01 — Carrera sin ningun plan de estudio.**
Una carrera puede existir en el sistema sin tener ningun plan de estudio asociado. En este estado, la carrera es valida pero no puede recibir inscripciones. En el listado, el campo de plan activo debe mostrar "Sin plan activo".

**CB-02 — Carrera con multiples planes activos simultaneos.**
Dado que la desactivacion del plan anterior no es automatica, es posible que una carrera quede con dos planes marcados como activos. El sistema debe tolerar este estado (no lo bloquea como error critico) pero debe mostrar un aviso visible en el detalle de la carrera indicando que hay mas de un plan activo y recomendando desactivar los redundantes. El modulo de inscripciones (Historia C) es el responsable de definir cual plan tiene precedencia en ese caso.

**CB-03 — Dar de baja una carrera sin estudiantes.**
Si la carrera no tiene estudiantes inscriptos, la operacion de baja logica sigue siendo logica (no fisica). El sistema no elimina el registro. El flujo de confirmacion se ejecuta igualmente.

**CB-04 — Año de vigencia en el futuro.**
El sistema permite crear un plan con un año de vigencia futuro (hasta el año actual mas cinco). Esto es valido ya que los planes pueden prepararse con anticipacion antes de entrar en vigencia.

**CB-05 — Año de vigencia muy antiguo.**
El sistema permite crear planes con años de vigencia historicos (desde 1900 en adelante) para representar planes de estudio anteriores a la digitalizacion. El sistema no bloquea años pasados.

**CB-06 — Desactivar el unico plan activo de una carrera.**
Si el Administrador desactiva el unico plan activo de una carrera, la carrera queda sin plan activo. El sistema ejecuta la accion sin bloquearla, y la carrera pasa a mostrar "Sin plan activo" en el listado. El sistema no obliga a designar un reemplazo.

**CB-07 — Editar nombre de carrera con el mismo nombre actual.**
Si el Administrador guarda el formulario de edicion de una carrera sin modificar el nombre (es decir, el nombre guardado es identico al nombre actual), el sistema no debe rechazarlo como duplicado, ya que la comparacion de unicidad debe excluir la propia carrera.

**CB-08 — Crear plan en carrera inactiva.**
El sistema debe prevenir la creacion de nuevos planes en carreras que esten en estado inactiva (activa = false). Si el Administrador intenta ejecutar esta accion, el sistema debe mostrar el mensaje de error: "No se pueden agregar planes a una carrera inactiva."

**CB-09 — Activar un plan de una carrera inactiva.**
Si una carrera esta en estado inactivo, el sistema debe impedir la activacion de sus planes. Mostrar el error: "No se pueden activar planes de una carrera inactiva. Reactive la carrera primero."

---

## 8. Fuera de Alcance

Los siguientes elementos estan explicitamente excluidos de esta historia y seran abordados en historias posteriores:

- **Carga de materias en el plan de estudio:** Esta historia unicamente crea el contenedor del plan. La definicion de materias, su nombre, carga horaria y cualquier atributo de materia corresponde a la Historia B.2.
- **Definicion de correlatividades entre materias:** La logica de prerequisitos y correlatividades entre materias es responsabilidad de la Historia B.3.
- **Logica de inscripcion a materias:** El motor de reglas que verifica correlatividades al momento de la inscripcion corresponde al Modulo C.
- **Asignacion de docentes a materias o comisiones:** Esto corresponde al Modulo D.
- **Reportes y exportacion de planes de estudio:** Corresponde al Modulo E.
- **Reactivacion de una carrera dada de baja:** El flujo inverso a la baja logica (reactivar una carrera inactiva) no esta definido en esta historia y debera especificarse por separado.
- **Gestion de comisiones o turnos dentro de un plan:** Fuera de alcance de este modulo.
- **Migracion de datos historicos desde sistemas anteriores:** La carga inicial de datos existentes es una tarea operativa separada, no contemplada en esta especificacion.

---

## 9. Dependencias y Siguiente Historia

### Dependencias previas (esta historia requiere)

| Dependencia                              | Estado         |
|------------------------------------------|----------------|
| Modulo A — Autenticacion y roles         | Implementado   |
| Entidad Usuario con rol Administrador    | Implementado   |
| Mecanismo de audit_log en el sistema     | Implementado   |

### Siguiente historia recomendada

**Historia B.2 — Gestion de Materias dentro de un Plan de Estudio**
Objetivo: Permitir al Administrador agregar, editar y eliminar materias dentro de un plan de estudio creado en B.1, definiendo nombre, codigo, carga horaria y tipo de cursado.

**Historia B.3 — Definicion de Correlatividades entre Materias**
Objetivo: Permitir al Administrador definir las relaciones de prerequisito entre materias de un plan de estudio, como base para el motor de inscripciones del Modulo C.

---

## 10. Asunciones Validadas

1. Carrera tiene nombre y codigo unico. El codigo es identificador corto (ej: "IS") y el nombre es la denominacion completa.
2. Una Carrera puede tener muchos Planes de Estudio asociados, pero solo uno puede estar activo a la vez para nuevas inscripciones.
3. El año de vigencia identifica de manera unica a un plan dentro de su carrera. No pueden existir dos planes con el mismo año dentro de la misma carrera.
4. Activar un nuevo plan NO desactiva automaticamente el plan anterior. La desactivacion del plan previo es una accion manual y explicita del Administrador.
5. Los planes inactivos se conservan en el sistema con todo su historial de materias y correlatividades para garantizar la integridad academica.
6. Esta historia unicamente crea el contenedor del plan. La carga de materias y correlatividades es responsabilidad de historias posteriores.
7. Solo el usuario con rol Administrador puede crear, editar y desactivar carreras y planes.
8. El listado de carreras es paginado y muestra el plan activo de cada carrera de manera visible.
9. Las carreras con estudiantes inscriptos se dan de baja de manera logica, no se eliminan fisicamente.
10. El nombre de la carrera es unico en el sistema de manera case-insensitive.
11. Solo se pueden editar el nombre y la descripcion de una carrera. El codigo es inmutable.
12. Toda accion de escritura queda registrada en el audit_log existente del sistema.
13. La duracion del plan (en años) es un atributo del Plan de Estudio, no de la Carrera.
14. Antes de desactivar un plan, se muestra al Administrador la cantidad de estudiantes afectados y se solicita confirmacion explicita.
15. El Plan de Estudio tiene un campo de descripcion de texto libre y opcional.
