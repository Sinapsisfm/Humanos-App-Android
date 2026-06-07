# TRACEABILITY_MATRIX -- humanOS Native Android

> Matriz de trazabilidad de requisitos. Conecta cada requisito con su modulo, fase, decisiones asociadas, riesgos, y proxima accion concreta.

## Fases

- **Phase 1:** Skeleton + modelos + mocks + navegacion basica
- **Phase 2:** Context engine, observabilidad, permisos granulares, integraciones reales
- **Phase 3:** IA local, Health Connect, terreno, captura avanzada

---

## Matriz

| ID | Area | Requisito | Modulo(s) | Estado | Fuente | Decision | Riesgo | Next Action |
|----|------|-----------|-----------|--------|--------|----------|--------|-------------|
| REQ-CTX-001 | Context Engine | Context Engine personal: agregar, relacionar, y consultar contexto del usuario (notas, archivos, conversaciones) con relevancia temporal | `core-model`, `data-context` | Phase 2 -- no iniciado | Spec humanOS producto | DEC-002 | -- | Definir modelo `ContextEntry` en Phase 2 planning |
| REQ-CAP-001 | Captura | Captura universal multimodal: texto libre, foto (camara), audio (microfono), archivo adjunto. Cada captura tiene `SourceReference` de trazabilidad | `feature-capture`, `data-capture` | Phase 1 -- skeleton pendiente | Spec humanOS producto | DEC-002 | RISK-003 | TASK-008: crear `CaptureScreen` skeleton |
| REQ-TASK-001 | Tareas | Tareas inteligentes con `SourceReference` que indica origen (captura, integracion, manual). CRUD basico + query por estado | `core-model`, `data-tasks` | Phase 1 -- modelo pendiente | Spec humanOS producto | DEC-002 | -- | TASK-005: crear `Task` data class con `SourceReference` |
| REQ-OBS-001 | Observabilidad | `TraceEvent` para auditoria local: quien hizo que, cuando, desde donde. Log inmutable con retencion configurable | `core-model` (Phase 1), `core-observability` (Phase 2) | Phase 1 -- modelo pendiente | DEC-009 | DEC-009 | TASK-005: crear `TraceEvent` data class |
| REQ-HEALTH-001 | Salud | Lectura de datos de Health Connect (pasos, sueno, frecuencia cardiaca) como senales de estado humano. Solo lectura, no escritura | `integration-healthconnect` | Phase 3 -- no iniciado | Spec humanOS producto | DEC-002 | RISK-005 | Sin accion hasta Phase 3 planning |
| REQ-TERRAIN-001 | Terreno | Asistente de terreno para inspecciones fisicas: checklist, fotos geolocalizadas, firma digital, reporte offline | `feature-terrain`, `data-terrain` | Phase 3 -- no iniciado | Spec empresa.eco | DEC-002 | -- | Sin accion hasta Phase 3 planning |
| REQ-AI-001 | IA Local | Inferencia on-device via ONNX Runtime Mobile (clasificacion, NER). Gateway pattern con fallback a remote | `core-ai`, `integration-local-ai` | Phase 3 -- no iniciado | Spec humanOS producto | DEC-002 | RISK-004 | Sin accion hasta Phase 3 planning |
| REQ-SEC-001 | Seguridad | Boveda privada: almacenamiento cifrado en Android Keystore, tokens never-in-plaintext, privacy-first por defecto | `core-security` | Phase 1 -- modulo pendiente | DEC-004 | DEC-004 | TASK-004: crear modulo `core-security` |
| REQ-HUMANOS-001 | Integracion | Integracion con HumanOS via `HumanosGateway` interface. Phase 1 read-only con mocks. Phase 2 sync real | `integration-humanos` | Phase 1 -- mock pendiente | DEC-003 | DEC-003 | RISK-001 | TASK-006: crear `HumanosGateway` interface |
| REQ-QUEBOT-001 | Integracion | Integracion con QueBot via `QuebotGateway` interface. Phase 1 read-only con mocks. Phase 2 mensajes reales | `integration-quebot` | Phase 1 -- mock pendiente | DEC-003 | DEC-003 | RISK-001 | TASK-006: crear `QuebotGateway` interface |
| REQ-AUTH-001 | Auth | Autenticacion dual-token: Firebase ID token + HumanOS bridge JWT. Sin secretos embebidos en el APK. Token storage en Android Keystore | `data-auth` | Phase 1 -- mock pendiente | DEC-007 | DEC-007 | RISK-001 | TASK-005: crear `AuthState` sealed interface |
| REQ-ANDROID-001 | Permisos | Permisos Android granulares: solicitar solo cuando se necesitan (in-context), nunca al inicio. Graceful degradation si se rechazan | `core-permissions` (Phase 2) | Phase 2 -- no iniciado | Best practices Android | DEC-002 | RISK-003 | TASK-008: implementar solicitud in-context en `CaptureScreen` |

---

## Cobertura por fase

### Phase 1 (skeleton + modelos + mocks)

| Requisito | Modulo Phase 1 | Alcance Phase 1 |
|-----------|---------------|-----------------|
| REQ-CAP-001 | `feature-capture` | Screen skeleton, boton FAB, sin captura real |
| REQ-TASK-001 | `core-model` | Data class `Task` + `SourceReference` |
| REQ-OBS-001 | `core-model` | Data class `TraceEvent` (sin infra de persistence) |
| REQ-SEC-001 | `core-security` | Modulo creado, Keystore wrapper basico |
| REQ-HUMANOS-001 | `integration-humanos` | Interface + mock implementation |
| REQ-QUEBOT-001 | `integration-quebot` | Interface + mock implementation |
| REQ-AUTH-001 | `data-auth` | `AuthState` sealed interface + mock auth flow |

### Phase 2 (integraciones reales + observabilidad)

| Requisito | Modulo Phase 2 | Alcance Phase 2 |
|-----------|---------------|-----------------|
| REQ-CTX-001 | `data-context` | Context Engine con `ContextEntry`, busqueda, relevancia |
| REQ-OBS-001 | `core-observability` | Persistence, retention, export de TraceEvents |
| REQ-ANDROID-001 | `core-permissions` | Permission manager centralizado |
| REQ-HUMANOS-001 | `integration-humanos` | Sync real con endpoint bridge |
| REQ-QUEBOT-001 | `integration-quebot` | Mensajes reales via Firebase Auth |

### Phase 3 (IA local + health + terreno)

| Requisito | Modulo Phase 3 | Alcance Phase 3 |
|-----------|---------------|-----------------|
| REQ-HEALTH-001 | `integration-healthconnect` | Lectura Health Connect, senales de estado |
| REQ-TERRAIN-001 | `feature-terrain`, `data-terrain` | Checklist offline, fotos geo, reportes |
| REQ-AI-001 | `core-ai`, `integration-local-ai` | ONNX Runtime, clasificacion, NER on-device |

---

## Dependencias entre requisitos

```
REQ-AUTH-001 (auth dual-token)
  └── REQ-HUMANOS-001 (necesita bridge JWT para sync)
  └── REQ-QUEBOT-001 (necesita Firebase token para mensajes)

REQ-CAP-001 (captura multimodal)
  └── REQ-ANDROID-001 (necesita permisos camara/mic)
  └── REQ-TASK-001 (captura genera tareas via SourceReference)
  └── REQ-OBS-001 (captura genera TraceEvents)

REQ-CTX-001 (context engine)
  └── REQ-CAP-001 (capturas alimentan contexto)
  └── REQ-TASK-001 (tareas alimentan contexto)

REQ-AI-001 (IA local)
  └── REQ-CTX-001 (IA usa contexto para inferencia)
```
