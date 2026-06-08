# Module Map

> humanOS Native Android -- Complete Module Inventory
> Last updated: 2026-06-06

## Phase 1 Modules (17 modules -- current, updated 2026-06-07 per DEC-011, DEC-013, DEC-014)

| MOD-ID | Name | Layer | Phase | Dependencies | Description |
|---|---|---|---|---|---|
| MOD-001 | `:app` | application | 1 | all feature modules, core-ui, core-observability, data-auth | Application entry point. `@HiltAndroidApp`, `MainActivity`, `NavHost`, splash screen. |
| MOD-002 | `:core-model` | core | 1 | none | All shared data classes, enums, Room `@Entity` classes, sealed result types. Zero Android framework dependency. |
| MOD-003 | `:core-database` | core | 1 | core-model(api) | Room database definition, all `@Dao` interfaces, `@TypeConverter` classes, migration helpers. Exports schema JSON for CI diff. |
| MOD-004 | `:core-datastore` | core | 1 | core-model | Jetpack DataStore preferences. User session state, theme preference, onboarding flags, last-sync timestamps. |
| MOD-005 | `:core-network` | core | 1 | core-security, core-datastore | Retrofit builder, OkHttp client with auth interceptor, SSE `EventSource` wrapper, `NetworkMonitor` (ConnectivityManager). JSON converter factory (kotlinx.serialization). |
| MOD-006 | `:core-security` | core | 1 | core-model | Android Keystore wrapper, `EncryptedSharedPreferences` for token storage, `BiometricGate` (BiometricPrompt wrapper), `PrivacyLevel` enforcement. |
| MOD-007 | `:core-ui` | core | 1 | core-model | Material 3 theme (colors, typography, shapes), shared composables (`HumanosTopBar`, `LoadingShimmer`, `ErrorCard`, `EmptyState`), design tokens, icon set. |
| MOD-008 | `:data-auth` | data | 1 | core-model, core-network, core-datastore, core-security, integration-humanos | `AuthRepository` implementation. Firebase Google Sign-In flow, token persistence, session refresh, logout cleanup. |
| MOD-009 | `:feature-dashboard` | feature | 1 | core-ui, core-model, data-auth | Dashboard screen. Daily summary placeholder, greeting, navigation cards to other features. `DashboardViewModel`. |
| MOD-010 | `:feature-capture` | feature | 1 | core-ui, core-model | Universal Capture screen. Text input, voice memo stub, photo stub. `CaptureViewModel`. Phase 1 = local-only, no sync. |
| MOD-011 | `:feature-settings` | feature | 1 | core-ui, core-model, core-datastore, data-auth | Settings screen. Theme toggle, account info, logout, app version, privacy preferences. `SettingsViewModel`. |
| MOD-012 | `:integration-humanos` | integration | 1 | core-network, core-model | Retrofit service interfaces for HumanOS API. DTO classes. `HumanosGateway` interface. `FakeHumanosGateway` for offline/testing. |
| MOD-013 | `:integration-quebot` | integration | 1 | core-network, core-model | Retrofit service + SSE client for QueBot. DTO classes. `QuebotGateway` interface. `FakeQuebotGateway` with canned responses. |
| MOD-014 | `:testing-common` | testing | 1 | core-model | Shared test utilities. Fake dispatchers (`TestDispatcherRule`), model fixtures (`TestFixtures.kt`), Room in-memory DB helper, fake `NetworkMonitor`. |
| MOD-015 | `:core-observability` | core | 1 | core-model, core-database | TraceEvent logging infrastructure, structured logging, audit trail persistence, retention policies. Models live in core-model; this module provides write/query implementation. (Added per DEC-011) |
| MOD-016 | `:data-capture` | data | 1 | core-model, core-database, core-observability | CaptureRepository implementation using Room CaptureDao. Persists text captures, observes via Flow, logs TraceEvents. Advanced from Phase 2 per DEC-013. |
| MOD-017 | `:data-tasks` | data | 1 | core-model, core-database, core-observability, integration-humanos | TaskRepository with offline-first sync: HumanosGateway -> Room -> UI. syncFromRemote caches remote tasks, observeTasks emits from Room. Advanced from Phase 2 per DEC-014. |

## Phase 2 Modules (planned -- ~12 modules)

| MOD-ID | Name | Layer | Phase | Dependencies | Description |
|---|---|---|---|---|---|
| MOD-020 | `:domain-context` | domain | 2 | core-model, core-database | Use cases for context graph: create/query/link ContextNodes, infer edges, merge duplicates. |
| MOD-021 | `:domain-tasks` | domain | 2 | core-model, core-database | Use cases for task CRUD, priority engine, recurrence, delegation tracking. |
| MOD-022 | `:domain-sync` | domain | 2 | core-model, core-database, core-network, integration-humanos | Sync orchestrator. Conflict resolution (last-write-wins with server tiebreak). WorkManager job definitions. |
| MOD-023 | `:data-context` | data | 2 | core-model, core-database, domain-context | `ContextRepository` implementation. Room graph storage, edge weight decay. |
| MOD-024 | `:data-tasks` | data | 2 | core-model, core-database, domain-tasks, integration-humanos | `TaskRepository`. Local CRUD + remote sync via NetworkBoundResource. |
| MOD-025 | `:data-capture` | data | 2 | core-model, core-database, core-network | `CaptureRepository`. Persists captures to Room, queues for upload via WorkManager. |
| MOD-026 | `:feature-tasks` | feature | 2 | core-ui, core-model, data-tasks | Task list, detail, create/edit screens. Drag-to-reorder. Filters by context. |
| MOD-027 | `:feature-quebot` | feature | 2 | core-ui, core-model, integration-quebot, data-auth | QueBot chat screen. SSE streaming UI, message history (local Room), case selector. |
| MOD-028 | `:feature-context` | feature | 2 | core-ui, core-model, data-context | Context graph explorer. Node detail, relationship visualization, manual link creation. |
| MOD-029 | `:feature-daily-review` | feature | 2 | core-ui, core-model, data-tasks, data-context | Morning/evening review flow. AI-generated summary (via QueBot), action suggestions, check-in. |
| MOD-030 | `:core-permissions` | core | 2 | core-model, core-ui | Runtime permission request orchestrator. Rationale dialogs, settings deep-link on permanent deny. |
| MOD-031 | `:core-notifications` | core | 2 | core-model | FCM token management, notification channel setup, local notification builder, `NotificationRepository`. |

## Phase 3 Modules (planned -- ~10 modules)

| MOD-ID | Name | Layer | Phase | Dependencies | Description |
|---|---|---|---|---|---|
| MOD-040 | `:domain-health` | domain | 3 | core-model | Use cases for Health Connect reads, energy scoring, medication adherence tracking. |
| MOD-041 | `:data-health` | data | 3 | core-model, core-database, domain-health | `HealthRepository`. Health Connect API reads, Room cache, daily aggregations. |
| MOD-042 | `:feature-health` | feature | 3 | core-ui, core-model, data-health | Health dashboard. Steps, sleep, heart rate, medications, energy score chart. |
| MOD-043 | `:feature-terrain` | feature | 3 | core-ui, core-model, core-permissions | Field assistant. GPS tracking, geofence triggers, offline map tiles, location-aware captures. |
| MOD-044 | `:feature-vault` | feature | 3 | core-ui, core-model, core-security | Encrypted vault UI. Biometric unlock, local-only documents, secure notes. Never syncs. |
| MOD-045 | `:domain-ai` | domain | 3 | core-model | On-device inference orchestrator. TFLite model loader, embedding generator, semantic search index. |
| MOD-046 | `:data-ai` | data | 3 | core-model, core-database, domain-ai | `AiRepository`. Local embedding storage, similarity search, model versioning. |
| MOD-047 | `:feature-memory` | feature | 3 | core-ui, core-model, data-ai, data-context | Semantic memory explorer. "What did I say about X?" Natural language query over context graph. |
| MOD-048 | `:feature-widgets` | feature | 3 | core-model, core-database | Glance widgets: daily summary, quick capture, next task, energy score. |
| MOD-049 | `:core-bluetooth` | core | 3 | core-model | BLE scanner, device pairing, GATT characteristic reader. Wearable data bridge. |

## Module Count Summary

| Phase | Core | Data | Domain | Feature | Integration | Testing | App | Total |
|---|---|---|---|---|---|---|---|---|
| Phase 1 | 6 | 1 | 0 | 3 | 2 | 1 | 1 | **14** |
| Phase 2 | 2 | 3 | 3 | 4 | 0 | 0 | 0 | **12** |
| Phase 3 | 1 | 3 | 2 | 4 | 0 | 0 | 0 | **10** |
| **Total** | **9** | **7** | **5** | **11** | **2** | **1** | **1** | **36** |

## Dependency Rules Enforcement

The following are compile-time errors enforced by convention plugins and Gradle module structure:

1. `:feature-*` modules cannot depend on other `:feature-*` modules.
2. `:core-*` modules cannot depend on `:data-*`, `:domain-*`, or `:feature-*` modules.
3. `:integration-*` modules cannot depend on `:data-*`, `:domain-*`, or `:feature-*` modules.
4. `:domain-*` modules cannot depend on `:feature-*` or `:data-*` modules (domain is pure logic).
5. Only `:app` may depend on `:feature-*` modules directly.
6. `:testing-common` is consumed via `testImplementation` or `androidTestImplementation` only.
