package eco.humanos.android.integrations.humanos.dto

import eco.humanos.android.integrations.humanos.DailyReviewDto
import kotlinx.serialization.Serializable

/**
 * Response of `GET /api/mobile/snapshot` — the aggregated "today" view for the
 * dashboard. Shape:
 *
 * ```
 * { "snapshot": {
 *     "generatedAt": "<iso>",
 *     "user":   { "firstName", "loadScore", "lifeAdminScore" },
 *     "counts": { "tasksOpen", "tasksOverdue", "tasksDueToday" },
 *     "checkInToday": CheckIn | null,
 *     "tasks": RemoteTask[]   // open tasks, soonest due first
 * } }
 * ```
 */
@Serializable
data class SnapshotEnvelope(val snapshot: MobileSnapshotDto)

@Serializable
data class MobileSnapshotDto(
    val generatedAt: String? = null,
    val user: SnapshotUserDto = SnapshotUserDto(),
    val counts: SnapshotCountsDto = SnapshotCountsDto(),
    val checkInToday: CheckInDto? = null,
    val tasks: List<RemoteTaskDto> = emptyList(),
)

@Serializable
data class SnapshotUserDto(
    val firstName: String? = null,
    val loadScore: Int = 0,
    val lifeAdminScore: Int = 0,
)

@Serializable
data class SnapshotCountsDto(
    val tasksOpen: Int = 0,
    val tasksOverdue: Int = 0,
    val tasksDueToday: Int = 0,
)

/**
 * Adapt the mobile snapshot to the legacy [DailyReviewDto] the gateway still
 * exposes via `fetchDailyReview()`, so existing callers keep working.
 */
fun MobileSnapshotDto.toDailyReview(): DailyReviewDto = DailyReviewDto(
    date = generatedAt?.substringBefore("T") ?: "",
    summary = buildString {
        val name = user.firstName?.takeIf { it.isNotBlank() }
        if (name != null) append("Hola $name. ")
        append("${counts.tasksOpen} tareas abiertas")
        if (counts.tasksOverdue > 0) append(", ${counts.tasksOverdue} atrasadas")
        if (counts.tasksDueToday > 0) append(", ${counts.tasksDueToday} para hoy")
        append(".")
    },
    pendingTaskCount = counts.tasksOpen,
    urgentItems = tasks
        .filter { it.priority.equals("urgent", ignoreCase = true) || it.priority.equals("high", ignoreCase = true) }
        .map { it.title },
)
