package eco.humanos.android.data.auth

import eco.humanos.android.core.model.auth.AuthState
import eco.humanos.android.core.model.auth.HumanOSSession
import kotlinx.coroutines.flow.Flow

/**
 * Manages authentication state, token lifecycle, and session persistence.
 *
 * Implementations handle the dual-token model: Firebase ID token for
 * identity verification, plus HumanOS bearer token for API access.
 * The repository emits [AuthState] changes as a cold [Flow] so the
 * UI layer can react to login, logout, and token-expiry events.
 */
interface AuthRepository {

    /** Observe the current authentication state as a cold flow. */
    fun observeAuthState(): Flow<AuthState>

    /** Exchange a Google ID token for a Firebase session and return the authenticated state. */
    suspend fun signInWithGoogle(idToken: String): Result<AuthState.Authenticated>

    /** Silently refresh the HumanOS bearer token using the current Firebase session. */
    suspend fun refreshHumanosToken(): Result<HumanOSSession>

    /** Sign out of both Firebase and HumanOS, clearing all persisted tokens. */
    suspend fun signOut()

    /** Retrieve the current Firebase ID token, or null if not authenticated. */
    suspend fun getFirebaseToken(): String?

    /** Retrieve the current HumanOS bearer token, or null if not exchanged yet. */
    suspend fun getHumanosToken(): String?
}
