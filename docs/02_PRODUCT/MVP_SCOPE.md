# MVP Scope -- Phase 1

> humanOS Native Android -- What Ships First
> Last updated: 2026-06-06

## Phase 1 Goal

Build a **skeleton that compiles, navigates, and defines every interface** that Phase 2 will fill in. Phase 1 is not user-facing. It is a verified architectural foundation.

At the end of Phase 1, a developer should be able to:
1. Clone the repo and build successfully on the first try.
2. Navigate between all three screens (Dashboard, Capture, Settings).
3. Read every data model, interface, and DTO used in later phases.
4. Run the test suite with all fakes passing.
5. Understand the full architecture from documentation alone.

## What Phase 1 Includes

### Screens (3)

| Screen | Content | ViewModel | Interactive |
|---|---|---|---|
| **Dashboard** | Greeting with user name (hardcoded or from DataStore), navigation cards to Capture and Settings, placeholder "Daily Summary" section | `DashboardViewModel` | Navigation only |
| **Capture** | Text input field, "Save" button, success snackbar. Capture saved to Room locally. Voice and photo buttons visible but disabled with "Coming in Phase 2" tooltip | `CaptureViewModel` | Text capture works, saves to Room |
| **Settings** | Theme toggle (light/dark/system), account info (name, email from DataStore), logout button (clears session), app version display, privacy preference placeholder | `SettingsViewModel` | Theme toggle and logout work |

### Models Defined (in `core-model`)

All data classes, enums, and sealed types are defined and documented, even if they are only used in Phase 2+:

- `User`, `Person`, `Organization`
- `Task`, `TaskStatus`, `TaskPriority`
- `Capture`, `CaptureType` (TEXT, VOICE, PHOTO, LINK)
- `ContextNode`, `ContextNodeType`, `ContextEdge`, `RelationshipType`
- `GovernanceState`, `PrivacyLevel`, `SyncState`
- `Appointment`, `AppointmentStatus`
- `HealthProfile`, `CheckIn`, `EnergyScore`
- `TraceEvent`, `TraceEventType`
- `Resource<T>` (Success, Error, Loading)
- `SseEvent` (Status, Delta, SearchExecuted, Done, Error)

### Room Database (in `core-database`)

- Database class defined with version 1 schema.
- DAOs for `Capture` entity (insert, query, delete) -- the only entity with real CRUD in Phase 1.
- `@TypeConverter` classes for `Instant`, `LocalDate`, `enum` types, and JSON maps.
- Schema export enabled for CI diffing.
- All other table definitions present as `@Entity` but no DAOs wired yet (deferred to Phase 2).

### DataStore (in `core-datastore`)

- `UserPreferences`: theme, onboarding completed, last sync timestamps.
- `SessionState`: user ID, display name, email (non-sensitive identity info).
- Read/write functions with `Flow` emission.

### Network (in `core-network`)

- Retrofit builder with base URL configuration.
- OkHttp client with auth interceptor (reads token from `core-security`).
- `NetworkMonitor` that observes `ConnectivityManager` and exposes `Flow<Boolean>`.
- SSE `EventSource` wrapper that converts callbacks to `Flow<SseEvent>`.
- JSON converter factory using `kotlinx.serialization`.
- **No real API calls in Phase 1.** All calls are intercepted by mock implementations.

### Security (in `core-security`)

- Android Keystore wrapper with key generation and retrieval.
- `EncryptedSharedPreferences` helper for token storage.
- `BiometricGate` wrapper around `BiometricPrompt`.
- `PrivacyLevel` enforcement utilities.

### UI (in `core-ui`)

- Material 3 theme with humanOS color scheme (dynamic color support).
- Light and dark theme variants.
- Typography scale.
- Shared composables: `HumanosTopBar`, `LoadingShimmer`, `ErrorCard`, `EmptyState`, `CaptureCard`.
- Design tokens (spacing, elevation, corner radius).

### Integration Modules

- `integration-humanos`: Retrofit interface with all Phase 2 endpoints declared. DTO classes. `HumanosGateway` interface. `FakeHumanosGateway` returning canned responses.
- `integration-quebot`: Retrofit interface + SSE endpoint. DTO classes. `QuebotGateway` interface. `FakeQuebotGateway` returning canned chat responses.

### Auth (in `data-auth`)

- `AuthRepository` interface with `signIn()`, `signOut()`, `getCurrentUser()`, `observeAuthState()`.
- `FakeAuthRepository` that simulates a signed-in user.
- Firebase Google Sign-In dependency declared but not wired to real flow.

### Navigation

- Type-safe Navigation Compose routes defined for all three screens.
- `NavHost` in `MainActivity` with bottom navigation bar.
- Route graph extensible for Phase 2 screens.

### Testing (in `testing-common`)

- `TestDispatcherRule` for coroutines.
- `TestFixtures` with sample `User`, `Task`, `Capture`, `ContextNode` instances.
- In-memory Room database helper.
- Fake `NetworkMonitor` (always online / always offline).

### Build System

- Convention plugins in `build-logic/` for library, compose, hilt, room, feature, test.
- Version catalog (`libs.versions.toml`) with all dependencies pinned.
- Gradle wrapper committed.
- `.gitignore` covering IDE files, build outputs, local properties.

### Documentation

- All 15+ docs in `docs/` directories written and cross-referenced.
- ADR-001 through ADR-005 written.
- `CLAUDE.md` project instructions written.
- `BACKLOG.md` with Phase 2 work items.

## What Phase 1 Does NOT Include

| Excluded | Reason | Phase |
|---|---|---|
| Real API calls to HumanOS/QueBot | Backend bridge endpoint not built yet | Phase 2 |
| Sync (upload/download) | Requires real API + WorkManager scheduling | Phase 2 |
| Camera capture | Requires runtime permissions + CameraX setup | Phase 2 |
| Voice recording | Requires runtime permissions + MediaRecorder | Phase 2 |
| QueBot chat UI | Requires SSE streaming + message persistence | Phase 2 |
| Tasks UI | Requires sync + domain logic + list/detail screens | Phase 2 |
| Health Connect | Requires Health Connect SDK + permission flow | Phase 3 |
| Push notifications | Requires FCM registration + server-side setup | Phase 2 |
| Background sync | Requires WorkManager + real API | Phase 2 |
| On-device AI | Requires TFLite model + embedding pipeline | Phase 3 |
| Widgets | Requires Glance + data refresh worker | Phase 3 |
| Location/GPS | Requires runtime permissions + foreground service | Phase 2/3 |
| Firebase Auth (real) | Can be wired quickly but blocked on bridge JWT endpoint | Phase 2 |

## Acceptance Criteria for Phase 1 Completion

- [ ] `./gradlew assembleDebug` succeeds with zero errors.
- [ ] `./gradlew testDebugUnitTest` passes all tests.
- [ ] App launches on emulator (API 26+) and shows Dashboard.
- [ ] Navigation between Dashboard, Capture, and Settings works.
- [ ] Text capture saves to Room and appears in a debug query.
- [ ] Theme toggle (light/dark/system) persists across app restart.
- [ ] Logout clears session state in DataStore.
- [ ] All convention plugins resolve without errors.
- [ ] All module dependencies compile without circular references.
- [ ] Documentation covers every module, every model, and every integration contract.
