package eco.humanos.android.core.model.common

import kotlinx.serialization.Serializable

/**
 * A pointer back to the remote system where an entity originated.
 *
 * Attached to any entity that was imported or synced from an external
 * source, enabling round-trip sync and deep-linking.
 */
@Serializable
data class SourceReference(
    val source: IntegrationSource,
    val remoteId: String? = null,
    val remoteUrl: String? = null,
    val lastSyncedAt: Long? = null,
    val syncStatus: SyncStatus,
)
