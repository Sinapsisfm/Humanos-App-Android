package eco.humanos.android.core.model.context

import kotlinx.serialization.Serializable

/**
 * The semantic category of a node in the personal context graph.
 *
 * Each node in the graph represents a real-world entity the user
 * interacts with. The type determines which UI, linking rules,
 * and AI enrichment pipelines apply.
 */
@Serializable
enum class ContextNodeType {
    PERSON,
    PLACE,
    PROJECT,
    EVENT,
    TASK,
    CAPTURE,
    DECISION,
    ORGANIZATION,
    DOCUMENT,
    MEDICATION,
}
