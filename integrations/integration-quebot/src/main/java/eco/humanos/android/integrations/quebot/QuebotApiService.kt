package eco.humanos.android.integrations.quebot

/**
 * Retrofit + OkHttp SSE interface for low-level HTTP calls to the QueBot pipeline.
 *
 * Higher-level business logic lives in [QuebotGateway]; this interface
 * is the raw Retrofit contract used by the gateway implementation.
 * Endpoints will be annotated when the real gateway is wired.
 */
interface QuebotApiService {
    // @POST("api/chat")
    // @Streaming
    // suspend fun sendMessage(@Body body: ChatRequest): ResponseBody

    // @GET("api/health")
    // suspend fun healthCheck(): Response<ServiceStatus>
}
