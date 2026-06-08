package eco.humanos.android.data.auth

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import eco.humanos.android.core.model.auth.AuthState
import eco.humanos.android.core.model.auth.HumanOSSession
import eco.humanos.android.core.model.common.IntegrationConfig
import eco.humanos.android.integrations.humanos.HumanosApiService
import eco.humanos.android.integrations.humanos.dto.toHumanOSSession
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase-backed implementation of [AuthRepository].
 *
 * Identity is provided by Firebase Authentication via the Google Sign-In
 * provider. The Google ID token (obtained by the UI layer through
 * [GoogleCredentialManager]) is exchanged for a Firebase credential here.
 *
 * ## HumanOS bridge token
 * Whether this repository obtains a real HumanOS bridge token is gated by
 * [IntegrationConfig.USE_REAL_HUMANOS_AUTH]:
 *
 * - **`false` (default)** — legacy mock behaviour. The Firebase half is fully
 *   real, but `humanosToken` is always `null` and [getHumanosToken] /
 *   [refreshHumanosToken] do not hit the network. Nothing changes versus the
 *   pre-bridge app.
 * - **`true`** — after Firebase sign-in, the Firebase ID token is exchanged for
 *   a HumanOS bridge JWT via `POST /api/auth/mobile/exchange`
 *   ([HumanosApiService.exchangeToken]). The resulting session is cached and
 *   surfaced through [AuthState.Authenticated.humanosToken]. Every exchange is
 *   guarded by try/catch so a bridge outage degrades to the mock state rather
 *   than failing the (otherwise successful) Firebase sign-in.
 *
 * See `docs/03_INTEGRATIONS/MOBILE_AUTH_ENDPOINT_SPEC.md` and TASK-010.
 */
@Singleton
class FirebaseAuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val humanosApiService: HumanosApiService,
) : AuthRepository {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    /** Last successful bridge session, cached so [getHumanosToken] is cheap. */
    private val cachedSession = AtomicReference<HumanOSSession?>(null)

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
                cachedSession.set(null)
                trySend(AuthState.Unauthenticated)
            } else {
                // Reuse a cached ID token here to keep the listener non-suspending;
                // a forced refresh is available via getFirebaseToken(). The
                // bridge token (if any) comes from the cached session.
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

            // Behind the flag: trade the Firebase token for a HumanOS bridge
            // session. Best-effort — a failure here must not fail the sign-in.
            val session = exchangeIfEnabled(firebaseToken)
            user.toAuthenticatedState(firebaseToken = firebaseToken, session = session)
        }

    /**
     * Refresh the HumanOS bridge token using the current Firebase session.
     *
     * Returns [NotImplementedError] when the bridge is disabled (default), so
     * existing callers see no behavioural change. When enabled, performs a
     * fresh exchange and updates the cache.
     */
    override suspend fun refreshHumanosToken(): Result<HumanOSSession> {
        if (!IntegrationConfig.USE_REAL_HUMANOS_AUTH) {
            return Result.failure(
                NotImplementedError(
                    "HumanOS bridge disabled (IntegrationConfig.USE_REAL_HUMANOS_AUTH=false).",
                ),
            )
        }
        val firebaseToken = getFirebaseToken()
            ?: return Result.failure(IllegalStateException("Not signed in to Firebase."))
        return runCatching {
            val session = performExchange(firebaseToken)
            cachedSession.set(session)
            session
        }
    }

    override suspend fun signOut() {
        cachedSession.set(null)
        firebaseAuth.signOut()
    }

    override suspend fun getFirebaseToken(): String? =
        firebaseAuth.currentUser?.getIdToken(false)?.await()?.token

    /**
     * The current HumanOS bridge token.
     *
     * - Flag off (default): always `null` (unchanged mock behaviour).
     * - Flag on: returns the cached bridge token, performing a fresh exchange
     *   if none is cached yet. Network failures degrade to `null`.
     */
    override suspend fun getHumanosToken(): String? {
        if (!IntegrationConfig.USE_REAL_HUMANOS_AUTH) return null
        cachedSession.get()?.let { return it.bearerToken }
        val firebaseToken = getFirebaseToken() ?: return null
        return exchangeIfEnabled(firebaseToken)?.bearerToken
    }

    /**
     * Run the bridge exchange when enabled, swallowing failures (returns null)
     * so the caller's primary flow is never broken by a bridge outage.
     * Updates [cachedSession] on success.
     */
    private suspend fun exchangeIfEnabled(firebaseToken: String): HumanOSSession? {
        if (!IntegrationConfig.USE_REAL_HUMANOS_AUTH) return null
        if (firebaseToken.isBlank()) return null
        return try {
            performExchange(firebaseToken).also { cachedSession.set(it) }
        } catch (e: Exception) {
            // Bridge unreachable / not provisioned / etc. Degrade to mock state.
            null
        }
    }

    /** Perform the raw exchange call and map the response to a session. */
    private suspend fun performExchange(firebaseToken: String): HumanOSSession =
        humanosApiService
            .exchangeToken("Bearer $firebaseToken")
            .toHumanOSSession(nowMillis = System.currentTimeMillis())

    private fun FirebaseUser.toAuthenticatedState(
        firebaseToken: String,
        session: HumanOSSession? = cachedSession.get(),
    ): AuthState.Authenticated =
        AuthState.Authenticated(
            userId = uid,
            // personId comes from the HumanOS bridge; null until exchanged.
            personId = session?.personId,
            email = email,
            displayName = displayName,
            firebaseToken = firebaseToken,
            humanosToken = session?.bearerToken,
            humanosTokenExpiresAt = session?.expiresAt,
        )
}
