package eco.humanos.android.core.model.capture

import eco.humanos.android.core.model.common.IntegrationSource
import eco.humanos.android.core.model.common.PrivacyLevel
import kotlinx.serialization.Serializable

/**
 * A raw piece of information captured by the user — text note, voice memo,
 * photo, file, or screenshot.
 *
 * Captures are the primary input surface of HumanOS. Once captured, the AI
 * pipeline processes them (transcription, entity extraction, context linking)
 * and optionally promotes them into tasks or context nodes.
 *
 * @property filePath Local file path for binary captures (voice, photo, file).
 * @property mimeType MIME type of the attached file, if any.
 * @property linkedContextNodeId Optional link to the context node this capture was promoted into.
 * @property createdAt Epoch millis when the capture was created.
 * @property syncedAt Epoch millis of last successful sync, null if never synced.
 */
@Serializable
data class CaptureItem(
    val id: String,
    val type: CaptureType,
    val title: String? = null,
    val textContent: String? = null,
    val filePath: String? = null,
    val mimeType: String? = null,
    val fileSizeBytes: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Float? = null,
    val source: IntegrationSource,
    val privacyLevel: PrivacyLevel,
    val processingStatus: ProcessingStatus,
    val linkedContextNodeId: String? = null,
    val createdAt: Long,
    val syncedAt: Long? = null,
)
