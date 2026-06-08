# Análisis de Código — Defensa

**Fecha:** 2026-06-07  
**Proyecto:** ProyectoIntegrador — UNRC  
**Equipo:** Cruseño, Dehaes, Destefanis, Dominguez, Garais, Narvaja

---

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Web framework | Spark Java |
| ORM | ActiveJDBC |
| Base de datos | SQLite |
| Templates | Mustache |
| Lenguaje | Java |

> **Nota para la defensa:** El spec D.2 menciona Laravel/PHP como referencia arquitectónica, pero la implementación del equipo fue en Java desde el inicio del proyecto.

---

## B.1 — Gestión de Carreras y Planes de Estudio

**Archivo clave:** `src/main/java/com/is1/proyecto/service/CarreraService.java`

### Entidades implementadas

- Tabla `carreras` — campos: id, codigo, nombre, descripcion, activa, timestamps
- Tabla `planes_estudio` — campos: id, carrera_id, anio_vigencia, descripcion, duracion_anios, activo, timestamps

### Decisiones técnicas y por qué

**1. Unicidad de código/nombre validada en capa de servicio con UPPER/LOWER**

```java
// CarreraService.java:670–681
private boolean codigoExiste(String codigo, int excludeId) {
    return Carrera.count("UPPER(codigo) = ?", codigo.toUpperCase()) > 0;
}
private boolean nombreExiste(String nombre, int excludeId) {
    return Carrera.count("LOWER(nombre) = ?", nombre.toLowerCase()) > 0;
}
```

SQLite no garantiza COLLATE NOCASE en todos los contextos. Se optó por control explícito en Java. Implementa RN-01 y RN-02.

---

**2. Paginación manual con LIMIT/OFFSET**

```java
// CarreraService.java:21–67
int offset = (page - 1) * PAGE_SIZE;  // PAGE_SIZE = 20
```

No hay librería de paginación. Se calcula offset y se arma lista de páginas para el template Mustache. Implementa CA-11 (20 registros por página).

---

**3. Un solo plan activo por carrera — sin baja automática del anterior (RN-04)**

```java
// CarreraService.java:360–368
long otrosActivos = PlanDeEstudio.count("carrera_id = ? AND activo = 1 AND id != ?", ...);
plan.setActivo(true);
plan.saveIt();
result.put("tieneOtroActivo", otrosActivos > 0);
```

Decisión deliberada: activar un plan nuevo NO desactiva el anterior. Un plan inactivo sigue siendo referencia para estudiantes ya inscriptos bajo él — forzar baja automática rompería esa integridad académica. El frontend muestra un aviso cuando hay más de un activo (CB-02).

---

**4. Bloqueo de crear plan en carrera inactiva (CB-08)**

```java
// CarreraService.java:307–311
if (carrera == null || !carrera.isActiva()) {
    result.put("message", "No se pueden agregar planes a una carrera inactiva.");
    return result;
}
```

---

**5. Año de vigencia validado dinámicamente**

```java
// CarreraService.java:314–318
int anioActual = Calendar.getInstance().get(Calendar.YEAR);
if (anioVigencia < 1900 || anioVigencia > anioActual + 5) { ... }
```

El límite superior no está hardcodeado — siempre es año actual + 5. Implementa RN-12.

---

### Limitación conocida

`desactivarPlan()` devuelve siempre `estudiantesCount = 0` (línea 381). El CA-08 pide mostrar cantidad de inscriptos afectados — la UI tiene el lugar preparado pero el número es stub hasta que se implemente la FK real entre estudiantes y planes.

---

## B.2 — Gestión de Materias y Asociación a Planes

**Archivo clave:** `src/main/java/com/is1/proyecto/service/MateriaService.java`

### Entidades implementadas

- Tabla `materias` — entidad global, única en el sistema
- Tabla `materias_planes` — asociación materia ↔ plan, con particularidades propias
- Tabla `correlatividades` — prerequisitos entre materias dentro de un plan

### Decisiones técnicas y por qué

**1. Materia como entidad global, particularidades en MateriaPlan**

Una misma materia (ej: "Análisis Matemático I") puede existir en múltiples planes con distinta carga horaria, cuatrimestre o carácter. En vez de duplicar el registro de materia por plan, se usa una tabla de asociación `materias_planes` con los atributos específicos de cada combinación. Implementa RN-05.

---

**2. Baja lógica, nunca física**

```java
// MateriaService.java:251–283
m.setActiva(false);
m.touch();
m.saveIt();
// avisa si tiene asociaciones activas, pero procede igual
```

Una materia puede tener historial de cursadas (notas). Eliminarla físicamente rompería integridad referencial. La baja lógica preserva ese historial. Implementa RN-06 y RN-07.

---

**3. Validación de correlatividades al editar posición de una materia en el plan**

```java
// MateriaService.java:397–421
// Verifica como ORIGEN: que la materia no quede "después" de algo que la requiere como previa
String sqlOrigen = "SELECT COUNT(*) FROM correlatividades c " +
    "JOIN materias_planes dest ON c.materia_plan_destino_id = dest.id " +
    "WHERE c.materia_plan_origen_id = ? " +
    "AND (? > dest.anio_plan OR ...)";

// Verifica como DESTINO: que no quede "antes" de sus propias correlativas
String sqlDestino = "SELECT COUNT(*) FROM correlatividades c " + ...
```

Si se mueve una materia a un año/cuatrimestre distinto, se verifica en ambas direcciones que no se rompan las relaciones de correlatividad existentes. Esto no estaba explícito en el spec pero es consecuencia lógica de la integridad del plan.

---

**4. Eliminación definitiva de asociación (extensión del spec)**

```java
// MateriaService.java:494–512
Correlatividad.delete("materia_plan_origen_id = ? OR materia_plan_destino_id = ?", mpId, mpId);
mp.delete();
```

El spec solo habla de desactivar (baja lógica). Se agregó la opción de eliminar físicamente cuando el administrador necesita limpiar un error de carga, eliminando en cascada las correlatividades relacionadas. Decisión: mayor flexibilidad operativa para el administrador.

---

**5. Filtros dinámicos en el listado de materias**

```java
// MateriaService.java:22–83
// Construye WHERE dinámicamente según parámetros recibidos:
// - incluirInactivas (solo admin)
// - planId
// - carreraId
```

Un solo método `listarMaterias()` sirve para admin (ve activas+inactivas, puede filtrar) y para docentes/estudiantes (solo activas). Implementa RN-11 y CA-16.

---

## D.1 — Asignación de Docentes a Comisiones

**Archivo clave:** `src/main/java/com/is1/proyecto/service/AsignacionDocenteService.java`

### Entidades implementadas

- Tabla `asignaciones_docentes` — campos: id, comision_id, docente_id, cargo, fecha_asignacion, asignado_por, activa

### Decisiones técnicas y por qué

**1. Búsqueda de docentes por nombre, apellido o DNI**

```java
// AsignacionDocenteService.java:18–45
"WHERE p.enabled = 1 " +
"  AND EXISTS (SELECT 1 FROM persona_roles pr WHERE pr.persona_id = p.id AND pr.rol = 'DOCENTE') " +
"  AND (LOWER(p.nombre) LIKE ? OR LOWER(p.apellido) LIKE ? OR p.dni LIKE ?)"
```

El spec menciona legajo — el sistema usa DNI como equivalente. El `EXISTS` sobre `persona_roles` garantiza que solo aparecen docentes (RN-03).

---

**2. Orden de validaciones en `asignarDocente()`**

Orden deliberado para minimizar queries innecesarias:
1. Comisión existe
2. Período activo (RN-02) — si no hay período activo, nada de lo demás importa
3. Cargo válido (RN-04) — validación barata, sin DB
4. Docente ya asignado a esa comisión (RN-07)
5. Titular duplicado (RN-05) — solo si el cargo es TITULAR

---

**3. Permisos derivados del cargo calculados en Java, no en el template**

```java
// AsignacionDocenteService.java:249–252
m.put("puedeCargarNotas", "TITULAR".equals(cargo) || "JTP".equals(cargo));
m.put("puedeCerrarActa", "TITULAR".equals(cargo));
```

El template Mustache es declarativo — solo muestra u oculta elementos según estos flags. La lógica de negocio no vive en el template. Implementa RN-09.

---

**4. RN-12 (baja bloqueada si acta cerrada) — integración con D.2**

```java
// AsignacionDocenteService.java:191–196
Comision comision = Comision.findById(ad.getComisionId());
if (comision == null || !comision.isActiva()) {
    result.put("message", "No es posible eliminar la asignación porque el acta ya fue cerrada.");
    return result;
}
```

Se verifica el estado de la comisión como proxy del estado del acta. Cuando D.2 esté completamente integrado, se puede refinar a verificar el acta directamente.

---

**5. RN-11 (advertencia por notas) — stub documentado**

```java
// AsignacionDocenteService.java:199–201
// RN-11: verificar notas (stub — tabla notas implementada en D.2)
// Por ahora siempre sin notas. Cuando D.2 exista:
// SELECT COUNT(*) FROM notas WHERE asignacion_docente_id = ? AND...
boolean tieneNotas = false;
```

El lugar en el código está preparado. La integración requiere conectar con la tabla `notas` de D.2.

---

## D.2 — Gestión de Actas de Calificaciones

**Archivo clave:** `src/main/java/com/is1/proyecto/service/ActaService.java`

Este es el servicio más complejo del proyecto.

### Entidades implementadas

- Tabla `actas` — campos: id, comision_id, periodo_id, instancia (ENUM), estado, fecha_cierre, cerrada_por_docente_id, reabierta_por_admin_id, fecha_reapertura, timestamps
- Tabla `notas` — campos: id, acta_id, estudiante_id, valor (DECIMAL), ausente (BOOLEAN), cargada_por_docente_id, ultima_modificacion_por, timestamps
- Tabla `auditoria_acta` — append-only, campos: id, acta_id, accion, realizado_por, detalle (JSON), motivo, created_at

### Invariante de consistencia valor/ausente

```
valor IS NULL  + ausente = false  → sin registrar (estado inicial)
valor NOT NULL + ausente = false  → nota numérica válida
valor IS NULL  + ausente = true   → estudiante ausente
valor NOT NULL + ausente = true   → INVÁLIDO — rechazado
```

### Decisiones técnicas y por qué

**1. Transacciones atómicas en tres operaciones críticas**

```java
// Crear acta: Base.openTransaction() / commitTransaction() / rollbackTransaction()
// ActaService.java:62–98

// Guardar lote de notas:
// ActaService.java:332–379

// Cerrar acta (incluye recálculo de condición de todos los estudiantes):
// ActaService.java:409–447
```

Si cualquier parte del cierre falla (por ejemplo, un UPDATE de inscripción_comision), el acta permanece "abierta" y ningún cambio queda a medias. Implementa RN-12 y RN-18.

---

**2. Validación de lote completo ANTES de abrir transacción**

```java
// ActaService.java:290–330
// Primero: iterar todo el payload, acumular errores
Map<String, List<String>> errors = new HashMap<>();
for (int i = 0; i < notasPayload.size(); i++) {
    // validar cada fila...
}
if (!errors.isEmpty()) {
    // retornar errores SIN abrir transacción
    return result;
}
// Solo entonces: Base.openTransaction()
```

Separa la validación de la persistencia. Si hay un error en la fila 50 de 100, no se persiste nada desde el inicio. Implementa RN-18.

---

**3. Lógica de selección de estudiantes por instancia**

```java
// ActaService.java:696–714
private List<Integer> getEstudiantesParaInstancia(int comisionId, int periodoId, String instancia) {
    switch (instancia) {
        case "parcial_1": case "parcial_2": case "tp":
            return todosInscriptos(comisionId);
        case "recuperatorio_1":
            return noAprobaronInstancia(comisionId, periodoId, "parcial_1");
        case "recuperatorio_2":
            return noAprobaronBloque2(comisionId, periodoId);
        case "examen_final":
            return aprobaronAmbosBloques(comisionId, periodoId);
    }
}
```

Al crear el acta, el sistema decide automáticamente qué estudiantes incluir según la instancia. Esto no es manual — reduce errores operativos del docente.

---

**4. Valor especial `-1.0` para "ausente" en mapas internos**

```java
// ActaService.java:873–892
double nota = ausente ? -1.0 : (v != null ? ((Number) v).doubleValue() : 0.0);
result.put(estId, nota);
```

Internamente, ausente = -1.0 para distinguir tres estados distintos en los mapas:
- `null` → el estudiante no estaba en esa acta
- `-1.0` → estuvo pero fue ausente (cuenta como reprobado)
- `>= 0.0` → nota numérica

---

**5. Cálculo de condición global al cierre**

```java
// ActaService.java:838–863
private String calcularCondicionGlobal(int comisionId, int periodoId, int estId) {
    // 1. Si tiene examen_final cerrado: decisión final (APROBADO o REPROBADO)
    // 2. Si aprobó bloque1 (parcial_1 o recuperatorio_1) Y bloque2 (promedio tp+parcial_2): APROBADO
    // 3. Si no: ACTIVA (aún en curso)
}
```

La condición no se calcula por acta individual sino considerando **todas** las actas cerradas de la comisión para ese estudiante. Implementa RN-12.

**Nota bloque 2:** `(nota_tp + nota_parcial_2) / 2.0` si hay TP, sino solo `nota_parcial_2`.

---

**6. Visibilidad diferenciada por rol en `getActaDetalle()`**

```java
// ActaService.java:184–215
boolean esEstudiante = roles.contains("ESTUDIANTE") && !roles.contains("DOCENTE")
                        && !roles.contains("ADMINISTRADOR");

if (esEstudiante && !esCerrada) {
    // HTTP 403 — notas no visibles mientras el acta esté abierta
}

if (esEstudiante) {
    // query filtrada: solo sus propias notas
} else {
    // query completa: toda la grilla
}
```

Implementa RN-13. El rol dual (una persona puede ser docente y estudiante) se maneja verificando que no tenga también rol de mayor privilegio.

---

**7. `auditoria_acta` append-only**

Toda acción usa `AuditoriaActa.registrar()` que hace INSERT. El spec dice que esta tabla no admite UPDATE ni DELETE. En código nunca se modifica — el constraint de DB debe complementar esto. Implementa RN-19.

---

## Preguntas probables en la defensa

**¿Por qué no se desactiva automáticamente el plan anterior al activar uno nuevo?**
> Decisión deliberada del dominio: un plan inactivo sigue siendo referencia para estudiantes inscriptos bajo él. Forzar baja automática rompería esa integridad. El administrador decide cuándo y si desactivar.

**¿Cómo garantizan la integridad del cierre de acta si falla algo a mitad?**
> Todo el cierre (UPDATE actas + UPDATE inscripciones_comisiones por cada estudiante) está dentro de una sola transacción `DB::transaction`. Si cualquier UPDATE falla, rollback completo y el acta queda en estado "abierta".

**¿Por qué baja lógica y no física en materias?**
> Una materia puede tener historial de cursadas (notas) de estudiantes. Eliminarla físicamente rompería la integridad referencial. La baja lógica preserva ese historial.

**¿Cómo funciona el cálculo de quién va al recuperatorio?**
> Al crear el acta de recuperatorio_1, se consultan las actas cerradas de parcial_1. Van quienes tuvieron menos de 5, fueron ausentes, o no aparecían en esa acta. Para recuperatorio_2, se evalúa el promedio del bloque 2 (TP + Parcial 2).

**¿Cómo se resuelve el caso de dos JTPs editando el mismo acta simultáneamente?**
> El spec lo define en CB-07: el último commit prevalece, sin bloqueo optimista. Es una limitación conocida y documentada como fuera de alcance para esta versión.

**¿Cómo sabe el sistema qué permisos tiene el docente sobre un acta?**
> Los permisos se derivan del cargo en `asignaciones_docentes`. Titular: puede cargar notas y cerrar. JTP: solo cargar notas. Ayudante: solo consulta. Se verifica en el backend con queries a esa tabla, no se confía en el frontend.

---

## Resumen de reglas de negocio implementadas vs. spec

| Historia | RN | Estado |
|---|---|---|
| B.1 | RN-01 a RN-12 | Implementadas. Excepción: RN-10 (conteo de estudiantes en desactivar plan) es stub (siempre 0). |
| B.2 | RN-01 a RN-13 | Implementadas. `eliminarAsociacionDefinitiva()` es extensión del spec. |
| D.1 | RN-01 a RN-14 | Implementadas. RN-11 (advertencia por notas al dar baja) es stub. |
| D.2 | RN-01 a RN-20 | Implementadas. RN-20 (sin notificaciones) trivialmente cumplido. |
