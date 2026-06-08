# DECISIONS_LOG -- humanOS Native Android

> Registro de decisiones de proyecto. Cada decision tiene ID unico, fecha, contexto, y referencias cruzadas. Las decisiones aqui son resumen ejecutivo; las que requieren justificacion tecnica extensa van en ADRs dedicados (`docs/01_ADR/`).

---

## DEC-001: Proyecto independiente

- **Fecha:** 2026-06-06
- **Decision:** humanOS Android es un proyecto independiente. HumanOS (Next.js) y QueBot (PHP/Next.js) son fuentes externas consultadas en modo read-only. No se comparte codigo fuente, no hay monorepo, no hay dependencia directa de compilacion.
- **Contexto:** Se evaluo crear el Android como modulo dentro de `humanos-eco` o como proyecto separado. Un monorepo complicaria CI/CD (Gradle vs npm), versionamiento, y permisos de deploy. Ademas, el equipo Android eventual no necesita acceso al backend.
- **Consecuencias:** Toda integracion con HumanOS y QueBot se hace via interfaces Gateway con contratos documentados. Los cambios en las APIs de HumanOS no rompen la compilacion Android (solo rompen en runtime si el contrato cambia).
- **ADR:** ADR-0001 (pendiente creacion)

---

## DEC-002: Stack tecnologico Kotlin + Jetpack Compose

- **Fecha:** 2026-06-06
- **Decision:** El stack es Kotlin + Jetpack Compose + Hilt + Room + DataStore + WorkManager + Material 3. Sin Java, sin XML layouts, sin Dagger puro.
- **Contexto:** Es el stack recomendado por Google para proyectos nuevos en 2025-2026. Compose simplifica UI reactiva, Hilt reduce boilerplate de DI, Room es el ORM estandar Android, DataStore reemplaza SharedPreferences, WorkManager maneja background work con constraints.
- **Consecuencias:** Requiere minSdk 24+ (Compose). KSP como procesador de anotaciones (no KAPT). Material 3 Dynamic Color requiere Android 12+ para colores dinamicos (degradacion graciosa en versiones anteriores).
- **ADR:** ADR-0002 (pendiente creacion)

---

## DEC-003: Integraciones via Gateway interfaces

- **Fecha:** 2026-06-06
- **Decision:** Toda comunicacion con sistemas externos (HumanOS, QueBot, APIs futuras) se abstrae detras de interfaces Gateway. Los modulos `integration-humanos` e `integration-quebot` implementan estas interfaces. El dominio nunca depende de detalles HTTP, endpoints, o formatos de respuesta especificos.
- **Contexto:** HumanOS y QueBot tienen APIs que pueden cambiar. El repo Android no controla esas APIs. Un patron Gateway permite mockear integraciones completas en tests y en Phase 1 (donde los endpoints reales no existen o no estan listos).
- **Consecuencias:** Phase 1 usa implementaciones mock de los gateways. Cuando los endpoints reales esten disponibles, se reemplazan las implementaciones sin tocar dominio ni UI.
- **ADR:** ADR-0001 (pendiente creacion)

---

## DEC-004: Trazabilidad documental como parte del producto

- **Fecha:** 2026-06-06
- **Decision:** La documentacion de trazabilidad (TASKS, DECISIONS, RISKS, TRACEABILITY_MATRIX, CHANGELOG) no es overhead administrativo sino parte integral del producto. Cada cambio significativo se registra con referencias cruzadas.
- **Contexto:** El ecosistema Sinapsis tiene multiples repos y agentes que trabajan en paralelo. Sin trazabilidad documental, las decisiones se pierden entre sesiones. Ademas, `TraceEvent` y `SourceReference` son conceptos del dominio de humanOS (auditoria de origen de datos).
- **Consecuencias:** Overhead de actualizacion de docs en cada cambio. Mitigado porque los docs son markdown plano, no sistemas externos.

---

## DEC-005: CI/CD via GitHub Actions

- **Fecha:** 2026-06-06
- **Decision:** El pipeline CI/CD usa GitHub Actions. Betas se distribuyen via Firebase App Distribution. Produccion va a Google Play Store.
- **Contexto:** GitHub Actions es gratuito para repos publicos y tiene runners Ubuntu con Android SDK. Firebase App Distribution permite distribuir APKs/AABs a testers internos sin Play Store. El flujo es: push a `main` -> CI build/test -> beta en Firebase App Distribution -> manual promote a Play Store.
- **Consecuencias:** Se necesita configurar Workload Identity Federation (WIF) o service account key para autenticacion con GCP/Firebase desde GitHub Actions. Se necesita Google Play Console de Sinapsis SpA (Q-004).

---

## DEC-006: 15 modulos Phase 1 (actualizada 2026-06-07)

- **Fecha:** 2026-06-06 (actualizada 2026-06-07 por DEC-011)
- **Decision:** Phase 1 tiene exactamente 15 modulos Gradle (originalmente 14, +1 core-observability por DEC-011):

| Modulo | Tipo | Proposito |
|--------|------|-----------|
| `app` | Android Application | Entry point, navegacion, DI wiring |
| `core-model` | Kotlin (pure JVM) | Data classes del dominio |
| `core-database` | Android Library | Room DB, entities, DAOs |
| `core-datastore` | Android Library | DataStore preferences |
| `core-network` | Android Library | Retrofit, OkHttp, interceptors |
| `core-security` | Android Library | Keystore, encryption, token storage |
| `core-ui` | Android Library | Compose components, theme, design tokens |
| `data-auth` | Android Library | AuthRepository impl, token management |
| `feature-dashboard` | Android Library | Dashboard screen + ViewModel |
| `feature-capture` | Android Library | Capture screen + CameraX |
| `feature-settings` | Android Library | Settings screen |
| `integration-humanos` | Android Library | HumanosGateway impl |
| `integration-quebot` | Android Library | QuebotGateway impl |
| `testing-common` | JVM Library | Test fixtures, fake data, mocks |

- **Contexto:** Se evaluo un split mas fino (ej. core-traceability, core-logging separados) y uno mas grueso (todo en app). 14 modulos balancea separacion de concerns con complejidad de Gradle. Modulos futuros (Phase 2-3): `core-observability`, `core-ai`, `integration-healthconnect`, `feature-terrain`, `data-context`, `data-capture`, `data-tasks`, `core-permissions`.
- **Consecuencias:** Cada modulo tiene su propio `build.gradle.kts`. Convention plugins en `build-logic/` reducen duplicacion. El grafo de dependencias fluye: `app` -> `feature-*` -> `data-*` -> `core-*`. `integration-*` depende de `core-network` + `core-model`.
- **ADR:** ADR-0003 (pendiente creacion)

---

## DEC-007: Auth dual-token como supuesto explicito

- **Fecha:** 2026-06-06
- **Decision:** La autenticacion usa dos tokens:
  1. **Firebase ID token**: obtenido via Firebase Auth (Google Sign-In o email/password). Usado para autenticarse con QueBot (que ya acepta Firebase tokens).
  2. **HumanOS bridge JWT**: obtenido intercambiando el Firebase ID token con HumanOS via `POST /api/auth/mobile/exchange`. Este endpoint **no existe aun**.
- **Contexto:** HumanOS usa NextAuth con session cookies (no JWTs publicos). Un cliente mobile no puede usar cookies de sesion web. Se necesita un endpoint que acepte un Firebase token verificable y devuelva un JWT de HumanOS con los claims necesarios (personId, orgId, roles).
- **Consecuencias:** Phase 1 usa mock auth. El endpoint real es una tarea para el repo `humanos-eco` (no este repo). El contrato se documenta en TASK-010.
- **ADR:** ADR-0004 (pendiente creacion)
- **Refs:** RISK-001, TASK-010

---

## DEC-008: Bridge endpoint documentado como contrato futuro

- **Fecha:** 2026-06-06
- **Decision:** El contrato `POST /api/auth/mobile/exchange` se documenta en este repo como especificacion de referencia. La implementacion del endpoint ocurre en el repo `humanos-eco`, no aqui. Este repo solo consume el endpoint (o su mock).
- **Contexto:** Si el contrato se documentara solo en `humanos-eco`, el equipo Android no tendria visibilidad. Si se implementara aqui, romperia la regla de que `humanos-eco` es la unica fuente de verdad de su API.
- **Consecuencias:** Posible drift entre el contrato documentado aqui y la implementacion real en HumanOS. Mitigado con tests de contrato y versionamiento del doc.
- **Refs:** DEC-007, TASK-010

---

## DEC-009: Merge core-traceability + core-logging en core-observability

- **Fecha:** 2026-06-06
- **Decision:** En lugar de tener modulos separados `core-traceability` y `core-logging`, se mergearon en un futuro modulo `core-observability` (Phase 2). Los modelos de trazabilidad (`TraceEvent`, `SourceReference`) viven en `core-model` desde Phase 1.
- **Contexto:** Se identifico que traceability y logging comparten infraestructura (escritura a Room, exportacion, retention policies). Separarlos crearia duplicacion. Los modelos de datos son puros (sin dependencias Android) y pertenecen a `core-model`.
- **Consecuencias:** Phase 1 tiene los modelos pero no la infraestructura de observabilidad. Phase 2 agrega `core-observability` con la implementacion completa (retencion, exportacion, dashboards de auditoria).

---

## DEC-010: compileSdk preferente 36

- **Fecha:** 2026-06-06
- **Decision:** El proyecto apunta a `compileSdk = 36` (API 36, Android 16). Si el SDK 36 no esta disponible en el entorno CI o local, se usa `compileSdk = 35` como fallback temporal con una TASK para subir cuando este disponible.
- **Contexto:** API 36 se anuncio en Google I/O 2025. Incluye mejoras de privacy, predictive back, y nuevas APIs de Health Connect. Compilar contra 36 no significa que el `minSdk` sea 36 -- el `minSdk` sera 26 (Android 8.0) para cubrir >95% de dispositivos activos.
- **Consecuencias:** Si se usa fallback 35, algunas APIs de Android 16 no estaran disponibles hasta el upgrade. No es bloqueante para Phase 1.

---

## DEC-011: core-observability entra en Phase 1

- **Fecha:** 2026-06-07
- **Decision:** `core-observability` se incluye en Phase 1 como modulo 15. Contiene la infraestructura de TraceEvent logging, structured logging, y audit trail. Los modelos (`TraceEvent`, `SourceReference`) viven en `core-model`; `core-observability` provee la implementacion (escritura a Room, retention, query).
- **Contexto:** GPT identifico en revision de Tanda 3 que dejar observabilidad para Phase 2 contradice DEC-004 (trazabilidad como parte del producto). Si la trazabilidad es core, su infraestructura tambien lo es.
- **Consecuencias:** Phase 1 pasa de 14 a 15 modulos. DEC-006 se actualiza para reflejar 15 modulos. MODULE_MAP se actualiza con MOD-015.
- **Refs:** DEC-004, DEC-006 (actualizada), DEC-009

---

## DEC-012: TraceEvent canonico en core-model

- **Fecha:** 2026-06-07
- **Decision:** La clase `TraceEvent` canonica vive en `core-model/common/TraceEvent.kt`. Se elimino la definicion duplicada que existia en `core-observability` (stub de Tanda 4). `core-observability` importa desde `core-model`. `TraceCategory` enum permanece en `core-observability` porque es infraestructura de logging, no modelo de dominio.
- **Contexto:** GPT identifico en revision de Tandas 5-6 que existian dos definiciones de `TraceEvent` con campos diferentes. La de `core-model` tiene `@Serializable`, entityType, entityId, action, source, userId. La de `core-observability` era un stub simplificado con name, category, metadata.
- **Consecuencias:** Una sola definicion canonica. `core-observability` depende de `core-model` (ya configurado en build.gradle.kts).
- **Refs:** DEC-009

---

## DEC-013: Adelantar data-capture a Phase 1

- **Fecha:** 2026-06-07
- **Decision:** El modulo `data-capture` (originalmente planificado Phase 2, MOD-025) se adelanta a Phase 1 como MOD-016. Implementa `CaptureRepository` usando el `CaptureDao` de Room.
- **Contexto:** Tras wirear Room (DEC-012/Tanda 13), tener la base de datos sin un repository que la use no aporta valor. `feature-capture` ya existe en Phase 1; conectarla a persistencia real (capturas que sobreviven al cierre de la app) es el cierre natural del loop UI -> ViewModel -> Repository -> Room.
- **Consecuencias:** Phase 1 pasa de 15 a 16 modulos. `feature-capture` depende de `data-capture`. El patron repository queda establecido para replicar en `data-tasks` y otros. `core-model` sigue puro (entities en core-database, mappers bridge).
- **Refs:** DEC-002, DEC-003, DEC-012, TASK-007

---

## DEC-014: Adelantar data-tasks a Phase 1 + patron offline-first

- **Fecha:** 2026-06-07
- **Decision:** El modulo `data-tasks` (originalmente Phase 2, MOD-024) se adelanta a Phase 1 como MOD-017. Implementa `TaskRepository` con patron offline-first: `HumanosGateway` (remoto) -> Room (source of truth) -> UI (observa Flow).
- **Contexto:** Tras validar el patron repository con `data-capture` (DEC-013) + tests (Tanda 15), GPT confirmo replicarlo para tasks. Esta vez se implementa el NetworkBoundResource completo documentado en DATA_FLOW.md: `syncFromRemote()` trae tareas del gateway fake y las cachea en Room; el Dashboard observa Room y reacciona. Demuestra el flujo offline-first real, no solo persistencia local.
- **Consecuencias:** Phase 1 pasa de 16 a 17 modulos. `DashboardViewModel` ahora usa `TaskRepository` (Room) en vez de `HumanosGateway` directo — las tareas persisten y sobreviven cierre de app. Patron validado para replicar en sync de context, health, etc. (Phase 2+).
- **Nota de trazabilidad:** DEC-013 cubre la promocion de `data-capture` a Phase 1. DEC-014 cubre `data-tasks`. La separacion Room entity/domain esta en ADR-0005 (no es una DEC). Aclaracion por observacion de GPT en revision de Tanda 14.
- **Refs:** DEC-003, DEC-013, ADR-0005, REQ-TASK-001

---

## Indice rapido

| ID | Titulo | Fecha |
|----|--------|-------|
| DEC-001 | Proyecto independiente | 2026-06-06 |
| DEC-002 | Stack Kotlin + Compose | 2026-06-06 |
| DEC-003 | Integraciones via Gateway | 2026-06-06 |
| DEC-004 | Trazabilidad documental | 2026-06-06 |
| DEC-005 | CI/CD GitHub Actions | 2026-06-06 |
| DEC-006 | 15 modulos Phase 1 (actualizada) | 2026-06-07 |
| DEC-007 | Auth dual-token | 2026-06-06 |
| DEC-008 | Bridge endpoint como contrato | 2026-06-06 |
| DEC-009 | Merge en core-observability | 2026-06-06 |
| DEC-010 | compileSdk 36 preferente | 2026-06-06 |
| DEC-011 | core-observability en Phase 1 | 2026-06-07 |
| DEC-012 | TraceEvent canonico en core-model | 2026-06-07 |
| DEC-013 | Adelantar data-capture a Phase 1 | 2026-06-07 |
| DEC-014 | Adelantar data-tasks a Phase 1 + offline-first | 2026-06-07 |
