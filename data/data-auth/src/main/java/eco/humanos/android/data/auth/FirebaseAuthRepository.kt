package eco.humanos.android.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import eco.humanos.android.core.model.auth.AuthState
import eco.humanos.android.core.model.auth.HumanosLinkState
import eco.humanos.android.core.model.common.IntegrationConfig
import eco.humanos.android.core.security.vault.HumanOSSessionStore
import eco.humanos.android.integrations.humanos.HumanosApiService
import eco.humanos.android.integrations.humanos.toHumanosErrorMessage
import eco.humanos.android.integrations.humanos.dto.toHumanOSSession
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import eco.humanos.android.core.model.auth.HumanOSSession as ApiSession
import eco.humanos.android.core.security.vault.HumanOSSession as VaultSession

/**
 * Firebase-backed implementation of [AuthRepository].
 *
 * Identity is provided by Firebase Authentication via the Google Sign-In
 * provider. The Google ID token (obtained by the UI layer through
 * [GoogleCredentialManager]) is exchanged for a Firebase credential here.
 *
 * ## HumanOS bridge token — stored in the Secure Vault
 * The HumanOS bridge JWT is **never** held in Room, plain memory caches, or
 * logs. When obtained, it is persisted to the [HumanOSSessionStore] (Keystore-
 * backed `EncryptedSharedPreferences`), read back on demand, and wiped on
 * sign-out. See `docs/01_ARCHITECTURE/SECURITY_PRIVACY.md` ("HumanOS Session
 * Vault").
 *
 * Whether a bridge token is obtained at all is gated by
 * [IntegrationConfig.USE_REAL_HUMANOS_AUTH]:
 *
 * - **`false` (default)** — legacy mock behaviour. The Firebase half is fully
 *   real, but `humanosToken` is always `null`, [getHumanosToken] /
 *   [refreshHumanosToken] do not hit the network, and **nothing is written to
 *   the vault**. Nothing changes versus the pre-bridge app.
 * - **`true`** — after Firebase sign-in, the Firebase ID token is exchanged for
 *   a HumanOS bridge JWT via `POST /api/auth/mobile/exchange`
 *   ([HumanosApiService.exchangeToken]). The resulting session is saved to the
 *   vault and surfaced through [AuthState.Authenticated.humanosToken]. Every
 *   exchange is guarded by try/catch so a bridge outage degrades to the mock
 *   state rather than failing the (otherwise successful) Firebase sign-in.
 *
 * See `docs/03_INTEGRATIONS/MOBILE_AUTH_ENDPOINT_SPEC.md` and TASK-010.
 */
@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val humanosApiService: HumanosApiService,
    private val sessionStore: HumanOSSessionStore,
) : AuthRepository {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _humanosLinkState = MutableStateFlow<HumanosLinkState>(HumanosLinkState.Unknown)
    override val humanosLinkState: StateFlow<HumanosLinkState> = _humanosLinkState.asStateFlow()

    /**
     * Emits the live authentication state, backed by a Firebase
     * [FirebaseAuth.AuthStateListener]. Emits [AuthState.Loading] first,
     * then the current state, and re-emits on every sign-in / sign-out.
     *
     * The listener is non-suspending, so it surfaces only the Firebase identity
     * (the bridge token / personId are vault-resident and exposed via
     * [getHumanosToken] and the [signInWithGoogle] result).
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

            // Behind the flag: trade the Firebase token for a HumanOS bridge
            // session and persist it to the vault. Best-effort — a failure here
            // must not fail the sign-in.
            val session = exchangeAndStoreIfEnabled(user, firebaseToken)
            user.toAuthenticatedState(firebaseToken = firebaseToken, session = session)
        }

    /**
     * Refresh the HumanOS bridge token using the current Firebase session.
     *
     * Returns [NotImplementedError] when the bridge is disabled (default), so
     * existing callers see no behavioural change. When enabled, performs a
     * fresh exchange, persists it to the vault, and returns the API session.
     */
    override suspend fun refreshHumanosToken(): Result<ApiSession> {
        if (!IntegrationConfig.USE_REAL_HUMANOS_AUTH) {
            return Result.failure(
                NotImplementedError(
                    "HumanOS bridge disabled (IntegrationConfig.USE_REAL_HUMANOS_AUTH=false).",
                ),
            )
        }
        val user = firebaseAuth.currentUser
            ?: return Result.failure(IllegalStateException("Not signed in to Firebase."))
        val firebaseToken = user.getIdToken(false).await().token
            ?: return Result.failure(IllegalStateException("Not signed in to Firebase."))
        return runCatching {
            val apiSession = performExchange(firebaseToken)
            sessionStore.save(apiSession.toVaultSession(user))
            apiSession
        }.onSuccess {
            _humanosLinkState.value = HumanosLinkState.Linked
        }.onFailure {
            _humanosLinkState.value = HumanosLinkState.Failed(it.toHumanosErrorMessage())
        }
    }

    override suspend fun signOut() {
        // Wipe the encrypted bridge session before tearing down the Firebase
        // session, so no token survives sign-out.
        sessionStore.clear()
        firebaseAuth.signOut()
        _humanosLinkState.value = HumanosLinkState.Unknown
    }

    override suspend fun getFirebaseToken(): String? =
        firebaseAuth.currentUser?.getIdToken(false)?.await()?.token

    /**
     * The current HumanOS bridge token.
     *
     * - Flag off (default): always `null` (unchanged mock behaviour; vault is
     *   never read or written).
     * - Flag on: returns the vault-stored bridge token (expiry-checked by the
     *   vault). If none is stored yet, performs a fresh exchange, persists it,
     *   and returns it. Network failures degrade to `null`.
     */
    override suspend fun getHumanosToken(): String? {
        if (!IntegrationConfig.USE_REAL_HUMANOS_AUTH) return null
        sessionStore.get()?.let { return it.bridgeToken }
        val user = firebaseAuth.currentUser ?: return null
        val firebaseToken = user.getIdToken(false).await().token ?: return null
        return exchangeAndStoreIfEnabled(user, firebaseToken)?.bridgeToken
    }

    /**
     * Run the bridge exchange when enabled, persisting the result to the vault
     * and swallowing failures (returns null) so the caller's primary flow is
     * never broken by a bridge outage.
     */
    private suspend fun exchangeAndStoreIfEnabled(
        user: FirebaseUser,
        firebaseToken: String,
    ): VaultSession? {
        if (!IntegrationConfig.USE_REAL_HUMANOS_AUTH) return null
        if (firebaseToken.isBlank()) return null
        return try {
            val vaultSession = performExchange(firebaseToken).toVaultSession(user)
            sessionStore.save(vaultSession)
            _humanosLinkState.value = HumanosLinkState.Linked
            vaultSession
        } catch (e: Exception) {
            // Bridge unreachable / not provisioned / etc. Sign-in still succeeds,
            // but record WHY the exchange failed so the UI can show it instead of
            // a silent downstream "no bridge token".
            _humanosLinkState.value = HumanosLinkState.Failed(e.toHumanosErrorMessage())
            null
        }
    }

    /** Perform the raw exchange call and map the response to an API session. */
    private suspend fun performExchange(firebaseToken: String): ApiSession =
        humanosApiService
            .exchangeToken("Bearer $firebaseToken")
            .toHumanOSSession(nowMillis = System.currentTimeMillis())

    private fun FirebaseUser.toAuthenticatedState(
        firebaseToken: String,
        session: VaultSession? = null,
    ): AuthState.Authenticated =
        AuthState.Authenticated(
            userId = uid,
            // personId comes from the HumanOS bridge; null until exchanged.
            personId = session?.personId,
            email = email,
            displayName = displayName,
            firebaseToken = firebaseToken,
            humanosToken = session?.bridgeToken,
            humanosTokenExpiresAt = session?.expiresAtEpochMs,
        )

    /** Map the API session DTO to the vault's session shape, enriched with profile fields. */
    private fun ApiSession.toVaultSession(user: FirebaseUser): VaultSession =
        VaultSession(
            bridgeToken = bearerToken,
            userId = userId,
            personId = personId,
            email = user.email,
            displayName = user.displayName,
            expiresAtEpochMs = expiresAt,
        )
}
