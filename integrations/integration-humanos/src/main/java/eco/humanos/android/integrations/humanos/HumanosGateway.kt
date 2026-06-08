package eco.humanos.android.integrations.humanos

import eco.humanos.android.core.model.auth.HumanOSSession
import eco.humanos.android.core.model.task.TaskItem
import eco.humanos.android.core.model.task.TaskPriority
import eco.humanos.android.integrations.humanos.dto.CheckInDto
import eco.humanos.android.integrations.humanos.dto.CheckInsEnvelope
import eco.humanos.android.integrations.humanos.dto.MobileSnapshotDto
import eco.humanos.android.integrations.humanos.dto.PersonDto
import kotlinx.serialization.Serializable

/**
 * Gateway to the HumanOS backend API (humanos.eco), `/api/mobile`.
 *
 * Abstracts all HTTP communication behind suspend functions that return
 * [Result] for fallible operations. Implementations may use Retrofit, Ktor, or
 * raw OkHttp under the hood.
 */
interface HumanosGateway {

    /** Exchange a Firebase ID token for a HumanOS bearer session. */
    suspend fun exchangeFirebaseToken(firebaseIdToken: String): Result<HumanOSSession>

    /** Fetch tasks, optionally filtered by status (e.g. "pending", "done"). */
    suspend fun fetchTasks(status: String? = null): Result<List<TaskItem>>

    /** Create a new task on the server and return the server-assigned entity. */
    suspend fun createTask(
        title: String,
        description: String?,
        priority: TaskPriority,
    ): Result<TaskItem>

    /** Update a task's status (e.g. "done") and return the updated entity. */
    suspend fun updateTaskStatus(taskId: String, status: String): Result<TaskItem>

    /** Fetch the daily review summary for the current user. */
    suspend fun fetchDailyReview(): Result<DailyReviewDto>

    /** Fetch the aggregated "today" snapshot for the dashboard. */
    suspend fun fetchSnapshot(): Result<MobileSnapshotDto>

    /** Fetch recent check-ins plus today's (for the wellbeing card). */
    suspend fun fetchCheckIns(): Result<CheckInsEnvelope>

    /** Record (upsert) today's wellbeing check-in. Scores are 1..5. */
    suspend fun submitCheckIn(
        energy: Int,
        mood: Int,
        stress: Int,
        perceivedLoad: Int? = null,
        note: String? = null,
    ): Result<CheckInDto>

    /** Fetch the signed-in user's HumanOS profile. */
    suspend fun fetchPerson(): Result<PersonDto>

    /** Quick authenticated connectivity check. */
    suspend fun checkConnectivity(): Boolean
}

/**
 * Data transfer object for the daily review summary (derived from the mobile
 * snapshot). Kept for backward compatibility with existing callers.
 */
@Serializable
data class DailyReviewDto(
    val date: String,
    val summary: String,
    val pendingTaskCount: Int,
    val urgentItems: List<String>,
)
