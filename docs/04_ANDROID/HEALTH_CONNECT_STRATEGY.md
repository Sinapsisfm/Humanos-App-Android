# Health Connect Integration Strategy

> humanOS Native Android -- Phase 3 Health Data Import
> Last updated: 2026-06-06

## Overview

Health Connect (formerly Google Health Services) is Android's unified health data platform. humanOS integrates as a **read-only consumer**, importing health signals to enrich the user's energy, wellness, and productivity dashboards.

## Phase

**Phase 3** -- Health Connect integration is not part of MVP. It depends on core infrastructure (Room, WorkManager, sync) being stable.

## Scope: Read-Only

humanOS **reads** health data from Health Connect. It does **not write** health data back. This simplifies permissions, reduces liability, and avoids conflicts with the user's primary health tracking app.

## Data Types

| Health Connect Data Type | Permission | humanOS Entity | Usage |
|---|---|---|---|
| Steps | `health.permission.READ_STEPS` | `EnergySignal(type=STEPS)` | Activity level, daily movement goals |
| Sleep | `health.permission.READ_SLEEP` | `EnergySignal(type=SLEEP)` | Sleep quality, energy correlation |
| Heart Rate | `health.permission.READ_HEART_RATE` | `EnergySignal(type=HEART_RATE)` | Stress detection, resting HR trends |
| Active Calories | `health.permission.READ_ACTIVE_CALORIES` | `EnergySignal(type=CALORIES)` | Energy expenditure tracking |
| Blood Pressure | `health.permission.READ_BLOOD_PRESSURE` | `EnergySignal(type=BLOOD_PRESSURE)` | Health monitoring dashboard |
| Blood Glucose | `health.permission.READ_BLOOD_GLUCOSE` | `EnergySignal(type=GLUCOSE)` | Health monitoring dashboard |
| Weight | `health.permission.READ_WEIGHT` | `EnergySignal(type=WEIGHT)` | Weight trends, body composition |

## Module: integration-healthconnect

```
integration-healthconnect/
  src/main/kotlin/.../healthconnect/
    HealthConnectGateway.kt          -- public interface
    HealthConnectGatewayImpl.kt      -- SDK implementation
    HealthConnectAvailability.kt     -- checks SDK availability
    HealthConnectPermissions.kt      -- permission request helpers
    mapper/
      StepsMapper.kt                 -- Steps → EnergySignal
      SleepMapper.kt                 -- SleepSession → EnergySignal
      HeartRateMapper.kt             -- HeartRate → EnergySignal
      CaloriesMapper.kt              -- ActiveCalories → EnergySignal
      BloodPressureMapper.kt         -- BloodPressure → EnergySignal
      GlucoseMapper.kt               -- BloodGlucose → EnergySignal
      WeightMapper.kt                -- Weight → EnergySignal
    worker/
      HealthConnectSyncWorker.kt     -- periodic import via WorkManager
    di/
      HealthConnectModule.kt         -- Hilt bindings
```

## Gateway Interface

```kotlin
interface HealthConnectGateway {
    
    /** Check if Health Connect is available on this device */
    suspend fun checkAvailability(): HealthConnectStatus
    
    /** Request read permissions for configured data types */
    suspend fun requestPermissions(): PermissionResult
    
    /** Check which permissions are currently granted */
    suspend fun getGrantedPermissions(): Set<HealthDataType>
    
    /** Import data of a specific type since the given timestamp */
    suspend fun importData(
        type: HealthDataType,
        since: Instant,
        until: Instant = Instant.now()
    ): List<EnergySignal>
    
    /** Import all granted data types since last sync */
    suspend fun importAll(since: Instant): ImportResult
}

enum class HealthConnectStatus {
    AVAILABLE,                  // SDK present, ready to use
    NOT_INSTALLED,              // Health Connect app not installed
    NOT_SUPPORTED,              // Android version too old (requires API 28+)
    UPDATE_REQUIRED,            // Health Connect app needs update
    CLIENT_NOT_AVAILABLE        // SDK initialization failed
}

enum class HealthDataType {
    STEPS, SLEEP, HEART_RATE, CALORIES,
    BLOOD_PRESSURE, GLUCOSE, WEIGHT
}

data class ImportResult(
    val imported: Map<HealthDataType, Int>,  // type → count
    val errors: Map<HealthDataType, Throwable>,
    val syncedUntil: Instant
)
```

## Periodic Import via WorkManager

```kotlin
@HiltWorker
class HealthConnectSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val healthConnectGateway: HealthConnectGateway,
    private val energySignalRepository: EnergySignalRepository,
    private val syncStateStore: SyncStateStore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val lastSync = syncStateStore.getLastHealthConnectSync()
        val result = healthConnectGateway.importAll(since = lastSync)
        
        result.imported.values.flatten().forEach { signal ->
            energySignalRepository.insert(signal)
        }
        
        syncStateStore.setLastHealthConnectSync(result.syncedUntil)
        return if (result.errors.isEmpty()) Result.success() else Result.retry()
    }
}
```

| Property | Value |
|---|---|
| Type | `PeriodicWorkRequest` |
| Interval | 1 hour |
| Constraints | `NetworkType.NOT_REQUIRED` (Health Connect is local) |
| Tags | `healthconnect`, `sync` |

## EnergySignal Entity

```kotlin
@Entity(tableName = "energy_signals")
data class EnergySignal(
    @PrimaryKey val id: String,           // UUID
    val type: EnergySignalType,
    val value: Double,                     // primary numeric value
    val unit: String,                      // "steps", "hours", "bpm", etc.
    val timestamp: Instant,                // when the measurement occurred
    val source: SignalSource,              // HEALTH_CONNECT, MANUAL, DEVICE
    val sourceAppPackage: String?,         // originating app (e.g., "com.google.android.apps.fitness")
    val rawData: String?,                  // JSON blob of original Health Connect record (audit)
    val importedAt: Instant,               // when humanOS imported it
    val syncedToServer: Boolean = false    // whether it has been synced to HumanOS backend
)

enum class EnergySignalType {
    STEPS, SLEEP, HEART_RATE, CALORIES,
    BLOOD_PRESSURE, GLUCOSE, WEIGHT
}

enum class SignalSource {
    HEALTH_CONNECT,
    MANUAL_ENTRY,
    DEVICE_SENSOR
}
```

## Privacy and Data Handling

### Raw Data Retention

- **Raw Health Connect records** are stored as JSON in `EnergySignal.rawData` for audit purposes.
- Raw data is kept locally for 90 days, then pruned by a scheduled cleanup worker.
- Raw data is **never synced** to the HumanOS backend. Only aggregated/derived values are synced.

### Aggregated Data for Display

- UI dashboards show daily/weekly/monthly aggregations, not individual readings.
- Aggregation happens in domain-layer use cases, not in the data layer.
- Example: "7,423 steps today" (aggregated), not "142 steps at 09:14, 87 steps at 09:21..." (raw).

### Data Synced to Server

Only these derived values are synced to HumanOS backend:
- Daily step total
- Sleep duration and quality score (derived)
- Average resting heart rate (daily)
- Daily active calories
- Latest blood pressure reading (if user consents)
- Latest glucose reading (if user consents)
- Latest weight reading

### User Controls

- User can disable Health Connect integration entirely in Settings.
- User can disable individual data types (e.g., import steps but not sleep).
- User can delete all imported health data from humanOS (local + server).
- Health data is excluded from AI agent context unless user explicitly opts in.

## Availability Handling

```kotlin
when (healthConnectGateway.checkAvailability()) {
    AVAILABLE -> proceedWithSetup()
    NOT_INSTALLED -> showInstallPrompt()      // deep link to Play Store
    NOT_SUPPORTED -> showUnsupportedMessage()  // Android < 9
    UPDATE_REQUIRED -> showUpdatePrompt()      // deep link to Play Store
    CLIENT_NOT_AVAILABLE -> showRetryOption()
}
```

## Testing

- **Unit tests**: Mock `HealthConnectGateway` interface. Test mappers with known input records.
- **Instrumented tests**: Use Health Connect Testing library to insert fake records.
- **Manual testing**: Requires a device with Health Connect installed and a source app (Google Fit, Samsung Health, etc.) providing data.
- **CI**: Health Connect tests require emulator API 34+ with Health Connect APK sideloaded.

## Play Store Compliance

- App must declare Health Connect permissions in `AndroidManifest.xml`.
- Privacy Policy must enumerate each health data type read and its purpose.
- Health data policy compliance form required during Play Store review.
- App must handle permission revocation gracefully (Health Connect allows per-type revocation).

## References

- DEC-009: Sensors in respective data modules (Health Connect is an integration module, not a sensor)
- PERMISSIONS_STRATEGY.md: Health Connect permission inventory
- BACKGROUND_EXECUTION.md: WorkManager for periodic import
- Android developer docs: [Health Connect](https://developer.android.com/health-and-fitness/guides/health-connect)
