package eco.humanos.android.core.model.health

import eco.humanos.android.core.model.common.IntegrationSource
import kotlinx.serialization.Serializable

/**
 * A daily self-reported snapshot of the user's subjective well-being.
 *
 * Collected once per day via the check-in flow. Each dimension is scored
 * 1-5 and used by the AI layer to adapt task suggestions, pacing, and
 * notification frequency.
 *
 * @property date ISO date string (e.g., "2026-06-07").
 * @property energy Subjective energy level, 1 (depleted) to 5 (full).
 * @property mood Emotional state, 1 (low) to 5 (great).
 * @property stress Perceived stress, 1 (calm) to 5 (overwhelmed).
 * @property perceivedLoad Workload feeling, 1 (light) to 5 (overloaded).
 * @property createdAt Epoch millis when the check-in was submitted.
 * @property syncedAt Epoch millis of last successful sync.
 */
@Serializable
data class HumanState(
    val id: String,
    val date: String,
    val energy: Int,
    val mood: Int,
    val stress: Int,
    val perceivedLoad: Int,
    val note: String? = null,
    val source: IntegrationSource,
    val createdAt: Long,
    val syncedAt: Long? = null,
)
