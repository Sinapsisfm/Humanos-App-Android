package eco.humanos.android.core.model.terrain

import eco.humanos.android.core.model.capture.CaptureType
import kotlinx.serialization.Serializable

/**
 * A piece of evidence attached to a [FieldInspection] — photo, voice note,
 * document, or text observation.
 *
 * Evidence items inherit the capture type taxonomy but are always bound
 * to a parent inspection, unlike standalone [CaptureItem] entries.
 *
 * @property inspectionId The parent [FieldInspection.id] this evidence belongs to.
 * @property filePath Local file path for binary evidence.
 * @property tags Free-form labels for categorizing evidence.
 * @property createdAt Epoch millis when the evidence was captured.
 */
@Serializable
data class EvidenceItem(
    val id: String,
    val inspectionId: String,
    val type: CaptureType,
    val filePath: String? = null,
    val description: String? = null,
    val location: GeoPoint? = null,
    val tags: List<String> = emptyList(),
    val createdAt: Long,
)
