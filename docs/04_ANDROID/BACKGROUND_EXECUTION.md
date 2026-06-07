# Background Execution Strategy

> humanOS Native Android -- WorkManager and Foreground Services
> Last updated: 2026-06-06

## Principles

1. **WorkManager is the default** for all deferrable background work.
2. **Foreground Services** only for user-initiated, ongoing tasks that require continuous execution.
3. **Respect battery**: always set appropriate constraints; never poll on a timer.
4. **Idempotent workers**: every Worker must handle being re-executed safely.
5. **Observable progress**: all background work exposes progress via `WorkInfo` for UI feedback.

## WorkManager Tasks

### 1. Periodic Sync (`PeriodicSyncWorker`)

| Property | Value |
|---|---|
| Type | `PeriodicWorkRequest` |
| Interval | 15 minutes (minimum enforced by WorkManager) |
| Flex window | 5 minutes |
| Constraints | `NetworkType.CONNECTED` |
| Backoff | Exponential, initial 30s, max 5 hours |
| Tags | `sync`, `periodic` |

**Behavior**: Syncs pending local changes (captures, task updates, agent responses) to HumanOS backend. Pulls latest data since last sync timestamp. Uses `SyncManager` from `core-sync` module.

**Conflict resolution**: Last-write-wins with server timestamp. Conflicts logged to `TraceEvent` for audit.

### 2. Daily Agent Briefing (`DailyBriefingWorker`)

| Property | Value |
|---|---|
| Type | `PeriodicWorkRequest` |
| Interval | 24 hours |
| Initial delay | Calculated to target user-configured time (default 07:00 local) |
| Constraints | `NetworkType.CONNECTED` |
| Backoff | Linear, initial 15 minutes |
| Tags | `briefing`, `daily` |

**Behavior**: Fetches daily briefing from HumanOS agent endpoint. Composes briefing from: pending tasks, calendar events (if permission granted), health summary (if available), priority alerts. Stores result in Room. Posts local notification on channel `daily_agent`.

**Rescheduling**: If user changes preferred briefing time in Settings, cancel existing work and re-enqueue with recalculated initial delay.

### 3. Token Refresh (`TokenRefreshWorker`)

| Property | Value |
|---|---|
| Type | `OneTimeWorkRequest`, enqueued with calculated delay |
| Trigger | Scheduled for (token_expiry - 5 minutes) |
| Constraints | `NetworkType.CONNECTED` |
| Backoff | Exponential, initial 10s |
| Tags | `auth`, `token` |

**Behavior**: Refreshes the active authentication token (Firebase for QueBot, bridge JWT for HumanOS) before it expires. On success, stores new token in encrypted DataStore and enqueues next refresh. On failure after max retries, posts notification asking user to re-authenticate.

**Dual-token handling**: Separate `UniqueWorkName` for each token type. Firebase token managed by Firebase SDK internally; this worker handles the HumanOS bridge JWT only.

### 4. Capture Upload Queue (`CaptureUploadWorker`)

| Property | Value |
|---|---|
| Type | `OneTimeWorkRequest` per capture, chained |
| Constraints | `NetworkType.CONNECTED`, `StorageNotLow` |
| Backoff | Exponential, initial 30s |
| Tags | `capture`, `upload` |

**Behavior**: Uploads pending captures (photos, audio, documents) from local Room queue to HumanOS storage endpoint. Marks capture as `UPLOADED` on success. Cleans up local file after confirmed server receipt.

**Chaining**: Multiple captures are chained via `WorkContinuation` to avoid flooding the network. Ordered by creation timestamp (oldest first).

**Large files**: Files over 5MB use chunked upload with resume support.

## WorkManager Constraints Reference

| Constraint | Used By | Purpose |
|---|---|---|
| `NetworkType.CONNECTED` | All sync/upload workers | Cannot sync without network |
| `NetworkType.UNMETERED` | _Not used_ (user may only have mobile data) | -- |
| `BatteryNotLow` | Heavy processing (future: on-device AI batch) | Avoid draining battery during intensive work |
| `StorageNotLow` | Capture upload | Ensure space for temp files during upload |
| `DeviceIdle` | _Not used_ (briefings are time-sensitive) | -- |
| `RequiresCharging` | _Not used in Phase 1_ | Future: batch AI processing |

## Foreground Services

Foreground Services are used only when the user explicitly starts an ongoing action that requires continuous execution. Each foreground service shows a persistent notification with controls.

### 1. Active Voice Recording (`VoiceRecordingService`)

| Property | Value |
|---|---|
| Type | `FOREGROUND_SERVICE_TYPE_MICROPHONE` |
| Notification channel | `capture` |
| Notification content | "Recording audio..." with Stop button |
| Lifecycle | Starts when user taps Record, stops on Stop or app-configured max duration |

**Behavior**: Records audio via `MediaRecorder`. Saves to app-internal storage. On stop, creates `CaptureItem` in Room and enqueues `CaptureUploadWorker`.

### 2. Active Location Tracking (`LocationTrackingService`)

| Property | Value |
|---|---|
| Type | `FOREGROUND_SERVICE_TYPE_LOCATION` |
| Notification channel | `capture` |
| Notification content | "Tracking location for field inspection..." with Stop button |
| Lifecycle | Starts when user begins a field inspection, stops when inspection ends |

**Behavior**: Requests location updates via `FusedLocationProviderClient` at medium priority (PRIORITY_BALANCED_POWER_ACCURACY). Records waypoints. On stop, attaches location trail to the active capture or inspection record.

## WorkManager Initialization

```kotlin
// In Application.onCreate() or via Hilt
val config = Configuration.Builder()
    .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR)
    .setWorkerFactory(hiltWorkerFactory)  // Hilt-injected worker dependencies
    .build()

WorkManager.initialize(applicationContext, config)
```

- Custom `WorkerFactory` via Hilt for dependency injection into Workers.
- `@HiltWorker` annotation on each Worker class.
- `@AssistedInject` constructor for `Context` and `WorkerParameters`.

## Monitoring and Debugging

- All workers log start/complete/failure to `TraceEvent` table in Room.
- Debug builds expose WorkManager Inspector via Android Studio.
- `WorkManager.getInstance(context).getWorkInfosByTag(tag)` for programmatic status checks.
- Failure notifications posted to `sync` channel with retry/details actions.

## Battery Optimization

- App should guide users to exclude it from battery optimization (via `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) only if sync reliability is critical and user has opted in.
- Never request exemption during onboarding; only after user reports missed briefings.
- Document battery impact in Play Store Data Safety section.

## References

- DEC-007: Offline-first sync architecture
- ADR-0002: Module architecture (core-sync module)
- Android developer docs: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- Android developer docs: [Foreground services](https://developer.android.com/develop/background-work/services/foreground-services)
