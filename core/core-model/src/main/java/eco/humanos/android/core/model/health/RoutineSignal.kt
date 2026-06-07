package eco.humanos.android.core.model.health

import kotlinx.serialization.Serializable

/**
 * A single routine event in the user's day (wake-up, meal, exercise, etc.).
 *
 * Tracks both scheduled and actual times so the AI can detect drift
 * between intended routines and real behavior, enabling gentle nudges.
 *
 * @property scheduledAt Epoch millis when the event was planned.
 * @property actualAt Epoch millis when the event actually occurred.
 * @property status One of: "scheduled", "completed", "skipped", "missed".
 * @property metadata Optional JSON blob with type-specific details.
 * @property createdAt Epoch millis when this record was created.
 */
@Serializable
data class RoutineSignal(
    val id: String,
    val type: RoutineSignalType,
    val scheduledAt: Long? = null,
    val actualAt: Long? = null,
    val status: String,
    val metadata: String? = null,
    val createdAt: Long,
)
