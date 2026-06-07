package eco.humanos.android.core.model.common

import kotlinx.serialization.Serializable

/**
 * An immutable audit-trail entry recording a mutation on any entity.
 *
 * Every create, update, or delete across the system emits a [TraceEvent],
 * enabling full provenance tracking and offline replay.
 */
@Serializable
data class TraceEvent(
    val id: String,
    val entityType: String,
    val entityId: String,
    val action: String,
    val source: IntegrationSource,
    val userId: String,
    val metadata: String? = null,
    val timestamp: Long,
)
