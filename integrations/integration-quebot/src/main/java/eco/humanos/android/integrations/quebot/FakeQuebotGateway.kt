package eco.humanos.android.integrations.quebot

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * In-memory fake of [QuebotGateway] for development, previews, and tests.
 *
 * Emits SSE events with realistic delays to simulate the streaming
 * experience of the real QueBot pipeline. No actual HTTP calls are made.
 */
class FakeQuebotGateway @Inject constructor() : QuebotGateway {

    override fun sendMessage(message: String, caseId: String?): Flow<SseEvent> = flow {
        // Simulate initial processing delay
        delay(INITIAL_DELAY_MS)

        // Emit status event
        emit(SseEvent.Status(mode = "direct", confidence = 0.92))
        delay(CHUNK_DELAY_MS)

        // Simulate search if the message contains a question-like pattern
        if (message.contains("?") || message.startsWith("busca", ignoreCase = true)) {
            emit(SseEvent.SearchExecuted(query = message.take(50), resultCount = 3))
            delay(SEARCH_DELAY_MS)
        }

        // Emit response as streaming deltas
        val responseChunks = buildFakeResponse(message)
        val fullResponse = StringBuilder()

        for (chunk in responseChunks) {
            emit(SseEvent.Delta(text = chunk))
            fullResponse.append(chunk)
            delay(CHUNK_DELAY_MS)
        }

        // Emit completion event
        emit(
            SseEvent.Done(
                fullResponse = fullResponse.toString(),
                tokensUsed = fullResponse.length / 4, // rough approximation
            ),
        )
    }

    override suspend fun checkStatus(): Result<ServiceStatus> {
        delay(INITIAL_DELAY_MS)
        return Result.success(
            ServiceStatus(
                healthy = true,
                version = "2.1.0-fake",
            ),
        )
    }

    private fun buildFakeResponse(message: String): List<String> {
        val base = "Soy QueBot, tu asistente de HumanOS. "
        val body = when {
            message.contains("tarea", ignoreCase = true) ->
                "Veo que tienes 3 tareas pendientes para hoy. " +
                    "La mas urgente es la presentacion para directorio que vence en 3 dias. " +
                    "Quieres que te ayude a priorizarlas?"

            message.contains("reunion", ignoreCase = true) ->
                "No tienes reuniones programadas para hoy. " +
                    "Tu proxima reunion es manana a las 10:00 con el equipo de Talca."

            message.contains("salud", ignoreCase = true) ->
                "Tu ultimo registro de energia fue hace 2 dias. " +
                    "Te recomiendo registrar como te sientes hoy para mantener el seguimiento."

            else ->
                "En que puedo ayudarte? Puedo revisar tus tareas, " +
                    "buscar informacion en tus documentos, o ayudarte a organizar tu dia."
        }

        // Split into chunks to simulate streaming
        val full = base + body
        return full.chunked(CHUNK_SIZE)
    }

    private companion object {
        const val INITIAL_DELAY_MS = 200L
        const val CHUNK_DELAY_MS = 50L
        const val SEARCH_DELAY_MS = 400L
        const val CHUNK_SIZE = 15
    }
}
