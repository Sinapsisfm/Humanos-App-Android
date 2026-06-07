package eco.humanos.android.integrations.humanos

/**
 * Retrofit interface for low-level HTTP calls to the HumanOS API.
 *
 * Higher-level business logic lives in [HumanosGateway]; this interface
 * is the raw Retrofit contract used by the gateway implementation.
 * Endpoints will be annotated with @GET/@POST when the real gateway
 * implementation is wired.
 */
interface HumanosApiService {
    // @POST("api/auth/firebase-exchange")
    // suspend fun exchangeToken(@Body body: TokenExchangeRequest): Response<HumanOSSession>

    // @GET("api/tasks")
    // suspend fun getTasks(@Query("status") status: String?): Response<List<TaskItem>>

    // @POST("api/tasks")
    // suspend fun createTask(@Body body: CreateTaskRequest): Response<TaskItem>

    // @GET("api/daily-review")
    // suspend fun getDailyReview(): Response<DailyReviewDto>

    // @GET("api/health")
    // suspend fun healthCheck(): Response<Unit>
}
