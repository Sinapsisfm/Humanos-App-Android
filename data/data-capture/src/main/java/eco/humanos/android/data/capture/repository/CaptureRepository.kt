package eco.humanos.android.data.capture.repository

import eco.humanos.android.core.model.capture.CaptureItem
import eco.humanos.android.core.model.common.PrivacyLevel
import kotlinx.coroutines.flow.Flow

/**
 * Domain-facing contract for capturing and observing [CaptureItem]s.
 *
 * Bridges the Capture UI layer to the Room-backed local store. Every
 * write also emits a [eco.humanos.android.core.model.common.TraceEvent]
 * through the observability layer so the action lands in the audit trail.
 */
interface CaptureRepository {

    /** Observe all stored captures as a cold flow, newest first. */
    fun observeCaptures(): Flow<List<CaptureItem>>

    /**
     * Persist a new text capture and return its generated id.
     *
     * Creates a [CaptureItem] of type TEXT from the local source with a
     * completed processing status, stores it, and records a trace event.
     */
    suspend fun saveTextCapture(
        text: String,
        privacyLevel: PrivacyLevel = PrivacyLevel.PRIVATE,
    ): String

    /** Delete the capture with the given [id], if it exists. */
    suspend fun deleteCapture(id: String)
}
