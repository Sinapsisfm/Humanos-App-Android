# Roadmap

> humanOS Native Android -- Phased Delivery Plan
> Last updated: 2026-06-06

## Phase Overview

| Phase | Name | Goal | Status |
|---|---|---|---|
| Phase 1 | Skeleton | Compiling multi-module project with navigation, models, interfaces, mocks, and full documentation | **Current** |
| Phase 2 | Connected | Real sync, context graph storage, tasks UI, QueBot chat, permissions, FCM, WorkManager | Planned |
| Phase 3 | Intelligent | Health Connect, terrain, on-device AI, semantic search, vault UI, widgets, memory | Planned |

---

## Phase 1 -- Skeleton

**Objective**: A multi-module Android project that compiles, navigates between screens, defines every model and interface, and is fully documented. No real backend connectivity. This is the architectural foundation.

### Deliverables

| # | Deliverable | Module(s) | Acceptance Criteria |
|---|---|---|---|
| 1.1 | Build system with convention plugins | `build-logic/` | `./gradlew assembleDebug` succeeds; all 14 modules compile |
| 1.2 | All data models and enums | `core-model` | Every entity, enum, and sealed type defined and documented |
| 1.3 | Room database v1 | `core-database` | Schema exports; Capture DAO inserts and queries; TypeConverters handle all types |
| 1.4 | DataStore preferences | `core-datastore` | Theme, session state, onboarding flag read/write via Flow |
| 1.5 | Network infrastructure | `core-network` | Retrofit builder, OkHttp client, auth interceptor, SSE wrapper, NetworkMonitor |
| 1.6 | Security foundations | `core-security` | Keystore wrapper, EncryptedSharedPreferences, BiometricGate interface |
| 1.7 | Material 3 theme + shared composables | `core-ui` | Light/dark theme, HumanosTopBar, LoadingShimmer, ErrorCard, EmptyState |
| 1.8 | Dashboard screen | `feature-dashboard` | Greeting, navigation cards, placeholder daily summary |
| 1.9 | Capture screen (text only) | `feature-capture` | Text input saves to Room; voice/photo buttons disabled with tooltip |
| 1.10 | Settings screen | `feature-settings` | Theme toggle persists; logout clears session; version displayed |
| 1.11 | Auth interfaces + fake | `data-auth` | AuthRepository interface; FakeAuthRepository simulates signed-in user |
| 1.12 | HumanOS integration module | `integration-humanos` | Retrofit interface, DTOs, HumanosGateway, FakeHumanosGateway |
| 1.13 | QueBot integration module | `integration-quebot` | Retrofit interface + SSE, DTOs, QuebotGateway, FakeQuebotGateway |
| 1.14 | Testing utilities | `testing-common` | TestDispatcherRule, TestFixtures, in-memory Room helper, fake NetworkMonitor |
| 1.15 | Navigation | `app` | NavHost with bottom bar; type-safe routes to all 3 screens |
| 1.16 | Documentation | `docs/` | All architecture, product, integration, and ADR docs complete |

### Exit Criteria

- App builds and runs on emulator (API 26+).
- Navigation between all screens works.
- Text capture persists in Room.
- Theme toggle persists across restart.
- All unit tests pass.
- Documentation reviewed and cross-referenced.

---

## Phase 2 -- Connected

**Objective**: The app connects to HumanOS and QueBot backends, syncs data, and provides real functionality: task management, QueBot chat, daily review, and context graph operations.

### Prerequisites

- HumanOS: `POST /api/auth/mobile/exchange` endpoint deployed.
- Firebase project configuration finalized (project ID verified).
- QueBot: SSE streaming endpoint stable and documented.

### Deliverables

| # | Deliverable | Module(s) | Description |
|---|---|---|---|
| 2.1 | Firebase Auth (real) | `data-auth` | Google Sign-In flow, token persistence, bridge JWT exchange |
| 2.2 | Sync engine | `domain-sync`, `data-*` | WorkManager periodic sync, NetworkBoundResource for all entities, conflict resolution |
| 2.3 | Context graph storage | `domain-context`, `data-context` | Room tables for nodes/edges, CRUD use cases, edge weight decay |
| 2.4 | Tasks UI | `domain-tasks`, `data-tasks`, `feature-tasks` | List, detail, create/edit screens; filters; sync with HumanOS |
| 2.5 | QueBot chat | `feature-quebot` | SSE streaming UI, message history in Room, case selector |
| 2.6 | Daily review | `feature-daily-review` | Morning briefing + evening review; AI summary via QueBot |
| 2.7 | Context explorer | `feature-context` | Node detail, relationship list, manual link creation |
| 2.8 | Camera capture | `feature-capture` | CameraX photo capture, preview, crop, save to Room |
| 2.9 | Voice capture | `feature-capture` | MediaRecorder voice memo, transcription via QueBot |
| 2.10 | Share sheet | `feature-capture` | Inbound: text, URLs, images from other apps |
| 2.11 | Push notifications | `core-notifications` | FCM registration, notification channels, local reminders |
| 2.12 | Runtime permissions | `core-permissions` | Permission request orchestrator with rationale dialogs |
| 2.13 | Background work | `domain-sync` | WorkManager jobs: periodic sync, deferred upload, cleanup |
| 2.14 | Foreground location | `feature-terrain`, `feature-capture` | Geotagging captures, nearby places |

### Exit Criteria

- User can sign in with Google and access their HumanOS data.
- Tasks sync bidirectionally with HumanOS backend.
- QueBot chat streams responses in real time.
- Captures (text, voice, photo) persist locally and queue for upload.
- Daily review screen shows AI-generated summary.
- Push notifications arrive for reminders and sync alerts.
- App works offline with stale data indicators.

---

## Phase 3 -- Intelligent

**Objective**: The app becomes a true personal operating system with health awareness, field capabilities, on-device AI, semantic search, and home screen integration.

### Deliverables

| # | Deliverable | Module(s) | Description |
|---|---|---|---|
| 3.1 | Health Connect | `domain-health`, `data-health`, `feature-health` | Read steps, sleep, heart rate, blood pressure, medications; daily aggregations; energy score |
| 3.2 | Terrain/field assistant | `feature-terrain` | Background location, geofence triggers, offline map tiles, NFC check-in |
| 3.3 | On-device AI | `domain-ai`, `data-ai` | TFLite model loading, text embedding generation, similarity search index |
| 3.4 | Semantic memory | `feature-memory` | Natural language queries over context graph ("What did I decide about...?") |
| 3.5 | Vault UI | `feature-vault` | Encrypted document viewer, secure notes, biometric unlock flow |
| 3.6 | Home screen widgets | `feature-widgets` | Glance widgets: daily summary, quick capture, next task, energy score |
| 3.7 | BLE wearable bridge | `core-bluetooth` | Scan, pair, read heart rate and steps from wearables |
| 3.8 | Proactive agent | `feature-daily-review` (enhanced) | Between-review nudges: task due, pattern detected, person nearby |
| 3.9 | Sensor integration | `data-health` | Accelerometer activity detection, barometer altitude, motion analysis |
| 3.10 | Picture-in-Picture | `feature-quebot` | QueBot chat overlay while using other apps |

### Exit Criteria

- Health data from Health Connect displayed in dashboard.
- Energy score computed from health + activity data.
- On-device search returns relevant results without network.
- Vault items encrypted and inaccessible without biometric auth.
- At least one widget installable on home screen.
- Geofence triggers context switch notifications.

---

## Timeline Guidance

These are effort estimates, not calendar dates. Actual timeline depends on team size and backend readiness.

| Phase | Estimated Effort | Key Blocker |
|---|---|---|
| Phase 1 | 2-3 weeks | None (self-contained) |
| Phase 2 | 6-8 weeks | HumanOS bridge JWT endpoint must be deployed before real auth works |
| Phase 3 | 8-12 weeks | Health Connect requires Android 14+ testing; TFLite model training pipeline |

## Dependency Chain

```
Phase 1 (no blockers)
    |
    +--→ Phase 2 (blocked by: bridge JWT endpoint, Firebase project ID confirmation)
              |
              +--→ Phase 3 (blocked by: Phase 2 sync stability, TFLite model availability)
```

## Risk Register

| Risk | Phase | Impact | Mitigation |
|---|---|---|---|
| Bridge JWT endpoint not built | 2 | Cannot authenticate against HumanOS API | Fake gateway allows all other Phase 2 work to proceed |
| Firebase project ID mismatch | 2 | Auth tokens rejected by QueBot | Runtime validation of project ID in token claims |
| Health Connect API instability | 3 | Health features unreliable | Abstraction layer allows swap to alternative data source |
| TFLite model too large | 3 | App size exceeds user tolerance | Download model on demand, not bundled in APK |
| Play Store background location policy | 3 | Background location permission rejected | Strong justification doc; foreground-only fallback |
