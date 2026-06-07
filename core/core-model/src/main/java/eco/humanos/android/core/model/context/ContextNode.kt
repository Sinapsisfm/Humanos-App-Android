package eco.humanos.android.core.model.context

import eco.humanos.android.core.model.common.IntegrationSource
import eco.humanos.android.core.model.common.PrivacyLevel
import kotlinx.serialization.Serializable

/**
 * A node in the user's personal context graph.
 *
 * Represents any real-world entity (person, place, project, etc.)
 * that the user has interacted with or that the system has inferred.
 * Nodes are connected by [ContextEdge] relationships to form the
 * knowledge graph that powers contextual AI features.
 *
 * @property id Local UUID, generated on device.
 * @property remoteId Server-side ID after first sync, null until then.
 * @property payload Arbitrary JSON blob for type-specific extra fields.
 * @property createdAt Epoch millis when the node was first created.
 * @property updatedAt Epoch millis of the last local mutation.
 * @property syncedAt Epoch millis of the last successful sync, null if never synced.
 * @property deletedAt Epoch millis of soft-delete, null if active.
 */
@Serializable
data class ContextNode(
    val id: String,
    val remoteId: String? = null,
    val type: ContextNodeType,
    val label: String,
    val summary: String? = null,
    val source: IntegrationSource,
    val privacyLevel: PrivacyLevel,
    val governanceState: GovernanceState,
    val payload: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val deletedAt: Long? = null,
)
