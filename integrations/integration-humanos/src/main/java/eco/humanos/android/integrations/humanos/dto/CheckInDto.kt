package eco.humanos.android.integrations.humanos.dto

import kotlinx.serialization.Serializable

/**
 * Request body for `POST /api/check-ins` — a wellbeing / health check-in.
 *
 * @property energy Subjective energy level, 1 (drained) to 5 (energised).
 * @property mood Free-text or coded mood label (e.g. "calm", "anxious").
 * @property stress Subjective stress level, 1 (none) to 5 (overwhelmed).
 * @property perceivedLoad Subjective workload, 1 (light) to 5 (crushing).
 * @property note Optional free-text note.
 */
@Serializable
data class CreateCheckInDto(
    val energy: Int,
    val mood: String? = null,
    val stress: Int? = null,
    val perceivedLoad: Int? = null,
    val note: String? = null,
)

/**
 * Server response for a recorded check-in.
 *
 * @property id Server-assigned check-in id.
 * @property createdAt Epoch millis the check-in was recorded.
 */
@Serializable
data class CheckInDto(
    val id: String,
    val energy: Int,
    val mood: String? = null,
    val stress: Int? = null,
    val perceivedLoad: Int? = null,
    val note: String? = null,
    val createdAt: Long? = null,
)
