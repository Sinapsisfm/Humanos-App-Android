package eco.humanos.android.data.auth

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import eco.humanos.android.core.model.auth.AuthState
import eco.humanos.android.core.model.auth.HumanOSSession
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase-backed implementation of [AuthRepository].
 *
 * Identity is provided by Firebase Authentication via the Google Sign-In
 * provider. The Google ID token (obtained by the UI layer through
 * [GoogleCredentialManager]) is exchanged for a Firebase credential here.
 *
 * The HumanOS bridge token is intentionally MOCK for now: the
 * `POST /api/auth/mobile/exchange` endpoint does not yet exist in HumanOS,
 * so every [AuthState.Authenticated] is emitted with `humanosToken = null`.
 * The Firebase half is fully real. See TASK-010 for the bridge work.
 */
@Singleton
class FirebaseAuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : AuthRepository {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Emits the live authentication state, backed by a Firebase
     * [FirebaseAuth.AuthStateListener]. Emits [AuthState.Loading] first,
     * then the current state, and re-emits on every sign-in / sign-out.
     */
    override fun observeAuthState(): Flow<AuthState> = callbackFlow {
        trySend(AuthState.Loading)

        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            if (user == null) {
                trySend(AuthState.Unauthenticated)
            } else {
                // Reuse a cached ID token here to keep the listener non-suspending;
                // a forced refresh is available via getFirebaseToken().
                trySend(user.toAuthenticatedState(firebaseToken = ""))
            }
        }

        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<AuthState.Authenticated> =
        runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user
                ?: error("Firebase sign-in succeeded but returned no user.")
            val firebaseToken = user.getIdToken(false).await().token.orEmpty()
            user.toAuthenticatedState(firebaseToken = firebaseToken)
        }

    /**
     * Bridge to HumanOS is not implemented yet (endpoint missing).
     * TODO(TASK-010): exchange the Firebase ID token for a HumanOS bearer
     * token via POST /api/auth/mobile/exchange and persist the session.
     */
    override suspend fun refreshHumanosToken(): Result<HumanOSSession> =
        Result.failure(
            NotImplementedError(
                "HumanOS bridge not implemented: POST /api/auth/mobile/exchange " +
                    "does not exist yet (see TASK-010).",
            ),
        )

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    override suspend fun getFirebaseToken(): String? =
        firebaseAuth.currentUser?.getIdToken(false)?.await()?.token

    /**
     * Always null until the HumanOS bridge exists.
     * TODO(TASK-010): return the persisted HumanOS bearer token once the
     * mobile exchange endpoint is live.
     */
    override suspend fun getHumanosToken(): String? = null

    private fun FirebaseUser.toAuthenticatedState(firebaseToken: String): AuthState.Authenticated =
        AuthState.Authenticated(
            userId = uid,
            // personId comes from the HumanOS bridge, which is mocked for now.
            personId = null,
            email = email,
            displayName = displayName,
            firebaseToken = firebaseToken,
            // Bridge mock — see TASK-010.
            humanosToken = null,
            humanosTokenExpiresAt = null,
        )
}
