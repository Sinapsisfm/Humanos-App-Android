package eco.humanos.android.core.model.common

import kotlinx.serialization.Serializable

/**
 * Identifies the system that originated or owns a piece of data.
 *
 * Used across every domain entity to track provenance and drive
 * sync logic between local, HumanOS backend, QueBot, Health Connect,
 * and Firebase.
 */
@Serializable
enum class IntegrationSource {
    HUMANOS,
    QUEBOT,
    LOCAL,
    HEALTH_CONNECT,
    FIREBASE,
}
