# Architecture Overview

> humanOS Native Android -- Multi-Module Architecture
> Last updated: 2026-06-06

## Guiding Principles

1. **Offline-first**: Room is the single source of truth. The UI never reads directly from the network.
2. **Modular by layer**: Each concern lives in its own Gradle module so builds are incremental and boundaries are enforced at compile time.
3. **Convention over configuration**: Shared build logic lives in `build-logic/` as convention plugins, keeping individual `build.gradle.kts` files minimal.
4. **Testable by default**: Every module below `feature-*` exposes interfaces, not implementations. Fakes ship in `testing-common`.

## Layer Hierarchy

```
feature-*          UI (Compose screens, ViewModels)
   |
domain-*           Use-case orchestration (Phase 2+)
   |
data-*             Repository implementations, sync logic
   |
integration-*      Retrofit services, DTOs, Gateway interfaces
   |
core-*             Framework-agnostic foundations
```

Dependency rules:
- A module may only depend on modules in the **same layer or below**.
- `feature` modules never depend on each other.
- `integration` modules never depend on `data` or `feature` modules.
- `core` modules never depend on anything above `core`, except `core-ui` which exists at the boundary.

## Technology Stack

| Concern | Library / API | Notes |
|---|---|---|
| Dependency injection | Hilt (Dagger) | `@HiltAndroidApp`, `@HiltViewModel`, `@Inject` |
| Local persistence | Room 2.6+ | Type-safe DAOs, `@TypeConverter` for enums and JSON blobs |
| Preferences | DataStore Proto / Preferences | Replaces SharedPreferences for user settings and session tokens |
| Background work | WorkManager | Periodic sync, deferred uploads, retry with backoff |
| Navigation | Navigation Compose (type-safe) | Single `NavHost` in `app`, feature modules declare route graphs |
| Async | Coroutines + Flow | `StateFlow` in ViewModels, `Flow` from Room DAOs |
| Networking | Retrofit 2 + OkHttp 4 | JSON via kotlinx.serialization; SSE via OkHttp EventSource |
| UI | Jetpack Compose + Material 3 | Dynamic color, dark/light theme, shared design tokens in `core-ui` |
| Image loading | Coil (Compose) | Disk + memory cache, placeholder shimmer |
| Security | Android Keystore + EncryptedSharedPreferences | Device-bound keys for VAULT data |
| Testing | JUnit 5, Turbine (Flow), MockK, Compose UI Test | Convention plugin wires defaults |

## Offline-First Data Strategy

```
   Compose UI
       |
   ViewModel (StateFlow<UiState>)
       |
   Repository
      / \
  Room    Retrofit
  (cache)  (remote)
```

The `NetworkBoundResource` pattern governs every synced entity:

1. **Emit cached** -- Room query returns immediately via `Flow`.
2. **Fetch remote** -- Retrofit call runs on `Dispatchers.IO`.
3. **Save to Room** -- Network response is mapped to entities and inserted.
4. **Re-emit** -- Room `Flow` automatically pushes the updated data to the UI.

If the network call fails, the UI continues showing stale cached data with a visual staleness indicator. The user is never blocked.

## Convention Plugins (`build-logic/`)

| Plugin ID | Purpose |
|---|---|
| `humanos.android.library` | Base Android library config (compileSdk, minSdk, kotlin) |
| `humanos.android.compose` | Compose compiler + Material 3 dependencies |
| `humanos.android.hilt` | Hilt + KSP annotation processor wiring |
| `humanos.android.room` | Room + KSP + schema export directory |
| `humanos.android.feature` | Combines library + compose + hilt for feature modules |
| `humanos.android.test` | JUnit 5 + Turbine + MockK + coroutines-test |

## Module Dependency Graph (Phase 1)

```
                        :app
                       / | \
          +-----------+  |  +-----------+
          |              |              |
  :feature-dashboard  :feature-capture  :feature-settings
          |              |              |
          +---------+----+----+---------+
                    |         |
                :core-ui   :core-model
                    |         |
          +---------+---------+---------+
          |         |         |         |
  :core-database  :core-datastore  :core-network
          |                   |         |
          |            :core-security   |
          |                             |
          +-----------------------------+
                        |
              :integration-humanos
              :integration-quebot
                        |
              :testing-common (testImplementation only)
```

## Key Architectural Decisions

- **ADR-001**: Multi-module from day one (not a monolith to be broken later).
- **ADR-002**: Room over SQLite-raw for type safety and Flow integration.
- **ADR-003**: Hilt over manual DI for ViewModel injection and module scoping.
- **ADR-004**: kotlinx.serialization over Gson/Moshi for multiplatform readiness.
- **ADR-005**: Navigation Compose type-safe routes over string-based deeplinks.

## What Is NOT in Phase 1

- `domain-*` use-case modules (added in Phase 2 when business logic grows).
- On-device ML inference.
- Health Connect integration.
- Background sync via WorkManager (jobs are defined but not scheduled).
- Push notifications (FCM token registration only).
