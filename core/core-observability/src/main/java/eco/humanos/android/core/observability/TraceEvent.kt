package eco.humanos.android.core.observability

/**
 * Lightweight trace event for audit trail and observability.
 * Full implementation in Tanda 5.
 */
data class TraceEvent(
    val name: String,
    val category: TraceCategory,
    val timestampMs: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap(),
)

enum class TraceCategory {
    AUTH,
    NETWORK,
    DATABASE,
    UI,
    SYNC,
    SECURITY,
}
