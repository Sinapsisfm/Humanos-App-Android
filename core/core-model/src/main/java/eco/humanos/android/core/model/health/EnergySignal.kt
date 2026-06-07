package eco.humanos.android.core.model.health

import eco.humanos.android.core.model.common.IntegrationSource
import kotlinx.serialization.Serializable

/**
 * An objective health or activity measurement from a wearable,
 * Health Connect, or manual entry.
 *
 * Complements the subjective [HumanState] check-in with hard data.
 * The AI layer correlates energy signals with productivity patterns
 * to suggest optimal work/rest schedules.
 *
 * @property value The numeric measurement (e.g., 7.5 hours of sleep, 8200 steps).
 * @property unit Human-readable unit string (e.g., "hours", "steps", "bpm", "kg").
 * @property measuredAt Epoch millis when the measurement was taken.
 * @property rawDataJson Optional JSON blob with vendor-specific raw data.
 * @property createdAt Epoch millis when this record was created locally.
 */
@Serializable
data class EnergySignal(
    val id: String,
    val type: EnergySignalType,
    val value: Double,
    val unit: String,
    val measuredAt: Long,
    val source: IntegrationSource,
    val rawDataJson: String? = null,
    val createdAt: Long,
)
