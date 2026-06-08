package eco.humanos.android.integrations.humanos

import eco.humanos.android.core.model.auth.HumanOSSession
import eco.humanos.android.core.model.task.TaskItem
import eco.humanos.android.core.model.task.TaskPriority
import eco.humanos.android.integrations.humanos.dto.buildCreateTaskDto
import eco.humanos.android.integrations.humanos.dto.toDailyReview
import eco.humanos.android.integrations.humanos.dto.toDomain
import eco.humanos.android.integrations.humanos.dto.toHumanOSSession
import javax.inject.Inject

/**
 * Real [HumanosGateway] backed by [HumanosApiService] over Retrofit.
 *
 * ## Dormant by default
 * This class is wired into the Hilt graph but **not bound** to [HumanosGateway]
 * until the integration goes live — see the swap point documented in
 * `HumanosModule`. While
 * [eco.humanos.android.core.model.common.IntegrationConfig.USE_REAL_HUMANOS_AUTH]
 * is `false`, `FakeHumanosGateway` remains the bound implementation and this
 * code never runs.
 *
 * ## Auth model
 * [exchangeFirebaseToken] sends the **Firebase** ID token. Every other call
 * needs the **bridge** JWT, fetched lazily from [tokenProvider]; if no bridge
 * token is available the call fails with a clear error instead of hitting the
 * network unauthenticated.
 *
 * All fallible calls are wrapped in [runCatching] so transport/HTTP errors
 * surface as `Result.failure` rather than thrown exceptions, matching the
 * [HumanosGateway] contract and [FakeHumanosGateway]'s behaviour.
 */
class RealHumanosGateway @Inject constructor(
    private val api: HumanosApiService,
    private val tokenProvider: HumanosTokenProvider,
) : HumanosGateway {

    override suspend fun exchangeFirebaseToken(firebaseIdToken: String): Result<HumanOSSession> =
        runCatching {
            val response = api.exchangeToken(bearer(firebaseIdToken))
            response.toHumanOSSession(nowMillis = System.currentTimeMillis())
        }

    override suspend fun fetchTasks(status: String?): Result<List<TaskItem>> =
        runCatching {
            api.getTasks(requireBridgeBearer(), status).map { it.toDomain() }
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
            ).toDomain()
        }

    override suspend fun fetchDailyReview(): Result<DailyReviewDto> =
        runCatching {
            api.getTodaySnapshot(requireBridgeBearer()).toDailyReview()
        }

    override suspend fun checkConnectivity(): Boolean =
        runCatching {
            // A snapshot fetch doubles as a lightweight authenticated ping.
            val token = tokenProvider.currentBridgeToken() ?: return false
            api.getTodaySnapshot(bearer(token))
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
            ?: error("No HumanOS bridge token available; sign in / exchange first.")
        return bearer(token)
    }
}
