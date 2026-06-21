package eco.humanos.android.integrations.humanos.dto

import kotlinx.serialization.Serializable

/**
 * One message in the mobile ↔ Claude bridge thread, as returned by
 * `GET /api/mobile/message` (humanos-eco `app/api/mobile/message/route.ts`,
 * ADR-0006 / TASK-027). The backend reuses the `ConversationMessage` row, so:
 *
 *  - `role`      ∈ "user" (Felipe, from the app) | "assistant" (the Claude agent)
 *  - `content`   the message text (server clips to 8000 chars)
 *  - `createdAt` ISO-8601 **string** (Prisma `DateTime`), not epoch millis
 *
 * Only the fields the poller needs are modelled; the lenient JSON config
 * (`ignoreUnknownKeys = true`) drops everything else (`sessionId`, `metadata`…).
 */
@Serializable
data class MessageDto(
    val id: String,
    val role: String = "user",
    val content: String = "",
    val createdAt: String? = null,
)

/** Envelope for `GET /api/mobile/message` → `{ "messages": [...] }`. */
@Serializable
data class MessagesEnvelope(val messages: List<MessageDto> = emptyList())
