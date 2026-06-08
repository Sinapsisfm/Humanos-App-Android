package eco.humanos.android.integrations.humanos.dto

import eco.humanos.android.integrations.humanos.DailyReviewDto
import kotlinx.serialization.Serializable

/**
 * Response of `GET /api/memory/snapshot/today` — the daily context summary.
 *
 * Aggregates the user's open tasks, pending bills, active alerts and recent
 * decisions into a single payload the app renders on the dashboard / daily
 * review screen.
 *
 * @property date ISO date (yyyy-MM-dd) the snapshot is for.
 * @property summary Natural-language summary of the day.
 * @property openTasks Count of currently open tasks.
 * @property bills Outstanding bills / payment reminders.
 * @property alerts Active alerts requiring attention.
 * @property decisions Recent or pending decisions surfaced by HumanOS.
 */
@Serializable
data class DailySnapshotDto(
    val date: String,
    val summary: String = "",
    val openTasks: Int = 0,
    val bills: List<SnapshotItemDto> = emptyList(),
    val alerts: List<SnapshotItemDto> = emptyList(),
    val decisions: List<SnapshotItemDto> = emptyList(),
)

/**
 * A single line item inside a [DailySnapshotDto] section.
 *
 * @property label Short human-readable label.
 * @property detail Optional longer description.
 * @property dueDate Optional epoch millis due date (for bills / time-bound items).
 */
@Serializable
data class SnapshotItemDto(
    val label: String,
    val detail: String? = null,
    val dueDate: Long? = null,
)

/**
 * Request body for `POST /api/context/snapshot` — push a client-side context
 * snapshot up to HumanOS (e.g. local signals to enrich the daily summary).
 */
@Serializable
data class CreateSnapshotDto(
    val date: String,
    val note: String? = null,
    val signals: Map<String, String> = emptyMap(),
)

/**
 * Adapt the server snapshot to the [DailyReviewDto] the [HumanosGateway]
 * exposes today, so existing callers keep working unchanged when the real
 * gateway is switched on.
 *
 * "Urgent items" are derived from the alerts plus any bills, surfacing the
 * most time-sensitive lines first.
 */
fun DailySnapshotDto.toDailyReview(): DailyReviewDto {
    val urgent = buildList {
        alerts.forEach { add(it.label) }
        bills.forEach { add(it.label) }
    }
    return DailyReviewDto(
        date = date,
        summary = summary,
        pendingTaskCount = openTasks,
        urgentItems = urgent,
    )
}
