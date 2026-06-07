package eco.humanos.android.core.observability

import eco.humanos.android.core.model.common.TraceEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple in-memory implementation of [TraceRepository] for development
 * and early phases. Events are stored in a mutable list and lost on
 * process death. Will be replaced by a Room-backed implementation
 * in a later phase.
 */
@Singleton
class InMemoryTraceRepository @Inject constructor() : TraceRepository {

    private val events = mutableListOf<TraceEvent>()

    override suspend fun logEvent(event: TraceEvent) {
        events.add(event)
    }

    override fun observeEventsForEntity(
        entityType: String,
        entityId: String,
    ): Flow<List<TraceEvent>> = flow {
        emit(events.filter { it.entityType == entityType && it.entityId == entityId })
    }

    override suspend fun getRecentEvents(limit: Int): List<TraceEvent> =
        events.takeLast(limit)

    override suspend fun pruneEventsBefore(timestampMillis: Long) {
        events.removeAll { it.timestamp < timestampMillis }
    }
}
