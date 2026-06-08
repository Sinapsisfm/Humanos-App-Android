package eco.humanos.android.core.observability

import eco.humanos.android.core.database.dao.TraceEventDao
import eco.humanos.android.core.database.entity.toDomain
import eco.humanos.android.core.database.entity.toEntity
import eco.humanos.android.core.model.common.TraceEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed implementation of [TraceRepository]. Persists the audit trail
 * to SQLite so TraceEvents survive process death (unlike the previous
 * in-memory implementation).
 *
 * The provenance/audit trail is part of the product (DEC-004), so it must
 * be durable. Replaces InMemoryTraceRepository per Tanda 19.
 */
@Singleton
class RoomTraceRepository @Inject constructor(
    private val traceEventDao: TraceEventDao,
) : TraceRepository {

    override suspend fun logEvent(event: TraceEvent) {
        traceEventDao.insert(event.toEntity())
    }

    override fun observeEventsForEntity(
        entityType: String,
        entityId: String,
    ): Flow<List<TraceEvent>> =
        traceEventDao.observeForEntity(entityType, entityId)
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun getRecentEvents(limit: Int): List<TraceEvent> =
        traceEventDao.getRecent(limit).map { it.toDomain() }

    override suspend fun pruneEventsBefore(timestampMillis: Long) {
        traceEventDao.pruneBefore(timestampMillis)
    }
}
