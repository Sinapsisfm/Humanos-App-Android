# TASKS -- humanOS Native Android

> Registro de tareas del proyecto. Cada tarea tiene un ID unico, estado, descripcion, y referencias cruzadas.

## Estados

- `[PENDING]` -- No iniciada
- `[IN_PROGRESS]` -- En curso
- `[BLOCKED]` -- Bloqueada por dependencia
- `[DONE]` -- Completada
- `[CANCELLED]` -- Descartada

---

## Tareas activas

### TASK-001: Crear estructura docs completa con contenido real

- **Estado:** `[DONE]` (Tanda 2, commit 277d44c)
- **Prioridad:** Alta
- **Descripcion:** Crear todos los archivos de documentacion en `docs/00_PROJECT_CONTROL/` con contenido real, no placeholders. Incluye CURRENT_STATE, CHANGELOG, TASKS, DECISIONS_LOG, OPEN_QUESTIONS, RISKS, TRACEABILITY_MATRIX.
- **Criterio de completitud:** Los 7 archivos existen con contenido sustantivo y referencias cruzadas consistentes.
- **Dependencias:** Ninguna
- **Refs:** DEC-004

---

### TASK-002: Crear ADRs 0001-0004

- **Estado:** `[DONE]` (Tanda 2, commit 277d44c)
- **Prioridad:** Alta
- **Descripcion:** Crear Architecture Decision Records fundacionales:
  - ADR-0001: Proyecto independiente con integracion via gateways
  - ADR-0002: Stack tecnologico (Kotlin/Compose/Hilt/Room/DataStore/WorkManager)
  - ADR-0003: Arquitectura modular y definicion de 14 modulos Phase 1
  - ADR-0004: Estrategia de autenticacion dual-token
- **Criterio de completitud:** 4 archivos ADR en `docs/01_ADR/` con formato estandar (contexto, decision, consecuencias, status).
- **Dependencias:** TASK-001 (documentacion base)
- **Refs:** DEC-001, DEC-002, DEC-006, DEC-007

---

### TASK-003: Crear proyecto Gradle con build-logic convention plugins

- **Estado:** `[DONE]` (Tanda 4, commit c03af1e)
- **Prioridad:** Alta
- **Descripcion:** Crear la estructura Gradle raiz del proyecto Android:
  - `settings.gradle.kts` con version catalogs
  - `build.gradle.kts` raiz
  - `gradle/libs.versions.toml` con todas las dependencias
  - `build-logic/` con convention plugins para Android library, Android app, Compose, Hilt
  - `.editorconfig`, `.gitignore` Android
  - `gradle.properties` con configuracion de proyecto
- **Criterio de completitud:** `./gradlew tasks` ejecuta sin error (aunque no haya modulos aun).
- **Dependencias:** TASK-002 (ADRs documentan decisiones de stack)
- **Refs:** DEC-002, DEC-010

---

### TASK-004: Crear los 15 modulos Phase 1

- **Estado:** `[DONE]` (Tanda 4, commit c03af1e)
- **Prioridad:** Alta
- **Descripcion:** Crear los 14 modulos definidos en DEC-006 como modulos Gradle vacios con `build.gradle.kts` y dependencias correctas entre ellos:
  - `app` (Android Application)
  - `core-model` (Kotlin library, sin Android deps)
  - `core-database` (Android library, Room)
  - `core-datastore` (Android library, DataStore)
  - `core-network` (Android library, Retrofit/OkHttp)
  - `core-security` (Android library, Keystore)
  - `core-ui` (Android library, Compose)
  - `data-auth` (Android library)
  - `feature-dashboard` (Android library, Compose)
  - `feature-capture` (Android library, Compose + CameraX)
  - `feature-settings` (Android library, Compose)
  - `integration-humanos` (Android library)
  - `integration-quebot` (Android library)
  - `testing-common` (JVM library)
- **Criterio de completitud:** `./gradlew assemble` compila todos los modulos sin error.
- **Dependencias:** TASK-003 (Gradle skeleton)
- **Refs:** DEC-006

---

### TASK-005: Crear modelos Kotlin en core-model

- **Estado:** `[DONE]` (Tanda 5, commit 22f00be)
- **Prioridad:** Alta
- **Descripcion:** Definir los data classes y sealed interfaces centrales del dominio en `core-model`:
  - `CaptureItem` (captura universal: texto, foto, audio, archivo)
  - `Task` (tarea inteligente con SourceReference)
  - `SourceReference` (trazabilidad de origen: modulo, timestamp, tipo)
  - `TraceEvent` (evento de observabilidad/auditoria)
  - `UserProfile` (perfil local del usuario)
  - `AuthState` (sealed interface: Unauthenticated, Authenticated, TokenExpired)
  - `SyncStatus` (enum: PENDING, SYNCING, SYNCED, FAILED)
- **Criterio de completitud:** Modelos compilan, tienen KDoc, y `testing-common` puede referenciarlos.
- **Dependencias:** TASK-004 (modulos creados)
- **Refs:** DEC-002, DEC-009

---

### TASK-006: Crear interfaces (repositories + gateways)

- **Estado:** `[DONE]` (Tanda 6, commit 116adfc)
- **Prioridad:** Media
- **Descripcion:** Definir las interfaces de repositorio y gateway que separan dominio de implementacion:
  - `AuthRepository` (login, logout, token refresh, auth state flow)
  - `CaptureRepository` (save, list, delete captures)
  - `TaskRepository` (CRUD tareas, query por estado)
  - `HumanosGateway` (sync profile, push events -- read-only Phase 1)
  - `QuebotGateway` (send message, get response -- read-only Phase 1)
  - `ObservabilityGateway` (log TraceEvent, query audit trail)
- **Criterio de completitud:** Interfaces compilan en sus modulos respectivos. Sin implementaciones aun (salvo mocks en `testing-common`).
- **Dependencias:** TASK-005 (modelos que las interfaces referencian)
- **Refs:** DEC-003, DEC-009

---

### TASK-007: Crear Room database + DAOs en core-database

- **Estado:** `[PENDING]`
- **Prioridad:** Media
- **Descripcion:** Implementar la base de datos Room local:
  - `HumanosDatabase` (abstract class, Room database)
  - Entities: `CaptureEntity`, `TaskEntity`, `TraceEventEntity`
  - DAOs: `CaptureDao`, `TaskDao`, `TraceEventDao`
  - Type converters para Instant, enums, JSON blobs
  - Migraciones vacias (schema version 1)
- **Criterio de completitud:** Database compila, schema se exporta a `schemas/`, DAOs tienen queries basicos (insert, query by id, query all, delete).
- **Dependencias:** TASK-005 (modelos que las entities mapean)
- **Refs:** DEC-002

---

### TASK-008: Crear pantallas skeleton Compose + navegacion

- **Estado:** `[DONE]` (Tanda 6, commit 116adfc)
- **Prioridad:** Media
- **Descripcion:** Crear pantallas Compose minimas con navegacion funcional:
  - `DashboardScreen` (feature-dashboard) -- pantalla principal con lista de captures y tareas
  - `CaptureScreen` (feature-capture) -- pantalla de captura con boton FAB
  - `SettingsScreen` (feature-settings) -- preferencias basicas
  - Navegacion con `NavHost` + bottom navigation (3 tabs: Dashboard, Capture, Settings)
  - Theme Material 3 con Dynamic Color
- **Criterio de completitud:** App compila y muestra navegacion funcional entre 3 pantallas (contenido placeholder OK, pero estructura de composables real).
- **Dependencias:** TASK-004 (modulos feature), TASK-005 (modelos para preview data)
- **Refs:** DEC-002

---

### TASK-009: Confirmar applicationId definitivo antes de primera beta

- **Estado:** `[PENDING]`
- **Prioridad:** Alta (bloqueante para beta)
- **Descripcion:** El `applicationId` (package name) que se registre en Firebase y Google Play es **permanente** y no se puede cambiar despues del primer upload. Necesita confirmacion explicita de Felipe.
  - Supuesto sugerido: `eco.humanos.android`
  - Alternativas consideradas: `com.sinapsis.humanos`, `app.humanos.android`
  - Criterio: debe alinearse con dominio `humanos.eco`, ser profesional, no colisionar en Play Store
- **Criterio de completitud:** Felipe confirma applicationId. Se actualiza `app/build.gradle.kts` y se registra en Firebase Console.
- **Dependencias:** Ninguna tecnica. Requiere decision de Felipe.
- **Refs:** DEC-010, RISK-007, Q-003

---

### TASK-010: Disenar contrato POST /api/auth/mobile/exchange (documentar, no implementar)

- **Estado:** `[PENDING]`
- **Prioridad:** Media
- **Descripcion:** Documentar el contrato del endpoint que HumanOS deberia exponer para intercambiar un Firebase ID token por un HumanOS bridge JWT:
  - Request: `POST /api/auth/mobile/exchange` con header `Authorization: Bearer <firebase-id-token>`
  - Response: `{ "humanosToken": "<jwt>", "expiresAt": "<iso8601>", "personId": "<uuid>" }`
  - Error cases: 401 (token invalido), 403 (usuario no existe en HumanOS), 429 (rate limit)
  - Scopes/claims del bridge JWT
  - Este contrato se documenta en este repo como referencia. La implementacion ocurre en el repo `humanos-eco`, no aqui.
- **Criterio de completitud:** Documento en `docs/` con contrato completo. No hay codigo ejecutable.
- **Dependencias:** Ninguna
- **Refs:** DEC-007, DEC-008, RISK-001

---

### TASK-011: QA documental — verificacion consistencia cruzada

- **Estado:** `[DONE]` (Tanda 3/3B, commit 2a28a10)
- **Prioridad:** Alta
- **Descripcion:** Verificar consistencia cruzada entre los 38 docs creados por agentes paralelos. Checks: MODULE_MAP vs TRACEABILITY_MATRIX, DECISIONS_LOG vs ADRs, TASKS vs CURRENT_STATE, RISKS vs SECURITY_PRIVACY, Integration docs consistency, compileSdk consistency, applicationId consistency, Firebase project ID consistency.
- **Criterio de completitud:** DOCS_CONSISTENCY_REPORT.md creado con resultados.
- **Refs:** DEC-004

---

### TASK-012: Agregar .gitattributes

- **Estado:** `[DONE]` (Tanda 3, commit 2a28a10)
- **Prioridad:** Media
- **Descripcion:** Agregar .gitattributes para normalizar line endings (LF) en archivos de texto y marcar binarios.
- **Refs:** DEC-002

---

### TASK-013: Incorporar core-observability a Phase 1

- **Estado:** `[DONE]` (Tanda 3B)
- **Prioridad:** Alta
- **Descripcion:** Actualizar Phase 1 de 14 a 15 modulos incluyendo core-observability. Actualizar DEC-006, MODULE_MAP, TRACEABILITY_MATRIX.
- **Refs:** DEC-011, DEC-009

---

### TASK-014: Crear DOCS_CONSISTENCY_REPORT.md

- **Estado:** `[DONE]` (Tanda 3B)
- **Prioridad:** Alta
- **Descripcion:** Crear informe de QA documental con resultados de verificacion cruzada.
- **Refs:** TASK-011

---

### TASK-015: Agregar Q-005 titularidad/licencia

- **Estado:** `[DONE]` (Tanda 3B)
- **Prioridad:** Media
- **Descripcion:** Registrar pregunta sobre titularidad del codigo y licencia (MIT vs propietaria).
- **Refs:** RISK-008

---

### TASK-016: Agregar RISK-008 titularidad IP

- **Estado:** `[DONE]` (Tanda 3B)
- **Prioridad:** Media
- **Descripcion:** Registrar riesgo de licencia MIT publicada sin claridad sobre propiedad intelectual.
- **Refs:** Q-005

---

### TASK-017: Actualizar CURRENT_STATE.md y CHANGELOG tras Tanda 3B

- **Estado:** `[DONE]` (Tanda 3B)
- **Prioridad:** Alta
- **Descripcion:** Actualizar estado actual del proyecto y changelog con resultados de QA y correcciones.
- **Refs:** DEC-004

---

## Resumen de estado

| ID | Descripcion | Estado | Prioridad |
|----|-------------|--------|-----------|
| TASK-001 | Crear estructura docs completa | `[DONE]` | Alta |
| TASK-002 | Crear ADRs 0001-0004 | `[DONE]` | Alta |
| TASK-003 | Crear proyecto Gradle con build-logic | `[DONE]` | Alta |
| TASK-004 | Crear los 15 modulos Phase 1 | `[DONE]` | Alta |
| TASK-005 | Crear modelos Kotlin en core-model | `[DONE]` | Alta |
| TASK-006 | Crear interfaces (repos + gateways) | `[DONE]` | Media |
| TASK-007 | Crear Room database + DAOs | `[PENDING]` | Media |
| TASK-008 | Crear pantallas skeleton Compose + nav | `[DONE]` | Media |
| TASK-009 | Confirmar applicationId definitivo | `[PENDING]` | Alta |
| TASK-010 | Disenar contrato mobile/exchange | `[PENDING]` | Media |
