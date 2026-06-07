package eco.humanos.android.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import eco.humanos.android.core.model.common.IntegrationSource
import eco.humanos.android.core.model.context.GovernanceState
import eco.humanos.android.core.model.task.EntityOrigin
import eco.humanos.android.core.model.task.TaskItem
import eco.humanos.android.core.model.task.TaskPriority
import eco.humanos.android.core.model.task.TaskStatus

/**
 * Room persistence mirror of [TaskItem].
 *
 * Enums are stored as their `.name` strings and [tags] is persisted as a JSON
 * string through the registered `List<String>` TypeConverter. Mapping happens
 * via [toDomain] / [toEntity].
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val remoteId: String?,
    val title: String,
    val description: String?,
    val status: String,
    val priority: String,
    val dueDate: Long?,
    val completedAt: Long?,
    val tags: List<String>,
    val origin: String,
    val governanceState: String,
    val source: String,
    val recurrence: String?,
    val linkedContextNodeId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
)

fun TaskEntity.toDomain(): TaskItem = TaskItem(
    id = id,
    remoteId = remoteId,
    title = title,
    description = description,
    status = TaskStatus.valueOf(status),
    priority = TaskPriority.valueOf(priority),
    dueDate = dueDate,
    completedAt = completedAt,
    tags = tags,
    origin = EntityOrigin.valueOf(origin),
    governanceState = GovernanceState.valueOf(governanceState),
    source = IntegrationSource.valueOf(source),
    recurrence = recurrence,
    linkedContextNodeId = linkedContextNodeId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncedAt = syncedAt,
)

fun TaskItem.toEntity(): TaskEntity = TaskEntity(
    id = id,
    remoteId = remoteId,
    title = title,
    description = description,
    status = status.name,
    priority = priority.name,
    dueDate = dueDate,
    completedAt = completedAt,
    tags = tags,
    origin = origin.name,
    governanceState = governanceState.name,
    source = source.name,
    recurrence = recurrence,
    linkedContextNodeId = linkedContextNodeId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncedAt = syncedAt,
)
