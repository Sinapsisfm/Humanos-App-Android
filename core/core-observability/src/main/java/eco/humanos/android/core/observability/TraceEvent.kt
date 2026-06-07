package eco.humanos.android.core.observability

/**
 * Re-export of the canonical TraceEvent from core-model.
 *
 * The canonical TraceEvent data class lives in:
 *   eco.humanos.android.core.model.common.TraceEvent
 *
 * This file previously contained a duplicate definition (Tanda 4 stub).
 * Removed in Tanda 8 to resolve duplication identified by GPT review.
 * See DEC-012.
 *
 * core-observability depends on core-model and uses TraceEvent directly.
 * Import from: eco.humanos.android.core.model.common.TraceEvent
 */

// TraceCategory remains here as it's observability infrastructure, not domain model.
enum class TraceCategory {
    AUTH,
    NETWORK,
    DATABASE,
    UI,
    SYNC,
    SECURITY,
    CAPTURE,
    TASK,
    CONTEXT,
    HEALTH,
    TERRAIN,
    INTEGRATION,
}
