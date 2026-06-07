package eco.humanos.android.core.observability

import eco.humanos.android.core.model.common.TraceEvent
import kotlinx.coroutines.flow.Flow

/**
 * Persistence and query interface for audit-trail [TraceEvent] entries.
 *
 * Every mutation across the system (create, update, delete) is recorded
 * as a [TraceEvent]. This repository provides write, observe, and
 * pruning operations for the local event store.
 */
interface TraceRepository {

    /** Persist a single trace event to the local store. */
    suspend fun logEvent(event: TraceEvent)

    /** Observe all trace events for a given entity type and ID, newest first. */
    fun observeEventsForEntity(entityType: String, entityId: String): Flow<List<TraceEvent>>

    /** Retrieve the most recent trace events across all entities. */
    suspend fun getRecentEvents(limit: Int = 50): List<TraceEvent>

    /** Delete all trace events with a timestamp before [timestampMillis]. */
    suspend fun pruneEventsBefore(timestampMillis: Long)
}
