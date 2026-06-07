package eco.humanos.android.core.model.context

import kotlinx.serialization.Serializable

/**
 * Lifecycle state of a context node or entity, controlling its
 * visibility and trustworthiness in the knowledge graph.
 *
 * - [CONFIRMED] — verified by the user or a trusted source.
 * - [INFERRED] — created by AI from observed patterns.
 * - [SEEDED] — pre-populated during onboarding or import.
 * - [DRAFT] — user-created but not yet finalized.
 * - [DUPLICATE_CANDIDATE] — flagged for merge review.
 * - [ARCHIVED] — soft-deleted, excluded from active queries.
 */
@Serializable
enum class GovernanceState {
    CONFIRMED,
    INFERRED,
    SEEDED,
    DRAFT,
    DUPLICATE_CANDIDATE,
    ARCHIVED,
}
