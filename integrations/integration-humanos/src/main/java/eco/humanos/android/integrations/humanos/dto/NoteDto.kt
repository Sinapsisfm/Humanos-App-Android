package eco.humanos.android.integrations.humanos.dto

import kotlinx.serialization.Serializable

/**
 * Request body for `POST /api/notes` — a quick capture / note creation.
 *
 * @property content Free-text body of the note (required).
 * @property tags Optional labels for filtering and routing on the server.
 * @property source Where the note originated, e.g. "android-quick-capture".
 */
@Serializable
data class CreateNoteDto(
    val content: String,
    val tags: List<String> = emptyList(),
    val source: String = "android",
)

/**
 * Server response for a created note.
 *
 * @property id Server-assigned note id.
 * @property content Stored content (echoed back).
 * @property createdAt Epoch millis the note was created on the server.
 */
@Serializable
data class RemoteNoteDto(
    val id: String,
    val content: String,
    val tags: List<String> = emptyList(),
    val createdAt: Long? = null,
)
