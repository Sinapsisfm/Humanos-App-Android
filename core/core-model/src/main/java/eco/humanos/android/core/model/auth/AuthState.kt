package eco.humanos.android.core.model.auth

/**
 * Sealed hierarchy representing the authentication state of the current user.
 *
 * Observed as a StateFlow by the UI layer to drive navigation guards
 * and conditional feature access. Not annotated with @Serializable
 * because it is never persisted or sent over the wire -- it is a
 * pure in-memory state machine.
 *
 * - [Unauthenticated] — no active session; show login screen.
 * - [Authenticated] — valid session with Firebase + optional HumanOS tokens.
 * - [TokenExpired] — session was valid but the HumanOS token has expired; trigger silent refresh.
 * - [Loading] — authentication state is being resolved (splash / token refresh in flight).
 */
sealed interface AuthState {

    data object Unauthenticated : AuthState

    data class Authenticated(
        val userId: String,
        val personId: String?,
        val email: String?,
        val displayName: String?,
        val firebaseToken: String,
        val humanosToken: String?,
        val humanosTokenExpiresAt: Long?,
    ) : AuthState

    data object TokenExpired : AuthState

    data object Loading : AuthState
}
