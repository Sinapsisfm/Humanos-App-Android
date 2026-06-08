package eco.humanos.android.integrations.humanos

import eco.humanos.android.integrations.humanos.dto.CheckInEnvelope
import eco.humanos.android.integrations.humanos.dto.CheckInsEnvelope
import eco.humanos.android.integrations.humanos.dto.CreateCheckInDto
import eco.humanos.android.integrations.humanos.dto.CreateTaskDto
import eco.humanos.android.integrations.humanos.dto.MobileExchangeResponse
import eco.humanos.android.integrations.humanos.dto.PersonEnvelope
import eco.humanos.android.integrations.humanos.dto.SnapshotEnvelope
import eco.humanos.android.integrations.humanos.dto.TaskEnvelope
import eco.humanos.android.integrations.humanos.dto.TasksEnvelope
import eco.humanos.android.integrations.humanos.dto.UpdateTaskDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit contract for the HumanOS mobile API (base URL
 * [eco.humanos.android.core.model.common.IntegrationConfig.HUMANOS_BASE_URL],
 * which ends in `/api/`).
 *
 * All data endpoints live under `mobile/` and authenticate with the **bridge
 * JWT** (minted by [exchangeToken]) — they are the Bearer-token analogues of
 * the web's session-cookie routes (see humanos-eco `app/api/mobile`). The
 * `Authorization` header is passed per-call: the **Firebase ID token** for the
 * one-off [exchangeToken], the **bridge JWT** for everything else. Callers pass
 * the full header value including `"Bearer "`.
 */
interface HumanosApiService {

    /** Exchange a Firebase ID token for a HumanOS bridge session. */
    @POST("auth/mobile/exchange")
    suspend fun exchangeToken(
        @Header("Authorization") firebaseBearer: String,
    ): MobileExchangeResponse

    /** List the signed-in person's tasks, optionally filtered by status. */
    @GET("mobile/tasks")
    suspend fun getTasks(
        @Header("Authorization") bridgeBearer: String,
        @Query("status") status: String? = null,
    ): TasksEnvelope

    /** Create a task; returns the server-assigned entity. */
    @POST("mobile/tasks")
    suspend fun createTask(
        @Header("Authorization") bridgeBearer: String,
        @Body body: CreateTaskDto,
    ): TaskEnvelope

    /** Partially update a task (e.g. mark done); ownership enforced server-side. */
    @PATCH("mobile/tasks/{id}")
    suspend fun updateTask(
        @Header("Authorization") bridgeBearer: String,
        @Path("id") id: String,
        @Body body: UpdateTaskDto,
    ): TaskEnvelope

    /** Aggregated "today" snapshot for the dashboard. */
    @GET("mobile/snapshot")
    suspend fun getSnapshot(
        @Header("Authorization") bridgeBearer: String,
    ): SnapshotEnvelope

    /** Recent check-ins + today's, for the wellbeing card. */
    @GET("mobile/check-ins")
    suspend fun getCheckIns(
        @Header("Authorization") bridgeBearer: String,
    ): CheckInsEnvelope

    /** Record (upsert) today's mood / energy / stress check-in. */
    @POST("mobile/check-ins")
    suspend fun createCheckIn(
        @Header("Authorization") bridgeBearer: String,
        @Body body: CreateCheckInDto,
    ): CheckInEnvelope

    /** The signed-in user's profile. */
    @GET("mobile/person")
    suspend fun getPerson(
        @Header("Authorization") bridgeBearer: String,
    ): PersonEnvelope
}
