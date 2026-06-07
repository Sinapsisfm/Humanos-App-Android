package eco.humanos.android.core.model.capture

import kotlinx.serialization.Serializable

/**
 * The medium through which a piece of information was captured.
 */
@Serializable
enum class CaptureType {
    TEXT,
    VOICE,
    PHOTO,
    FILE,
    SCREENSHOT,
}
