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

### Tanda 8: CI/CD + TraceEvent fix + editorconfig (commit fff39ec)

- CI workflow: .github/workflows/ci.yml (lint + tests + debug APK)
- Beta deploy template: .github/workflows/deploy-beta.yml (Firebase App Distribution, commented)
- .editorconfig para formateo consistente
- DEC-012: TraceEvent duplicado resuelto (canonico en core-model)
- Refs: DEC-005, DEC-012

### Tanda 9: Hilt DI + ViewModels + UI wiring (commit ed4e6f6)

- Hilt modules: HumanosModule, QuebotModule, ObservabilityModule
- InMemoryTraceRepository (implementacion in-memory de TraceRepository)
- ViewModels: DashboardViewModel, CaptureViewModel, SettingsViewModel
- UI wired: pantallas usan hiltViewModel() + collectAsStateWithLifecycle()
- Dashboard muestra tareas reales del FakeHumanosGateway
- Capture guarda con TraceEvent logging
- Settings muestra estado de conexion live
- AndroidFeatureConventionPlugin actualizado con lifecycle + hilt-navigation-compose
- Refs: TASK-008, DEC-002, DEC-003

### Tanda 10: Docs sync + GitHub repo + verificacion estatica

- CURRENT_STATE.md actualizado con commits 8-10
- CHANGELOG actualizado
- TASKS.md sincronizado
- Verificacion estatica de imports y consistencia de codigo
- Refs: DEC-004

### Tanda 11-12: Build verificado + GitHub + CI (commits 2e115ba, 18e2de9)

- Felipe instalo Android Studio Quail 1 (Java 21 + Android SDK 36)
- Corregidos 5 errores de build: dependencyResolutionManagement, import JavaVersion x3, smart cast
- BUILD SUCCESSFUL, APK 20 MB
- Repo creado: github.com/Sinapsisfm/Humanos-App-Android
- CI #1 fallo (gradlew sin exec), fix con chmod +x, CI #2 VERDE en GitHub Actions
- Decision Felipe: GCP diferido, features primero
- Refs: TASK-003, TASK-004

### Tanda 13: Room database wiring (commit 025db31)

- core-database con Room real: 3 entities, 3 DAOs, converters, DatabaseModule Hilt
- core-model queda puro (entities separadas, mappers bridge)
- Schema v1 exportado y commiteado
- BUILD SUCCESSFUL 417 tasks
- Refs: TASK-007, DEC-012

### Tanda 14: data-capture + ADR Room (commit 839799f)

- Modulo data-capture (MOD-016, adelantado de Phase 2 por DEC-013)
- CaptureRepository + Impl usando CaptureDao: capturas persisten en Room
- feature-capture rewired: ViewModel usa repository, pantalla muestra capturas guardadas
- ADR-0005: Room entity/domain separation formalizado
- BUILD SUCCESSFUL 451 tasks
- Refs: DEC-013, TASK-007, REQ-CAP-001

### Tanda 15: Tests unitarios (commit fb99dff)

- 33 tests JVM: mappers Room (round-trip), converters, modelos
- Fix CI: agregado task `test` para cubrir modulos Kotlin puro
- CI #6 verde

### Tanda 16: data-tasks offline-first (commit c0fee88)

- Modulo data-tasks (MOD-017, DEC-014)
- TaskRepository: HumanosGateway -> Room -> UI (NetworkBoundResource)
- DashboardViewModel offline-first, 4 tests repo

### Tanda 17: ViewModel tests (commit 1a8fbd2)

- 9 tests: Dashboard/Capture/Settings ViewModels con fakes + Turbine
- 46 tests totales

### Tanda 18: feature-tasks screen (commit fbce65f)

- Modulo feature-tasks (MOD-018, DEC-015)
- Pantalla Tasks CRUD desde Room, 4to destino bottom nav
- TasksViewModel + 5 tests, 51 tests totales

### Tanda 19: observability persistente (commit 0691262)

- RoomTraceRepository: audit trail durable en Room (era in-memory)
- ObservabilityModule rebind, core-observability -> core-database

### Tanda 20: cierre Phase 1

- PHASE1_CLOSURE.md: inventario, deuda tecnica (TD-01 a TD-08), roadmap Phase 2/3, checklist
- CURRENT_STATE actualizado a estado de cierre
- Phase 1 COMPLETO: 24 commits, 18 modulos, 51 tests, build verde local + CI

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

---

## 2026-06-08 — Datos reales end-to-end + plan hibrido (v0.3.0 → v0.4.0)

### Bridge de datos real (v0.3.0–v0.3.2)
- humanos-eco: rutas `/api/mobile/*` (bridge-JWT) + helper `mobile-bridge-auth` +
  exencion en middleware (MED-04 bloqueaba `/api/mobile/*` con 404). Fix login
  409 → re-auth con email verificado. Felipe confirmo: login OK + 102 tareas reales.
- App: gateway repuntado a `/api/mobile/*` (envelopes, fechas ISO, priority lowercase);
  Dashboard real (snapshot, contadores, check-in, toggle); captura (voz/foto/archivo);
  surfacing de errores de auth (HumanosLinkState); createTask server-first + setDone
  (toggle sincronizado, promueve tareas locales); detalle de tareas; ApkInstaller
  (update desde el telefono) + re-check de updates en ON_RESUME.

### Plan hibrido nativo + WebView (v0.4.0) — ADR-0006
- **Session bridge**: provider NextAuth `mobile-bridge` (humanos-eco) canjea el
  bridge JWT por una sesion web → el WebView entra logueado sin Google-OAuth-en-WebView.
  Pagina `/mobile-login` + `lib/auth/bridge-session.ts` (6 tests). Refs DEC-016, DEC-017, RISK-010.
- **feature-web** (MOD-018): pestaña "Modulos" → hub de modulos web (home, empresa,
  estudiante, salud, care, legal) → WebView embebido autenticado. TASK-018..020 DONE.
- Releases firmados a6041dcf (instalan sobre la version previa sin conflicto).

## 2026-06-08 — v0.4.1 (iteración WebView)

- Menu doble resuelto: la barra inferior de la app se OCULTA en rutas `web/{moduleKey}`
  (el módulo web es full-screen con su propia navegación).
- WebViewScreen v2: settings completos (databaseEnabled, multiple-windows in-place,
  wide viewport, mixed-content compat), barra superior (atrás/recargar/título),
  progreso, y **captura de errores JS en pantalla** (chip ⚠) + setWebContentsDebuggingEnabled
  (chrome://inspect) para diagnosticar por qué el contenido no renderiza.
- Hallazgo Felipe: el shell autenticado carga (FABs QueBot, tour, nav) pero el
  CONTENIDO `<main>` no → hipótesis: hidratación React falla en el WebView.
  Diagnóstico via el chip ⚠ en la próxima prueba. TASK-023.

## 2026-06-08 — v0.4.2 (contenido WebView renderiza)

- **Causa raíz del contenido en blanco:** CSP `strict-dynamic`+nonce bloqueaba
  los scripts inline de hidratación de Next dentro del WebView (error capturado
  por Felipe: `/dashboard:1 Executing inline script violates ... script-src`).
- **Fix:** middleware `buildCsp(relaxInline)` — si el UA es `humanOSApp` (solo el
  WebView de la app), sirve CSP sin nonce/strict-dynamic → los scripts corren.
  Desktop/web sin cambios. RISK-011. (humanos-eco commit f259e28)
- App: WebView marca UA `humanOSApp` + host whitelist (solo humanos.eco/empresa.eco;
  externos al navegador) + botón "Compartir errores" (ACTION_SEND) para mandar el
  log de errores directo desde la app (pedido de Felipe: debug sin PC).

## 2026-06-08 — v0.4.3 (diagnóstico WebView conclusivo)

- v0.4.2 quitó el error CSP (el chip ⚠ ya no aparece) → el contenido en blanco
  ahora es por otra causa SIN error de consola (layout altura 0 o datos vacíos).
- v0.4.3: botón Diagnóstico (🐞) SIEMPRE visible + Compartir (📤) + sondeo del DOM
  post-hidratación (main existe?, mainChildren, mainH, bodyH, textLen, url) +
  captura de consola ERROR+WARNING. Distingue layout-collapse vs datos-vacíos vs
  redirect. TASK-023.
- Emulador (pedido de Felipe): requiere cmdline-tools + system image (~1.5GB) +
  AVD + workaround de login Google → esfuerzo dedicado (TASK-026). El sondeo DOM
  resuelve este round sin emulador.

## 2026-06-08 — v0.4.4 (contenido WebView visible)

- Sondeo DOM (leído del screenshot de Felipe): `mainExists:true, mainH:7937,
  textLen:6598, bodyH:0` → el contenido EXISTE pero el `<body>` colapsa a altura 0
  (el shell web usa unidades vh/dvh que el WebView no calcula bien).
- Fix: `loadWithOverviewMode=false` + inyección JS (HEIGHT_FIX_JS) que fuerza
  html/body a innerHeight en PÍXELES + des-colapsa ancestros 0-height/overflow-hidden
  de `<main>` (sin tocar position → header/sidebar intactos). TASK-023 cerrada.
- Botón compartir arreglado (no hacía nada): ahora COPIA al portapapeles + toast +
  share sheet (más confiable). El "otro bug" que reportó Felipe.

## 2026-06-08 — v0.4.5 (canal directo Felipe↔Claude)

- TASK-027: canal de 2 vías. Endpoint POST/GET /api/mobile/message (dual-auth:
  sesión web para la app, service-token QUEBOT_BACKEND_TOKEN para el agente),
  reusa ConversationMessage (sin migración). Página /mobile-chat (chat UI) abierta
  en el WebView como módulo "Claude". El agente corre un loop que lee y responde.
- Felipe escribe desde la app → yo leo/respondo sin que vaya al PC.

## 2026-06-08 — v0.4.6 (nav fix) + TASK-028 (imágenes en el canal)

- Canal Felipe↔Claude VERIFICADO end-to-end (Felipe escribió desde la app, respondí).
- TASK-028: carga de imágenes en /mobile-chat (resize+compress → metadata.imageDataUrl,
  cap ~3MB; render inline). Felipe puede mandarme capturas desde la app.
- Nav fix (v0.4.6): el botón ← del WebView vuelve directo a la app (la barra inferior
  reaparece) en vez de exigir muchos "atrás". El back del dispositivo sigue navegando
  el historial del WebView. TASK-029.

## 2026-06-08 — v0.4.7 (teclado + mic nativo) + canal founder-only/voz/imágenes

- Teclado: MainActivity windowSoftInputMode=adjustResize (el input ya no queda
  tapado al abrir el teclado). TASK-031.
- Mic: webkitSpeechRecognition no anda en WebView → mic NATIVO en la barra del
  chat (RecognizerIntent es-CL) que inyecta el transcript en el textarea (value
  setter React-compatible + input event). Quitado el 🎤 de la página. TASK-030.
- Canal: founder-only (resolveFounder + allowlist), imágenes (TASK-028), 2 vías.
- PENDIENTE: logo de la app (no encontré el PNG en humanos-eco/public; Felipe lo
  manda o lo busco en sesión enfocada). TASK-032.
