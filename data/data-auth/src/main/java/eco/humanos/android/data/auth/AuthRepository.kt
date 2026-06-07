package eco.humanos.android.data.auth

/**
 * Auth repository — manages authentication state, token refresh, session lifecycle.
 * Full implementation in Tanda 5.
 */
interface AuthRepository {
    suspend fun isAuthenticated(): Boolean
    suspend fun signOut()
}
