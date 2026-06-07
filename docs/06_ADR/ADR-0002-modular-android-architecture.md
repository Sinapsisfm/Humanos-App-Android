# ADR-0002: Modular Android Architecture with Convention Plugins

**Status:** Accepted

**Date:** 2026-06-06

**Deciders:** Felipe Mehr (Sinapsis SpA)

## Context

The humanOS Android app has a broad feature surface: capture (photo, audio, document), tasks, agent interaction, terrain mapping, health tracking, and multiple third-party integrations. This will grow to approximately 30 modules across 4 phases.

Two architectural approaches were considered:

1. **Monolithic app module**: All code in a single `:app` module. Simple to start, increasingly painful as the codebase grows (slow builds, tangled dependencies, difficult feature isolation).
2. **Multi-module with convention plugins**: Modules organized by layer (core, data, domain, feature, integration) with shared build logic in convention plugins. Higher setup cost, but enforces boundaries and scales well.

## Decision

Adopt a **multi-module Gradle architecture** with **convention plugins** for shared build configuration.

### Module Layers

```
feature-*     ← UI + ViewModel (depends on domain, core)
    |
domain-*      ← Use cases, business logic (depends on core)
    |
data-*        ← Room, DataStore, repositories (depends on core)
    |
core-*        ← Shared models, utilities, DI, security
    |
integration-* ← External system interfaces (HumanOS, QueBot, Health Connect)
    |
testing-common ← Fakes, fixtures, test rules
```

### Phase 1 Modules (14 modules)

| Module | Layer | Purpose |
|---|---|---|
| `:app` | Application | Main entry point, navigation host, Hilt setup |
| `:core:core-model` | Core | Domain models, enums, value objects |
| `:core:core-common` | Core | Shared utilities, extensions, base classes |
| `:core:core-database` | Core | Room database definition, migrations |
| `:core:core-datastore` | Core | DataStore (preferences, encrypted) |
| `:core:core-network` | Core | Retrofit setup, interceptors, auth headers |
| `:core:core-security` | Core | Encryption, biometric, token storage |
| `:core:core-sync` | Core | SyncManager, conflict resolution |
| `:data:data-capture` | Data | Capture entities, DAO, repository |
| `:data:data-tasks` | Data | Task entities, DAO, repository |
| `:feature:feature-capture` | Feature | Capture UI (camera, audio, gallery) |
| `:feature:feature-tasks` | Feature | Task list, detail, creation UI |
| `:feature:feature-agent` | Feature | Agent chat, briefing UI |
| `:feature:feature-dashboard` | Feature | Home dashboard, overview cards |

### Phase 2+ Additional Modules (~16 modules)

| Module | Layer | Phase |
|---|---|---|
| `:core:core-ai` | Core | 3 |
| `:core:core-notification` | Core | 2 |
| `:data:data-terrain` | Data | 2 |
| `:data:data-health` | Data | 3 |
| `:data:data-agent` | Data | 2 |
| `:domain:domain-capture` | Domain | 2 |
| `:domain:domain-tasks` | Domain | 2 |
| `:domain:domain-terrain` | Domain | 2 |
| `:domain:domain-health` | Domain | 3 |
| `:feature:feature-terrain` | Feature | 2 |
| `:feature:feature-health` | Feature | 3 |
| `:feature:feature-settings` | Feature | 2 |
| `:integration:integration-humanos` | Integration | 1 |
| `:integration:integration-quebot` | Integration | 1 |
| `:integration:integration-healthconnect` | Integration | 3 |
| `:testing:testing-common` | Testing | 1 |

### Technology Stack

| Category | Choice | Version (as of 2026-06-06) |
|---|---|---|
| Language | Kotlin | 2.1.20 |
| UI Framework | Jetpack Compose | BOM 2025.05 |
| Design System | Material 3 | Via Compose BOM |
| DI | Hilt | 2.54 |
| Local Database | Room | 2.7.1 |
| Preferences | DataStore | 1.1.4 |
| Background Work | WorkManager | 2.10.1 |
| Navigation | Navigation Compose | 2.9.0 |
| Networking | Retrofit + OkHttp | 2.11 / 4.12 |
| Serialization | Kotlinx Serialization | 1.8.0 |
| Image Loading | Coil | 3.1 |
| Build System | Gradle (Kotlin DSL) | 8.12 |
| compileSdk | 36 | Android 16 |
| targetSdk | 36 | Android 16 |
| minSdk | 26 | Android 8.0 (Oreo) |

### Convention Plugins

Shared build logic in `build-logic/convention/`:

| Plugin | Purpose |
|---|---|
| `humanos.android.application` | App module defaults (compileSdk, minSdk, compose, signing) |
| `humanos.android.library` | Library module defaults |
| `humanos.android.library.compose` | Library + Compose compiler + Material 3 |
| `humanos.android.feature` | Feature module (Compose + Hilt + Navigation) |
| `humanos.android.hilt` | Hilt KSP setup |
| `humanos.android.room` | Room KSP setup + schema export |
| `humanos.android.test` | Test dependencies (JUnit5, MockK, Turbine) |
| `humanos.kotlin.library` | Pure Kotlin library (no Android) |

### Dependency Management

Version catalog in `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "2.1.20"
compose-bom = "2025.05.00"
hilt = "2.54"
room = "2.7.1"
# ...

[libraries]
# Grouped by category
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
# ...

[plugins]
# Convention plugins applied via id
humanos-android-application = { id = "humanos.android.application" }
# ...
```

## Consequences

### What becomes easier

- **Build times**: Incremental builds only recompile affected modules. Feature development rarely touches core.
- **Dependency enforcement**: Gradle module boundaries prevent unauthorized cross-feature dependencies. A feature module cannot import another feature module.
- **Parallel development**: Multiple features can be developed simultaneously without merge conflicts.
- **Testing isolation**: Each module has its own test suite. Fakes replace real implementations at module boundaries.
- **Feature flags**: Individual feature modules can be excluded from the build via Gradle configuration.
- **Onboarding**: New developers can focus on one module without understanding the entire codebase.

### What becomes harder

- **Initial setup**: Creating 14+ modules with convention plugins is significant upfront work before any visible feature.
- **Gradle sync time**: Each module adds overhead to Gradle sync (mitigated by convention plugins reducing boilerplate).
- **Navigation complexity**: Cross-module navigation requires careful route/deep-link management.
- **Refactoring across modules**: Moving code between modules requires careful dependency graph updates.

### Mitigations

- Convention plugins eliminate per-module build boilerplate (each module's `build.gradle.kts` is typically < 20 lines).
- `MODULE_MAP.md` documents the full module graph and dependencies.
- Navigation routes are centralized in a shared navigation module pattern.
- Module creation is scripted (future task).

## References

- DEC-002: Technology stack selection
- DEC-003: Module layer boundaries
- `docs/01_ARCHITECTURE/MODULE_MAP.md`: Full module inventory
- [Now in Android](https://github.com/android/nowinandroid): Reference multi-module architecture
