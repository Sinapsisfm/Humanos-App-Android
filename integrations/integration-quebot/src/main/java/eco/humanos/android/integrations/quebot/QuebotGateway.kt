package eco.humanos.android.integrations.quebot

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * Gateway to the QueBot conversational AI pipeline.
 *
 * Messages are sent as a single request and the response streams back
 * as a [Flow] of [SseEvent] entries, mirroring the server-sent events
 * protocol used by the QueBot backend.
 */
interface QuebotGateway {

    /**
     * Send a user message and receive a stream of SSE events.
     *
     * @param message The user's input text.
     * @param caseId Optional case/conversation ID for context continuity.
     * @return A cold [Flow] of [SseEvent] entries, ending with [SseEvent.Done] or [SseEvent.Error].
     */
    fun sendMessage(message: String, caseId: String? = null): Flow<SseEvent>

    /** Check whether the QueBot service is reachable and healthy. */
    suspend fun checkStatus(): Result<ServiceStatus>
}

/**
 * Sealed hierarchy representing server-sent events from the QueBot pipeline.
 *
 * Events arrive in order: [Status] (optional) -> [SearchExecuted] (optional) ->
 * one or more [Delta] -> [Done]. An [Error] can arrive at any point.
 */
sealed class SseEvent {
    /** Pipeline status update (e.g. which processing mode was selected). */
    data class Status(val mode: String, val confidence: Double) : SseEvent()

    /** Incremental text chunk of the assistant response. */
    data class Delta(val text: String) : SseEvent()

    /** Notification that a search was executed as part of the response. */
    data class SearchExecuted(val query: String, val resultCount: Int) : SseEvent()

    /** Terminal event indicating the response is complete. */
    data class Done(val fullResponse: String, val tokensUsed: Int?) : SseEvent()

    /** Terminal error event. */
    data class Error(val message: String) : SseEvent()
}

/**
 * Health status of the QueBot service.
 */
@Serializable
data class ServiceStatus(
    val healthy: Boolean,
    val version: String?,
)
