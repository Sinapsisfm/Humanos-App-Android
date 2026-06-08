package eco.humanos.android.integrations.humanos

import eco.humanos.android.core.model.auth.HumanOSSession
import eco.humanos.android.core.model.task.TaskItem
import eco.humanos.android.core.model.task.TaskPriority
import eco.humanos.android.integrations.humanos.dto.CheckInDto
import eco.humanos.android.integrations.humanos.dto.CheckInsEnvelope
import eco.humanos.android.integrations.humanos.dto.CreateCheckInDto
import eco.humanos.android.integrations.humanos.dto.MobileSnapshotDto
import eco.humanos.android.integrations.humanos.dto.PersonDto
import eco.humanos.android.integrations.humanos.dto.UpdateTaskDto
import eco.humanos.android.integrations.humanos.dto.buildCreateTaskDto
import eco.humanos.android.integrations.humanos.dto.toDailyReview
import eco.humanos.android.integrations.humanos.dto.toDomain
import eco.humanos.android.integrations.humanos.dto.toHumanOSSession
import javax.inject.Inject

/**
 * Real [HumanosGateway] backed by [HumanosApiService] over Retrofit, talking to
 * the HumanOS `/api/mobile` routes.
 *
 * ## Auth model
 * [exchangeFirebaseToken] sends the **Firebase** ID token. Every other call
 * needs the **bridge** JWT, fetched lazily from [tokenProvider]; if no bridge
 * token is available the call fails with a clear error instead of hitting the
 * network unauthenticated.
 *
 * All fallible calls are wrapped in [runCatching] so transport/HTTP errors
 * surface as `Result.failure` (carrying the underlying exception, including
 * Retrofit `HttpException` with the status code) rather than thrown exceptions.
 */
class RealHumanosGateway @Inject constructor(
    private val api: HumanosApiService,
    private val tokenProvider: HumanosTokenProvider,
) : HumanosGateway {

    override suspend fun exchangeFirebaseToken(firebaseIdToken: String): Result<HumanOSSession> =
        runCatching {
            api.exchangeToken(bearer(firebaseIdToken))
                .toHumanOSSession(nowMillis = System.currentTimeMillis())
        }

    override suspend fun fetchTasks(status: String?): Result<List<TaskItem>> =
        runCatching {
            api.getTasks(requireBridgeBearer(), status).tasks.map { it.toDomain() }
        }

    override suspend fun createTask(
        title: String,
        description: String?,
        priority: TaskPriority,
    ): Result<TaskItem> =
        runCatching {
            api.createTask(
                requireBridgeBearer(),
                buildCreateTaskDto(title, description, priority),
            ).task.toDomain()
        }

    override suspend fun updateTaskStatus(taskId: String, status: String): Result<TaskItem> =
        runCatching {
            api.updateTask(requireBridgeBearer(), taskId, UpdateTaskDto(status = status))
                .task.toDomain()
        }

    override suspend fun fetchDailyReview(): Result<DailyReviewDto> =
        runCatching {
            api.getSnapshot(requireBridgeBearer()).snapshot.toDailyReview()
        }

    override suspend fun fetchSnapshot(): Result<MobileSnapshotDto> =
        runCatching {
            api.getSnapshot(requireBridgeBearer()).snapshot
        }

    override suspend fun fetchCheckIns(): Result<CheckInsEnvelope> =
        runCatching {
            api.getCheckIns(requireBridgeBearer())
        }

    override suspend fun submitCheckIn(
        energy: Int,
        mood: Int,
        stress: Int,
        perceivedLoad: Int?,
        note: String?,
    ): Result<CheckInDto> =
        runCatching {
            api.createCheckIn(
                requireBridgeBearer(),
                CreateCheckInDto(
                    energy = energy,
                    mood = mood,
                    stress = stress,
                    perceivedLoad = perceivedLoad,
                    note = note,
                ),
            ).checkIn
        }

    override suspend fun fetchPerson(): Result<PersonDto> =
        runCatching {
            api.getPerson(requireBridgeBearer()).person
        }

    override suspend fun checkConnectivity(): Boolean =
        runCatching {
            // An authenticated profile fetch doubles as a lightweight ping.
            val token = tokenProvider.currentBridgeToken() ?: return false
            api.getPerson(bearer(token))
            true
        }.getOrDefault(false)

    /** Build an `Authorization: Bearer <token>` header value. */
    private fun bearer(token: String): String = "Bearer $token"

    /**
     * The bridge bearer header, or throw if no token is available so the
     * failure is explicit (and caught by the surrounding [runCatching]).
     */
    private suspend fun requireBridgeBearer(): String {
        val token = tokenProvider.currentBridgeToken()
            ?: error("No hay sesión HumanOS. Inicia sesión con Google para conectar.")
        return bearer(token)
    }
}
