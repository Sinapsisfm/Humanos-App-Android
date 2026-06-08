package eco.humanos.android.integrations.humanos.dto

import eco.humanos.android.core.model.common.IntegrationSource
import eco.humanos.android.core.model.context.GovernanceState
import eco.humanos.android.core.model.task.EntityOrigin
import eco.humanos.android.core.model.task.TaskItem
import eco.humanos.android.core.model.task.TaskPriority
import eco.humanos.android.core.model.task.TaskStatus
import kotlinx.serialization.Serializable

/**
 * Task as returned by `GET/POST /api/mobile/tasks`. Field names + value casing
 * mirror the HumanOS `PersonalTask` contract:
 *  - `status`   ∈ pending | in_progress | done | cancelled
 *  - `priority` ∈ low | medium | high | urgent
 *  - date fields are ISO-8601 **strings** (Prisma DateTime), not epoch millis.
 *
 * Enum-like fields are carried as plain strings so an unexpected server value
 * never crashes deserialization; mapping into the typed domain model happens in
 * [toDomain] with safe fallbacks.
 */
@Serializable
data class RemoteTaskDto(
    val id: String,
    val personId: String? = null,
    val title: String,
    val description: String? = null,
    val status: String = "pending",
    val priority: String = "medium",
    val dueDate: String? = null,
    val completedAt: String? = null,
    val tags: List<String> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/** Envelope for `GET /api/mobile/tasks` → `{ "tasks": [...] }`. */
@Serializable
data class TasksEnvelope(val tasks: List<RemoteTaskDto> = emptyList())

/** Envelope for `POST/PATCH /api/mobile/tasks` → `{ "task": {...} }`. */
@Serializable
data class TaskEnvelope(val task: RemoteTaskDto)

/**
 * Request body for `POST /api/mobile/tasks`. Validated server-side by the same
 * Zod schema the web uses, so `priority` MUST be one of low|medium|high|urgent
 * and `dueDate` (if present) an ISO-8601 datetime string.
 */
@Serializable
data class CreateTaskDto(
    val title: String,
    val priority: String = "medium",
    val dueDate: String? = null,
    val tags: List<String> = emptyList(),
)

/** Request body for `PATCH /api/mobile/tasks/{id}` — partial update. */
@Serializable
data class UpdateTaskDto(
    val title: String? = null,
    val status: String? = null,
    val priority: String? = null,
    val dueDate: String? = null,
)

/**
 * Map a server task into the domain [TaskItem]. Remote tasks are confirmed and
 * sourced from HumanOS, so provenance is fixed. Unknown status/priority strings
 * and unparseable dates fall back to safe defaults rather than throwing.
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
        dueDate = isoToMillis(dueDate),
        completedAt = isoToMillis(completedAt),
        tags = tags,
        origin = EntityOrigin.IMPORTED,
        governanceState = GovernanceState.CONFIRMED,
        source = IntegrationSource.HUMANOS,
        createdAt = isoToMillis(createdAt) ?: now,
        updatedAt = isoToMillis(updatedAt) ?: now,
        syncedAt = now,
    )
}

/** Build the create-request body, mapping the domain priority to the server enum. */
fun buildCreateTaskDto(
    title: String,
    description: String?,
    priority: TaskPriority,
): CreateTaskDto = CreateTaskDto(
    title = title,
    priority = priority.toServerPriority(),
)

/**
 * Domain → server priority. HumanOS uses `urgent` where the app models the top
 * tier as `CRITICAL`; everything else lowercases 1:1.
 */
fun TaskPriority.toServerPriority(): String = when (this) {
    TaskPriority.CRITICAL -> "urgent"
    else -> name.lowercase()
}

private fun parseTaskStatus(raw: String): TaskStatus =
    TaskStatus.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
        ?: TaskStatus.PENDING

private fun parseTaskPriority(raw: String): TaskPriority =
    when (raw.lowercase()) {
        "urgent" -> TaskPriority.CRITICAL
        else -> TaskPriority.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
            ?: TaskPriority.MEDIUM
    }
