package eco.humanos.android.integrations.humanos.dto

import eco.humanos.android.core.model.common.IntegrationSource
import eco.humanos.android.core.model.context.GovernanceState
import eco.humanos.android.core.model.task.EntityOrigin
import eco.humanos.android.core.model.task.TaskItem
import eco.humanos.android.core.model.task.TaskPriority
import eco.humanos.android.core.model.task.TaskStatus
import kotlinx.serialization.Serializable

/**
 * Task as returned by `GET /api/tasks` and `POST /api/tasks`.
 *
 * Field names mirror the HumanOS REST contract. Enum-like fields are carried
 * as plain strings so an unexpected server value never crashes deserialization;
 * mapping into the strongly-typed domain model happens in [toDomain] with safe
 * fallbacks.
 *
 * @property id Server-assigned task id (the remote id from the app's perspective).
 * @property status One of PENDING / IN_PROGRESS / DONE / CANCELLED (case-insensitive on read).
 * @property priority One of LOW / MEDIUM / HIGH / CRITICAL (case-insensitive on read).
 * @property dueDate Epoch millis the task is due, null if none.
 * @property completedAt Epoch millis the task was completed, null if open.
 * @property createdAt Epoch millis the task was created on the server.
 * @property updatedAt Epoch millis of the last server-side mutation.
 */
@Serializable
data class RemoteTaskDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val status: String,
    val priority: String,
    val dueDate: Long? = null,
    val completedAt: Long? = null,
    val tags: List<String> = emptyList(),
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
)

/**
 * Request body for `POST /api/tasks`.
 */
@Serializable
data class CreateTaskDto(
    val title: String,
    val description: String? = null,
    val priority: String = TaskPriority.MEDIUM.name,
    val dueDate: Long? = null,
    val tags: List<String> = emptyList(),
)

/**
 * Map a server task into the domain [TaskItem].
 *
 * Remote tasks are, by definition, confirmed and sourced from HumanOS, so
 * those provenance fields are fixed here. Unknown status/priority strings fall
 * back to safe defaults rather than throwing.
 */
fun RemoteTaskDto.toDomain(): TaskItem {
    val now = System.currentTimeMillis()
    return TaskItem(
        id = id,
        remoteId = id,
        title = title,
        description = description,
        status = parseTaskStatus(status),
        priority = parseTaskPriority(priority),
        dueDate = dueDate,
        completedAt = completedAt,
        tags = tags,
        origin = EntityOrigin.IMPORTED,
        governanceState = GovernanceState.CONFIRMED,
        source = IntegrationSource.HUMANOS,
        createdAt = createdAt ?: now,
        updatedAt = updatedAt ?: now,
        syncedAt = now,
    )
}

/** Build the create-request body for a new task. */
fun buildCreateTaskDto(
    title: String,
    description: String?,
    priority: TaskPriority,
): CreateTaskDto = CreateTaskDto(
    title = title,
    description = description,
    priority = priority.name,
)

private fun parseTaskStatus(raw: String): TaskStatus =
    TaskStatus.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
        ?: TaskStatus.PENDING

private fun parseTaskPriority(raw: String): TaskPriority =
    TaskPriority.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
        ?: TaskPriority.MEDIUM
