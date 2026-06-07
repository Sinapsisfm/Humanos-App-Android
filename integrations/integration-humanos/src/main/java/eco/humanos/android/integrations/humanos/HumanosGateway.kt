package eco.humanos.android.integrations.humanos

import eco.humanos.android.core.model.auth.HumanOSSession
import eco.humanos.android.core.model.task.TaskItem
import eco.humanos.android.core.model.task.TaskPriority
import kotlinx.serialization.Serializable

/**
 * Gateway to the HumanOS backend API (humanos.eco).
 *
 * Abstracts all HTTP communication behind suspend functions that
 * return [Result] for fallible operations. Implementations may use
 * Retrofit, Ktor, or raw OkHttp under the hood.
 */
interface HumanosGateway {

    /** Exchange a Firebase ID token for a HumanOS bearer session. */
    suspend fun exchangeFirebaseToken(firebaseIdToken: String): Result<HumanOSSession>

    /** Fetch tasks, optionally filtered by status (e.g. "PENDING", "DONE"). */
    suspend fun fetchTasks(status: String? = null): Result<List<TaskItem>>

    /** Create a new task on the server and return the server-assigned entity. */
    suspend fun createTask(
        title: String,
        description: String?,
        priority: TaskPriority,
    ): Result<TaskItem>

    /** Fetch the daily review summary for the current user. */
    suspend fun fetchDailyReview(): Result<DailyReviewDto>

    /** Quick connectivity check against the HumanOS health endpoint. */
    suspend fun checkConnectivity(): Boolean
}

/**
 * Data transfer object for the daily review endpoint response.
 */
@Serializable
data class DailyReviewDto(
    val date: String,
    val summary: String,
    val pendingTaskCount: Int,
    val urgentItems: List<String>,
)
