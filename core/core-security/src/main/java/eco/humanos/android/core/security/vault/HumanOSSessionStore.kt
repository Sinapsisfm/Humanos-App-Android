package eco.humanos.android.core.security.vault

import kotlinx.coroutines.flow.Flow

/**
 * The Secure Vault for the HumanOS backend session.
 *
 * Encapsulates **durable, encrypted** storage of the HumanOS bridge token and
 * its surrounding session metadata. The contract guarantees:
 *
 * - The session is persisted only in Keystore-backed encrypted storage. It is
 *   **never** written to Room, plain `SharedPreferences`, DataStore, or logs.
 * - [get] performs an expiry check: an expired session is indistinguishable
 *   from no session (both return `null`), so callers cannot accidentally use a
 *   stale bridge token.
 * - [clear] fully removes the persisted session (used on sign-out).
 *
 * Implementations must not log the [HumanOSSession.bridgeToken] value (or any
 * other token material) — only generic lifecycle events.
 *
 * See [EncryptedHumanOSSessionStore] for the production implementation and
 * `docs/01_ARCHITECTURE/SECURITY_PRIVACY.md` ("HumanOS Session Vault").
 */
interface HumanOSSessionStore {

    /** Persist [session], replacing any previously stored session. */
    suspend fun save(session: HumanOSSession)

    /**
     * The current session, or `null` if none is stored **or** the stored
     * session has expired ([HumanOSSession.expiresAtEpochMs] `< now`).
     */
    suspend fun get(): HumanOSSession?

    /** Remove the stored session, if any. Safe to call when empty. */
    suspend fun clear()

    /**
     * Emits whether a non-expired session currently exists. Emits the current
     * value on collection and re-emits on every [save] / [clear].
     */
    fun observeIsAuthenticated(): Flow<Boolean>
}
