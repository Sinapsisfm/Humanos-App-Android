package eco.humanos.android.core.model.auth

import kotlinx.serialization.Serializable

/**
 * A serializable snapshot of the active HumanOS backend session.
 *
 * Stored in encrypted shared preferences and used to bootstrap
 * authenticated API calls on cold start without hitting Firebase
 * every time. Replaced when a token refresh succeeds.
 *
 * @property userId Firebase UID of the authenticated user.
 * @property personId HumanOS Person ID, null if the user hasn't been linked yet.
 * @property bearerToken JWT bearer token for the HumanOS API.
 * @property expiresAt Epoch millis when [bearerToken] expires.
 */
@Serializable
data class HumanOSSession(
    val userId: String,
    val personId: String?,
    val bearerToken: String,
    val expiresAt: Long,
)
