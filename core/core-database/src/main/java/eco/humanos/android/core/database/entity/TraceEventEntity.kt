package eco.humanos.android.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import eco.humanos.android.core.model.common.IntegrationSource
import eco.humanos.android.core.model.common.TraceEvent

/**
 * Room persistence mirror of [TraceEvent].
 *
 * The (entityType, entityId) pair is indexed because the provenance trail is
 * queried per-entity. [source] is stored as its `.name` string. Mapping happens
 * via [toDomain] / [toEntity].
 */
@Entity(
    tableName = "trace_events",
    indices = [Index(value = ["entityType", "entityId"])],
)
data class TraceEventEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val action: String,
    val source: String,
    val userId: String,
    val metadata: String?,
    val timestamp: Long,
)

fun TraceEventEntity.toDomain(): TraceEvent = TraceEvent(
    id = id,
    entityType = entityType,
    entityId = entityId,
    action = action,
    source = IntegrationSource.valueOf(source),
    userId = userId,
    metadata = metadata,
    timestamp = timestamp,
)

fun TraceEvent.toEntity(): TraceEventEntity = TraceEventEntity(
    id = id,
    entityType = entityType,
    entityId = entityId,
    action = action,
    source = source.name,
    userId = userId,
    metadata = metadata,
    timestamp = timestamp,
)
