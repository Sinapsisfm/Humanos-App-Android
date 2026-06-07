package eco.humanos.android.core.model.capture

import kotlinx.serialization.Serializable

/**
 * Pipeline state of a captured item as it moves through AI processing.
 *
 * - [PENDING] — queued, not yet picked up.
 * - [PROCESSING] — AI enrichment in progress (transcription, tagging, linking).
 * - [DONE] — processing completed successfully.
 * - [FAILED] — processing failed, may be retried.
 */
@Serializable
enum class ProcessingStatus {
    PENDING,
    PROCESSING,
    DONE,
    FAILED,
}
