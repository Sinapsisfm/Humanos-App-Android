package eco.humanos.android.integrations.humanos

import eco.humanos.android.integrations.humanos.dto.CheckInDto
import eco.humanos.android.integrations.humanos.dto.CreateCheckInDto
import eco.humanos.android.integrations.humanos.dto.CreateNoteDto
import eco.humanos.android.integrations.humanos.dto.CreateTaskDto
import eco.humanos.android.integrations.humanos.dto.DailySnapshotDto
import eco.humanos.android.integrations.humanos.dto.MobileExchangeResponse
import eco.humanos.android.integrations.humanos.dto.RemoteNoteDto
import eco.humanos.android.integrations.humanos.dto.RemoteTaskDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit contract for the HumanOS REST API (base URL
 * [eco.humanos.android.core.model.common.IntegrationConfig.HUMANOS_BASE_URL]).
 *
 * Endpoint paths are relative to that base (which ends in `/api/`), so e.g.
 * `@GET("tasks")` resolves to `https://www.humanos.eco/api/tasks`.
 *
 * The `Authorization` header is passed per-call rather than via an interceptor
 * because two different bearers are in play: the **Firebase ID token** for the
 * one-off [exchangeToken] call, and the **HumanOS bridge JWT** (returned by
 * that exchange) for every other endpoint. Callers must pass the full header
 * value including the `"Bearer "` prefix.
 *
 * Contract source: `docs/03_INTEGRATIONS/MOBILE_AUTH_ENDPOINT_SPEC.md`.
 */
interface HumanosApiService {

    /**
     * Exchange a Firebase ID token for a HumanOS bridge session.
     *
     * @param firebaseBearer `"Bearer <firebase-id-token>"`.
     */
    @POST("auth/mobile/exchange")
    suspend fun exchangeToken(
        @Header("Authorization") firebaseBearer: String,
    ): MobileExchangeResponse

    /**
     * List tasks, optionally filtered by status.
     *
     * @param bridgeBearer `"Bearer <bridge-jwt>"`.
     * @param status server status filter (e.g. "PENDING"), or null for all.
     */
    @GET("tasks")
    suspend fun getTasks(
        @Header("Authorization") bridgeBearer: String,
        @Query("status") status: String? = null,
    ): List<RemoteTaskDto>

    /** Create a task and return the server-assigned entity. */
    @POST("tasks")
    suspend fun createTask(
        @Header("Authorization") bridgeBearer: String,
        @Body body: CreateTaskDto,
    ): RemoteTaskDto

    /** Create a note / quick capture. */
    @POST("notes")
    suspend fun createNote(
        @Header("Authorization") bridgeBearer: String,
        @Body body: CreateNoteDto,
    ): RemoteNoteDto

    /** Fetch today's aggregated context snapshot (daily summary). */
    @GET("memory/snapshot/today")
    suspend fun getTodaySnapshot(
        @Header("Authorization") bridgeBearer: String,
    ): DailySnapshotDto

    /** Record a wellbeing / health check-in. */
    @POST("check-ins")
    suspend fun createCheckIn(
        @Header("Authorization") bridgeBearer: String,
        @Body body: CreateCheckInDto,
    ): CheckInDto
}
