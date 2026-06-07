# Sensor Strategy

> humanOS Native Android -- Distributed Sensor Handling (DEC-009)
> Last updated: 2026-06-06

## Decision: No Centralized Sensor Module

Per DEC-009, sensors are **not** handled in a centralized `integration-android-sensors` module. Each sensor is owned by the data module that consumes its output. This avoids a god-module that couples unrelated hardware concerns and simplifies dependency graphs.

## Sensor Ownership Map

| Sensor / Hardware | Owner Module | Purpose | Phase |
|---|---|---|---|
| GPS / Fused Location | `data-terrain` | Geo-reference captures, field inspection waypoints, area detection | 2 |
| Camera | `data-capture` | Photo capture, document scanning, visual context | 1 |
| Microphone | `data-capture` | Voice recording, dictation, audio notes | 1 |
| Accelerometer | `data-health` | Activity detection (walking, running, stationary) | 3 |
| Gyroscope | `data-health` | Motion pattern analysis, activity classification refinement | 3 |
| Step Counter | `data-health` | On-device step counting (fallback when Health Connect unavailable) | 3 |
| Health Connect API | `integration-healthconnect` | Steps, sleep, heart rate, calories, blood pressure, glucose, weight | 3 |
| Biometric (fingerprint, face) | `core-security` | Vault access, sensitive operation confirmation | 3 |
| Bluetooth LE | `integration-ble` (future) | Wearable connectivity, environmental sensors | 4 |
| NFC | `integration-nfc` (future) | Asset tag reading, quick-capture triggers | 4 |

## Architecture Pattern per Sensor

Each owning module follows the same internal pattern:

```
data-{domain}/
  sensor/
    {Sensor}DataSource.kt        -- interface (testable)
    {Sensor}DataSourceImpl.kt    -- Android SDK implementation
    {Sensor}AvailabilityChecker.kt  -- checks hardware + permission
  model/
    {Sensor}Reading.kt           -- domain model for sensor data
  repository/
    {Sensor}Repository.kt        -- interface
    {Sensor}RepositoryImpl.kt    -- combines sensor + persistence
  di/
    {Sensor}Module.kt            -- Hilt bindings
```

### Example: Location in data-terrain

```kotlin
// sensor/LocationDataSource.kt
interface LocationDataSource {
    fun getLocationUpdates(
        priority: LocationPriority,
        intervalMs: Long
    ): Flow<LocationReading>
    
    suspend fun getLastKnownLocation(): LocationReading?
}

// sensor/LocationDataSourceImpl.kt
class LocationDataSourceImpl @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient,
    private val availabilityChecker: LocationAvailabilityChecker
) : LocationDataSource {
    // Implementation using FusedLocationProviderClient
}

// sensor/LocationAvailabilityChecker.kt
class LocationAvailabilityChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun isGpsAvailable(): Boolean
    fun isPermissionGranted(): Boolean
    fun isLocationEnabled(): Boolean  // system-level location toggle
}
```

## Shared Abstractions in core-common

While sensors live in their owning modules, a few shared abstractions exist in `core-common`:

```kotlin
// SensorAvailability.kt
enum class SensorAvailability {
    AVAILABLE,           // hardware present + permission granted + enabled
    HARDWARE_MISSING,    // device lacks the sensor
    PERMISSION_DENIED,   // user has not granted permission
    DISABLED,            // sensor exists but is turned off (e.g., GPS toggle)
    UNAVAILABLE          // other reason (e.g., Health Connect not installed)
}

// SensorReading.kt
interface SensorReading {
    val timestamp: Instant
    val source: SensorSource
    val accuracy: SensorAccuracy
}

enum class SensorSource {
    DEVICE_HARDWARE,     // direct sensor reading
    HEALTH_CONNECT,      // imported via Health Connect
    MANUAL_ENTRY,        // user-entered value
    DERIVED              // calculated from other readings
}

enum class SensorAccuracy {
    HIGH, MEDIUM, LOW, UNRELIABLE
}
```

## Why Not a Centralized Module?

| Concern | Centralized Module | Distributed (chosen) |
|---|---|---|
| Dependency graph | Every feature depends on the sensor module | Features depend only on their data module |
| Build times | Changes to any sensor recompile entire sensor module | Changes isolated to the affected data module |
| Testing | Must mock entire sensor facade | Mock only the sensor interface you need |
| Team ownership | Single team bottleneck | Each domain team owns their sensors |
| Permission coupling | All permissions bundled | Permissions requested per feature |
| Phase rollout | All or nothing | Sensors ship with their phase |

## Sensor Data Flow

```
Hardware Sensor
    |
    v
{Sensor}DataSourceImpl  (raw readings, Android SDK)
    |
    v
{Sensor}RepositoryImpl  (filters, deduplicates, persists to Room)
    |
    v
Domain Use Case          (business logic, aggregation)
    |
    v
ViewModel                (UI-ready state)
    |
    v
Compose UI               (display + user interaction)
```

## Testing Sensors

- **Unit tests**: Use fake `{Sensor}DataSource` implementations from `testing-common`.
- **Instrumented tests**: Use `SensorTestHelper` to simulate sensor events (limited to emulator-supported sensors).
- **Manual testing**: Physical device required for GPS, camera, microphone, accelerometer, Health Connect.
- **CI**: Sensor-dependent tests tagged with `@RequiresDevice` and excluded from emulator CI runs.

## Power Considerations

| Sensor | Battery Impact | Mitigation |
|---|---|---|
| GPS (fine) | High | Use only during active field inspections; batched updates |
| GPS (coarse) | Low | Acceptable for background area detection |
| Camera | Medium (during use) | No background camera access ever |
| Microphone | Medium (during use) | Foreground service with user-visible notification |
| Accelerometer | Low | Batched readings via SensorManager batching |
| Health Connect | Negligible | Periodic import via WorkManager, not continuous |

## References

- DEC-009: Sensor handling in respective data modules
- PERMISSIONS_STRATEGY.md: Permission requirements per sensor
- BACKGROUND_EXECUTION.md: WorkManager for periodic sensor data import
- ADR-0002: Module architecture
