package eco.humanos.android.core.model.health

import kotlinx.serialization.Serializable

/**
 * Categories of objective health and activity measurements,
 * typically sourced from Health Connect or wearable integrations.
 */
@Serializable
enum class EnergySignalType {
    SLEEP,
    STEPS,
    HEART_RATE,
    ACTIVE_MINUTES,
    CALORIES,
    BLOOD_PRESSURE,
    GLUCOSE,
    WEIGHT,
}
