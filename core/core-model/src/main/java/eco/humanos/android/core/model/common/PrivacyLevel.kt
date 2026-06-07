package eco.humanos.android.core.model.common

import kotlinx.serialization.Serializable

/**
 * Classification of how sensitive a piece of data is.
 *
 * - [PUBLIC] — visible to collaborators and shared contexts.
 * - [PRIVATE] — visible only to the owning user.
 * - [VAULT] — encrypted at rest, requires explicit unlock to access.
 */
@Serializable
enum class PrivacyLevel {
    PUBLIC,
    PRIVATE,
    VAULT,
}
