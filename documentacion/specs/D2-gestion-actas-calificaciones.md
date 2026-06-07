# Especificación Funcional — Módulo D: Gestión de Actas de Calificaciones

**Ref. D.2 | Versión 1.0 | 2026-06-07**
**Stack: Laravel (PHP) + MySQL | Equipo: Cruseño, Dehaes, Destefanis, Dominguez, Garais, Narvaja**

---

## 1. Historia de Usuario

Como docente titular o JTP asignado a una cátedra, quiero registrar las calificaciones de mis estudiantes en parciales, trabajos prácticos y exámenes finales, y poder oficializar el acta cuando las notas están completas, para garantizar que las calificaciones queden registradas formalmente y no puedan ser modificadas sin autorización.

**Valor de negocio:** Reemplaza el proceso manual de confección de actas en papel o planillas desconectadas. Centraliza el registro de calificaciones, garantiza trazabilidad de cada carga y automatiza la actualización del estado académico del estudiante (Regular/Libre) al cierre del acta.

---

## 2. Modelo de Datos

### Entidad: actas

| Atributo | Tipo | Requerido | Restricciones |
|---|---|---|---|
| id | BIGINT UNSIGNED | SI | PK, autoincrement |
| catedra_id | BIGINT UNSIGNED | SI | FK → catedras |
| periodo_academico_id | BIGINT UNSIGNED | SI | FK → periodos_academicos |
| instancia | ENUM | SI | 'parcial_1', 'parcial_2', 'tp', 'recuperatorio_1', 'recuperatorio_2', 'examen_final' |
| estado | ENUM | SI | 'abierta', 'cerrada'; DEFAULT 'abierta' |
| fecha_cierre | DATETIME | NO | NULL si abierta |
| cerrada_por_docente_id | BIGINT UNSIGNED | NO | FK → users; NULL si abierta |
| reabierta_por_admin_id | BIGINT UNSIGNED | NO | FK → users; NULL si nunca reabierta |
| fecha_reapertura | DATETIME | NO | NULL si nunca reabierta |
| created_at | TIMESTAMP | SI | Automático Laravel |
| updated_at | TIMESTAMP | SI | Automático Laravel |

**Índice único:** UNIQUE(catedra_id, periodo_academico_id, instancia)

### Entidad: notas

| Atributo | Tipo | Requerido | Restricciones |
|---|---|---|---|
| id | BIGINT UNSIGNED | SI | PK, autoincrement |
| acta_id | BIGINT UNSIGNED | SI | FK → actas |
| estudiante_id | BIGINT UNSIGNED | SI | FK → users |
| valor | DECIMAL(4,2) | NO | [0.00, 10.00]; NULL = sin registrar |
| ausente | BOOLEAN | SI | DEFAULT false |
| cargada_por_docente_id | BIGINT UNSIGNED | SI | FK → users; quien hizo la primera carga |
| ultima_modificacion_por | BIGINT UNSIGNED | NO | FK → users; quien hizo la última escritura |
| created_at | TIMESTAMP | SI | Automático Laravel |
| updated_at | TIMESTAMP | SI | Automático Laravel |

**Índice único:** UNIQUE(acta_id, estudiante_id)

### Entidad: auditoria_acta

| Atributo | Tipo | Requerido | Restricciones |
|---|---|---|---|
| id | BIGINT UNSIGNED | SI | PK, autoincrement |
| acta_id | BIGINT UNSIGNED | SI | FK → actas |
| accion | ENUM | SI | 'creacion', 'carga_nota', 'modificacion_nota', 'cierre', 'reapertura' |
| realizado_por | BIGINT UNSIGNED | SI | FK → users |
| detalle | TEXT | NO | JSON con valores anteriores/nuevos para modificaciones |
| motivo | VARCHAR(500) | NO | Obligatorio solo para 'reapertura' |
| created_at | TIMESTAMP | SI | Automático Laravel |

**Restricción:** esta tabla no admite UPDATE ni DELETE. Solo INSERT.

### Invariante de consistencia entre valor y ausente

```
(valor IS NULL AND ausente = false) → nota sin registrar (estado inicial)
(valor IS NOT NULL AND ausente = false) → nota numérica válida
(valor IS NULL AND ausente = true) → estudiante ausente
(valor IS NOT NULL AND ausente = true) → INVÁLIDO, rechazar
```

---

## 3. Reglas de Negocio

**RN-01 — Nota numérica válida:** Una nota numérica debe ser DECIMAL con hasta 2 decimales en el rango [0.00, 10.00]. Cualquier valor fuera de este rango es rechazado.

**RN-02 — Valor ausente:** Una nota puede registrarse como "Ausente" (ausente = true, valor = NULL). No pueden coexistir valor numérico y ausente = true en el mismo registro.

**RN-03 — Umbral de aprobación:** La nota mínima de aprobación es 5.00 sobre 10.00. Valor fijo institucional, igual para todas las cátedras. No es configurable por ningún rol.

**RN-04 — Sin redondeo automático:** El sistema almacena y muestra la nota exactamente como fue ingresada. Ejemplo: 6.75 se guarda y muestra como 6.75.

**RN-05 — Unicidad de acta por instancia:** No pueden existir dos actas con la misma combinación de catedra_id + periodo_academico_id + instancia. El constraint es a nivel de base de datos y de validación de capa de negocio.

**RN-06 — Estado inicial del acta:** Un acta recién creada tiene estado "abierta" con todos los campos de nota sin valor (valor = NULL, ausente = false).

**RN-07 — Estudiantes generados automáticamente:** La lista de estudiantes del acta se genera al momento de su creación, tomando los inscriptos activos de esa cátedra en el período activo. No se pueden agregar ni eliminar estudiantes manualmente del acta.

**RN-08 — Carga restringida a titular y JTPs asignados:** Solo el docente titular o un JTP asignado a la cátedra del acta puede cargar o modificar notas en un acta "abierta". La pertenencia a la cátedra se verifica en el backend.

**RN-09 — Cierre exclusivo del titular:** Solo el docente titular puede cambiar el estado de "abierta" a "cerrada". Un JTP no puede oficializar el acta bajo ninguna circunstancia.

**RN-10 — Acta cerrada es inmutable:** Con estado "cerrada", ninguna escritura sobre notas es posible. El backend rechaza cualquier intento con HTTP 403, independientemente del rol del solicitante.

**RN-11 — Reapertura solo por Administrador:** Solo un usuario con rol Administrador puede cambiar el estado de "cerrada" a "abierta". Requiere motivo obligatorio. La acción queda registrada en auditoría.

**RN-12 — Cierre recalcula condición del estudiante:** Al cerrar el acta, el sistema recalcula automáticamente la condición académica de cada estudiante (Regular/Libre) y actualiza su Legajo. Esta operación es parte de la misma transacción atómica del cierre: si falla cualquier actualización de Legajo, el cierre completo se revierte.

**RN-13 — Visibilidad de notas para estudiantes:** Las notas de un acta son visibles para los estudiantes únicamente cuando el acta tiene estado "cerrada". Con estado "abierta", el endpoint retorna HTTP 403 para el rol Estudiante.

**RN-14 — Recuperatorio como instancia independiente:** Un recuperatorio es un acta separada (instancia 'recuperatorio_1' o 'recuperatorio_2'). No sobreescribe la nota del acta original. El docente titular decide cuál nota prevalece para el cálculo de condición al momento del cierre.

**RN-15 — Período activo obligatorio:** No se puede crear un acta sin un período académico activo configurado por el Administrador. La verificación se realiza en el backend al momento de la creación.

**RN-16 — Múltiples JTPs con iguales permisos:** Una cátedra puede tener varios JTPs asignados. Todos tienen los mismos permisos de carga de notas entre sí. No existe jerarquía entre JTPs.

**RN-17 — Guardado temporal sin efecto sobre Legajos:** El guardado temporal persiste las notas pero no modifica el estado del acta ni actualiza Legajos. Solo el cierre formal dispara el recálculo.

**RN-18 — Atomicidad de escritura en lote:** La actualización de un lote de notas ocurre dentro de una transacción de base de datos. Si una nota del lote falla la validación, se rechaza el lote completo y no se persiste ningún cambio parcial.

**RN-19 — Auditoría continua e indeleble:** Toda acción sobre el acta o sus notas queda registrada en auditoria_acta. No se permite UPDATE ni DELETE sobre esa tabla. Los registros de auditoría son inmutables.

**RN-20 — Sin notificaciones automáticas:** Esta versión no genera notificaciones (email, push, in-app) ante ningún evento del ciclo de vida del acta.

---

## 4. Flujos de Usuario

### Flujo Principal 1 — Carga de notas (guardado temporal)

1. El docente titular o JTP accede al listado de actas de su cátedra, filtrado por período activo.
2. El sistema muestra las actas disponibles con instancia y estado (abierta/cerrada).
3. El docente selecciona un acta con estado "abierta".
4. El sistema presenta la grilla con los estudiantes inscriptos. Cada fila muestra el nombre del estudiante, el campo de nota (vacío o con valor previo) y la opción "Ausente".
5. El docente ingresa para cada estudiante un valor numérico (0.00–10.00) o marca "Ausente".
6. El docente presiona "Guardar" (guardado temporal).
7. El backend recibe el payload completo del lote e inicia transacción.
8. Valida cada nota del lote contra RN-01, RN-02 y el invariante de consistencia.
9. Si todas las notas son válidas: persiste los cambios, registra auditoría (acción = 'carga_nota' o 'modificacion_nota') y retorna HTTP 200.
10. Si alguna nota falla: revierte la transacción completa, retorna HTTP 422 con errores por fila. No se persiste ningún cambio.

### Flujo Principal 2 — Cierre de acta (oficialización)

1. El docente titular presiona "Cerrar Acta" desde la vista del acta.
2. El backend verifica que el usuario sea el docente titular de esa cátedra.
3. El sistema muestra diálogo de confirmación: "Esta acción es irreversible sin intervención del Administrador. Confirmar cierre del acta de [instancia] — [cátedra] — [período]."
4. El docente confirma.
5. El backend inicia transacción: actualiza `actas` SET estado = 'cerrada', fecha_cierre = NOW(), cerrada_por_docente_id = {id_docente}.
6. Dentro de la misma transacción, recalcula la condición académica de cada estudiante y actualiza los Legajos (RN-12).
7. Registra auditoría (acción = 'cierre').
8. Si la transacción es exitosa: retorna HTTP 200. El acta queda en modo lectura.
9. Si la transacción falla: revierte completo. El acta permanece "abierta". Retorna HTTP 500 con mensaje descriptivo.

### Flujo Principal 3 — Reapertura de acta (Administrador)

1. El Administrador accede al listado de actas con filtros por cátedra, período e instancia.
2. Selecciona un acta con estado "cerrada" y presiona "Reabrir Acta".
3. El sistema muestra campo de texto obligatorio: "Motivo de reapertura".
4. El Administrador ingresa el motivo y confirma.
5. El backend valida que el motivo no esté vacío.
6. Actualiza `actas` SET estado = 'abierta', reabierta_por_admin_id = {id_admin}, fecha_reapertura = NOW().
7. Registra en auditoría (acción = 'reapertura', motivo = {texto}).
8. Las notas existentes permanecen intactas.
9. El sistema revierte el estado de condición de los estudiantes en Legajos a "pendiente de recálculo" hasta el próximo cierre.
10. Retorna HTTP 200. El acta queda editable por titular y JTPs.

### Flujo Alternativo A — JTP intenta cerrar acta

1. El JTP presiona "Cerrar Acta".
2. El backend verifica el rol del usuario sobre esa cátedra.
3. Retorna HTTP 403: "Solo el docente titular puede oficializar el acta."
4. El acta permanece "abierta". No se registra auditoría.

### Flujo Alternativo B — Intento de escritura en acta cerrada

1. Cualquier usuario envía PUT /api/actas/{id}/notas sobre un acta con estado "cerrada".
2. El backend verifica el estado del acta antes de procesar el payload.
3. Retorna HTTP 403: "El acta está cerrada y no admite modificaciones."

### Flujo Alternativo C — Creación de acta sin período activo

1. El docente intenta crear una nueva acta (POST /api/actas).
2. El backend verifica la existencia de un período académico activo.
3. Si no existe: retorna HTTP 422: "No hay un período académico activo. Contacte al Administrador."

### Flujo Alternativo D — Acta duplicada

1. El docente intenta crear un acta para una combinación cátedra + período + instancia que ya existe.
2. El backend detecta la violación del índice único.
3. Retorna HTTP 409: "Ya existe un acta para esta instancia en el período activo."

---

## 5. Endpoints API

Todos los endpoints requieren autenticación (Bearer token). Los controles de acceso se implementan mediante Laravel Policies, no en el frontend.

| Método | URI | Descripción | Roles permitidos |
|---|---|---|---|
| GET | /api/actas | Listar actas (filtros: catedra_id, periodo_id, estado, instancia) | Titular, JTP, Admin |
| POST | /api/actas | Crear nueva acta | Titular |
| GET | /api/actas/{id} | Ver detalle del acta con grilla de notas | Titular, JTP, Admin, Estudiante (*) |
| PUT | /api/actas/{id}/notas | Guardar lote de notas (transacción atómica) | Titular, JTP |
| POST | /api/actas/{id}/cerrar | Oficializar el acta | Solo Titular |
| POST | /api/actas/{id}/reabrir | Reabrir acta cerrada (requiere motivo) | Solo Admin |
| GET | /api/actas/{id}/auditoria | Consultar log de auditoría del acta | Solo Admin |

(*) Estudiante: solo cuando el acta está "cerrada" y el estudiante pertenece a esa acta. Recibe únicamente sus propias notas.

### Payload — PUT /api/actas/{id}/notas

```json
{
  "notas": [
    { "estudiante_id": 1, "valor": 7.50, "ausente": false },
    { "estudiante_id": 2, "valor": null, "ausente": true },
    { "estudiante_id": 3, "valor": null, "ausente": false }
  ]
}
```

### Respuesta de error de validación — HTTP 422

```json
{
  "message": "El lote de notas contiene errores. No se guardó ningún cambio.",
  "errors": {
    "notas.0.valor": ["El valor debe estar entre 0 y 10."],
    "notas.1": ["No puede especificar valor numérico y ausente = true simultáneamente."]
  }
}
```

### Payload — POST /api/actas/{id}/reabrir

```json
{
  "motivo": "Error de carga detectado por el titular. Autorizado por Director."
}
```

---

## 6. Validaciones por Campo

| Campo | Regla | Mensaje de error |
|---|---|---|
| valor | DECIMAL(4,2), rango [0.00, 10.00] | "El valor debe ser numérico entre 0.00 y 10.00." |
| ausente | BOOLEAN | "El campo ausente debe ser true o false." |
| valor + ausente | Mutuamente excluyentes | "No puede registrarse valor numérico y ausente = true simultáneamente." |
| instancia | Debe pertenecer al ENUM definido | "La instancia especificada no es válida." |
| acta estado (escritura) | Debe estar "abierta" | "El acta está cerrada y no admite modificaciones." |
| estudiante_id en lote | Debe pertenecer al acta | "El estudiante no pertenece a esta acta." |
| motivo (reapertura) | No nulo, no vacío | "El motivo de reapertura es obligatorio." |
| catedra_id + periodo_id + instancia | Unicidad | "Ya existe un acta para esta instancia en el período activo." |

---

## 7. Criterios de Aceptación

**CA-01 — Carga exitosa de notas**
Dado que el docente titular o JTP está autenticado y accede a un acta "abierta",
Cuando envía un lote de notas con valores válidos (numéricos en [0, 10] o ausente = true),
Entonces el sistema persiste todos los cambios en una transacción atómica, registra en auditoría el ID del docente y la marca temporal, y retorna HTTP 200.

**CA-02 — Rechazo de nota fuera de rango**
Dado que el docente envía un lote donde una o más notas tienen valor fuera de [0, 10] o formato inválido,
Cuando el backend recibe el payload,
Entonces el sistema revierte la transacción completa, no persiste ningún cambio, retorna HTTP 422 y lista los errores por índice de fila.

**CA-03 — Cierre de acta por titular**
Dado que el docente titular confirma "Cerrar Acta",
Entonces el sistema cambia el estado a "cerrada", registra fecha_cierre y cerrada_por_docente_id, recalcula condición de todos los estudiantes en Legajos dentro de la misma transacción, y retorna HTTP 200.

**CA-04 — Bloqueo de cierre para JTP**
Dado que un JTP intenta POST /api/actas/{id}/cerrar,
Entonces el sistema retorna HTTP 403: "Solo el docente titular puede oficializar el acta." El estado del acta no cambia.

**CA-05 — Inmutabilidad de acta cerrada**
Dado que un acta tiene estado "cerrada",
Cuando cualquier usuario envía PUT /api/actas/{id}/notas,
Entonces el sistema retorna HTTP 403. No se persiste ningún cambio.

**CA-06 — Reapertura por Administrador**
Dado que el Administrador ingresa motivo no vacío al reabrir un acta cerrada,
Entonces el sistema cambia el estado a "abierta", registra en auditoría ID, timestamp y motivo, y las notas existentes permanecen intactas. Retorna HTTP 200.

**CA-07 — Visibilidad de notas para estudiantes**
Cuando el acta está "abierta" → estudiante recibe HTTP 403.
Cuando el acta está "cerrada" → estudiante recibe HTTP 200 con solo sus propias notas.

**CA-08 — Unicidad de acta por instancia**
Dado que ya existe acta para cátedra + período + instancia,
Cuando se intenta crear otra con la misma combinación,
Entonces el sistema retorna HTTP 409: "Ya existe un acta para esta instancia en el período activo."

**CA-09 — Atomicidad del cierre con fallo en Legajo**
Cuando durante el recálculo de condición de un estudiante en Legajos ocurre un error,
Entonces el sistema revierte la transacción completa: acta permanece "abierta", ningún Legajo se actualiza, retorna HTTP 500.

**CA-10 — Registro de auditoría en todas las acciones**
Cuando cualquier usuario realiza una acción exitosa (carga, modificación, cierre, reapertura),
Entonces se registra en auditoria_acta: tipo de acción, ID del usuario, timestamp, y detalle JSON con valores anteriores/nuevos.

---

## 8. Casos Borde

**CB-01 — Nota 0.00 es válida:** Valor numérico válido (reprobado), no equivale a NULL.

**CB-02 — Nota 10.00 es válida:** El valor máximo exacto debe aceptarse sin error.

**CB-03 — Cierre con notas pendientes:** El docente puede cerrar un acta con estudiantes en estado NULL/no registrado. El sistema lo permite; esos estudiantes quedan como "sin evaluación" en el Legajo.

**CB-04 — Ausente y valor simultáneos:** Si el payload incluye ausente = true y valor numérico para el mismo estudiante, el backend rechaza esa fila y el lote completo.

**CB-05 — JTP sin asignación a la cátedra:** Un JTP no asignado a esa cátedra recibe HTTP 403 en cualquier intento de escritura, aunque el acta esté abierta.

**CB-06 — Recuperatorio sin acta original previa:** Puede existir acta de recuperatorio aunque no exista acta del parcial original. El sistema no bloquea esta situación.

**CB-07 — Edición concurrente por múltiples JTPs:** El commit que llega primero prevalece. No hay bloqueo optimista en esta versión. Cada guardado exitoso genera su propio registro de auditoría.

**CB-08 — Reaperturas sucesivas:** Un acta puede ser cerrada y reabierta múltiples veces. Cada ciclo queda registrado independientemente en auditoría. Sin límite de ciclos.

**CB-09 — Acta sin inscriptos:** Si la cátedra no tiene estudiantes inscriptos, el sistema crea el acta con grilla vacía. No es un error. El docente puede cerrarla.

**CB-10 — Creación sin período activo:** El backend retorna HTTP 422 con mensaje claro. No genera error 500 ni comportamiento indefinido.

---

## 9. Fuera de Alcance

1. Exportación del acta a PDF o planilla de cálculo (Excel/CSV).
2. Notificaciones automáticas ante cierre, reapertura o carga de notas.
3. Configuración del umbral de aprobación por cátedra o período. El valor 5/10 es fijo institucional.
4. Alta o baja manual de estudiantes en el acta una vez generada.
5. Bloqueo optimista ante edición concurrente del mismo acta por múltiples JTPs.
6. Versionado histórico completo con snapshots del acta.
7. Instancias de acta personalizadas fuera del ENUM predefinido.
8. Integración con sistemas externos (SIU Guaraní u otros) en esta versión.

---

## 10. Asunciones Validadas

1. **DECIMALES** — Notas hasta 2 decimales (ej: 7.50), sin redondeo automático.
2. **VALORES ESPECIALES** — Solo "Ausente" como valor no numérico, además del rango 0-10.
3. **ESTRUCTURA DEL ACTA** — Acta separada por instancia + cátedra + período.
4. **QUIEN CIERRA** — Solo docente titular oficializa. JTP solo carga y edita.
5. **REAPERTURA** — Solo Administrador puede reabrir acta cerrada, con motivo obligatorio registrado en auditoría.
6. **CONDICIÓN ESTUDIANTE** — Al cerrar, recalcula Regular/Libre y actualiza Legajos dentro de la misma transacción.
7. **UMBRAL APROBACIÓN** — Mínimo 5.00/10.00, valor fijo institucional. No configurable.
8. **VISIBILIDAD ESTUDIANTES** — Notas visibles para estudiantes solo con acta "cerrada".
9. **MÚLTIPLES JTPS** — Una cátedra puede tener varios JTPs con los mismos permisos de carga.
10. **RECUPERATORIOS** — Instancia separada, no sobreescribe la original. Docente decide qué nota prevalece al cierre.
11. **ESTUDIANTES EN ACTA** — Generados automáticamente desde inscriptos. Sin alta/baja manual.
12. **ESTADO INICIAL** — Acta nueva en estado "abierta" con todos los campos de nota vacíos.
13. **NOTIFICACIONES** — Sin notificaciones automáticas en esta versión.
14. **EXPORTACIÓN** — Sin PDF ni planilla en esta versión.
15. **PERÍODO ACADÉMICO** — Acta siempre ligada al período activo configurado por el Administrador.

---

## 11. Notas para Implementación

- Control de acceso por cátedra → implementar en **Laravel Policy**, no en el controlador.
- Transacción del cierre → `DB::transaction()` que envuelve escritura en `actas` + todas las escrituras en Legajos. Si falla alguna, rollback completo.
- `auditoria_acta` → el usuario de base de datos de la aplicación no debe tener permisos de UPDATE ni DELETE sobre esta tabla.
- GET /api/actas/{id} → aplicar scope diferente según rol: Estudiante ve solo su fila; Titular/JTP/Admin ven la grilla completa.
