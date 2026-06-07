package eco.humanos.android.core.model.common

import kotlinx.serialization.Serializable

/**
 * Tracks the synchronization state of a local entity relative to the remote backend.
 *
 * Used by the sync engine to determine which entities need uploading,
 * downloading, or conflict resolution.
 */
@Serializable
enum class SyncStatus {
    SYNCED,
    PENDING_UPLOAD,
    PENDING_DOWNLOAD,
    CONFLICT,
    ERROR,
}
