# CHANGELOG_PROJECT -- humanOS Native Android

> Registro cronologico de cambios significativos en el proyecto. Cada entrada incluye fecha, descripcion y referencias a decisiones o tareas asociadas.

---

## 2026-06-06

### Proyecto inicializado

- Creado repositorio `humanos-android` en `C:\Users\felip\Claude\humanos-android\`
- Creada carpeta `docs/00_PROJECT_CONTROL/` con estructura de control de proyecto
- Documentos creados:
  - `CURRENT_STATE.md` -- estado inicial del proyecto
  - `CHANGELOG_PROJECT.md` -- este archivo
  - `TASKS.md` -- 10 tareas iniciales (TASK-001 a TASK-010)
  - `DECISIONS_LOG.md` -- 10 decisiones fundacionales (DEC-001 a DEC-010)
  - `OPEN_QUESTIONS.md` -- 4 preguntas abiertas (Q-001 a Q-004)
  - `RISKS.md` -- 7 riesgos identificados (RISK-001 a RISK-007)
  - `TRACEABILITY_MATRIX.md` -- 12 requisitos trazados (REQ-*-001)
- Fase de planificacion Phase 1 completada en sesiones previas
- Inspecciones READ-ONLY de HumanOS y QueBot completadas
- Decisiones clave registradas: proyecto independiente, Kotlin/Compose/Hilt/Room, 14 modulos, auth dual-token, compileSdk 36 preferido
- Refs: DEC-001 a DEC-010, TASK-001

---

## 2026-06-07

### Tanda 3: QA documental + .gitattributes (commit 2a28a10)

- Creado `.gitattributes` para normalizar line endings (LF para texto, binary para imagenes)
- QA documental completo: 10 checks cruzados, todos PASS
- Refs: TASK-011, TASK-012

### Tanda 3B: Correcciones de consistencia (commit 4d47a4b)

- DEC-011 creada: core-observability entra en Phase 1 (15 modulos)
- DEC-006 actualizada de 14 a 15 modulos
- MOD-015 (core-observability) agregado a MODULE_MAP.md
- TASK-001, TASK-002 marcados DONE
- TASK-011 a TASK-017 creados
- Q-005 (titularidad/licencia) agregada
- RISK-008 (MIT sin claridad IP) agregado
- DOCS_CONSISTENCY_REPORT.md creado
- Refs: DEC-011, TASK-013 a TASK-017

### Tanda 4: Proyecto Android Gradle (commit c03af1e)

- Creado proyecto Gradle multi-modulo completo con 15 modulos Phase 1
- Convention plugins en build-logic/ (6 plugins)
- Version catalog en gradle/libs.versions.toml
- compileSdk 36, minSdk 26, targetSdk 36, JDK 21
- applicationId provisional: eco.humanos.android.dev
- Material 3 theme con brand color #0A3D62
- HumanosApp + MainActivity skeleton
- 70 archivos, 1,532 lineas
- Refs: TASK-003, TASK-004, DEC-002, DEC-006, DEC-010, DEC-011

### Tanda 5: Modelos Kotlin (commit 22f00be)

- 27 modelos Kotlin en core-model organizados en 7 packages
- context/: ContextNode, ContextEdge, ContextNodeType, GovernanceState
- capture/: CaptureItem, CaptureType, ProcessingStatus
- task/: TaskItem, TaskStatus, TaskPriority, EntityOrigin
- health/: HumanState, EnergySignal, EnergySignalType, RoutineSignal, RoutineSignalType
- terrain/: FieldInspection, EvidenceItem, GeoPoint
- auth/: AuthState (sealed interface), HumanOSSession
- common/: IntegrationSource, PrivacyLevel, SyncStatus, AiExecutionMode, SourceReference, TraceEvent, PermissionCapability
- Refs: TASK-005, DEC-002, DEC-009

### Tanda 6: Interfaces + Gateways + Navegacion (commit 116adfc)

- AuthRepository interface (data-auth): 6 metodos
- TraceRepository interface (core-observability): logEvent, query, prune
- HumanosGateway interface + FakeHumanosGateway (integration-humanos)
- QuebotGateway interface + FakeQuebotGateway con SSE simulado (integration-quebot)
- CaptureDao stub (core-database)
- Navigation Compose: TopLevelDestination, HumanosNavHost, HumanosApp scaffold
- Feature screens mejoradas: Dashboard (cards), Capture (form), Settings (ListItems)
- SseEvent sealed class con 5 tipos
- 20 archivos, 807 lineas
- Refs: TASK-006, TASK-008, DEC-003

### Tanda 7: Sincronizacion de documentacion

- CURRENT_STATE.md reescrito completamente con estado real
- CHANGELOG_PROJECT.md actualizado con Tandas 3-7
- TASKS.md actualizado con estados reales de TASK-003 a TASK-008
- TRACEABILITY_MATRIX.md actualizado con estados reales de requisitos
- Refs: DEC-004

---

## Formato de entradas futuras

```
## YYYY-MM-DD

### Titulo breve del cambio

- Descripcion concreta de lo que cambio
- Archivos o modulos afectados
- Refs: DEC-NNN, TASK-NNN, RISK-NNN (si aplica)
```
