package eco.humanos.android.core.model.context

import eco.humanos.android.core.model.common.IntegrationSource
import kotlinx.serialization.Serializable

/**
 * A directed, weighted relationship between two [ContextNode] entries
 * in the personal context graph.
 *
 * Edges encode how entities relate to each other (e.g., "works_at",
 * "assigned_to", "located_in") and are used by the AI layer to
 * surface relevant context during conversations and task planning.
 *
 * @property relationship Free-form label describing the edge semantics.
 * @property weight Strength of the relationship, defaults to 1.0.
 * @property source Which system created this edge.
 */
@Serializable
data class ContextEdge(
    val id: String,
    val sourceNodeId: String,
    val targetNodeId: String,
    val relationship: String,
    val weight: Float = 1.0f,
    val source: IntegrationSource,
    val createdAt: Long,
)
