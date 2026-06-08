package eco.humanos.android.core.security.vault

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pure-JVM, in-memory [HumanOSSessionStore] used to exercise the store
 * **contract** without the real `EncryptedSharedPreferences` (which requires an
 * Android runtime / Keystore and cannot run in a plain unit test).
 *
 * It mirrors the semantics that [EncryptedHumanOSSessionStore] must uphold:
 * expiry-aware [get], state-reflecting [observeIsAuthenticated], and a
 * fully-clearing [clear]. Expiry is evaluated against an injectable [clock] so
 * tests can advance time deterministically instead of sleeping.
 *
 * This is a test double only; it lives under `src/test` and is never shipped.
 */
class FakeHumanOSSessionStore(
    private val clock: () -> Long = { System.currentTimeMillis() },
) : HumanOSSessionStore {

    private var stored: HumanOSSession? = null
    private val authState = MutableStateFlow(false)

    /** Number of times [clear] has been invoked (handy for assertions). */
    var clearCount: Int = 0
        private set

    override suspend fun save(session: HumanOSSession) {
        stored = session
        authState.value = session.isValidAt(clock())
    }

    override suspend fun get(): HumanOSSession? {
        val current = stored ?: run {
            authState.value = false
            return null
        }
        if (!current.isValidAt(clock())) {
            // Expired behaves exactly like absent, and is purged.
            stored = null
            authState.value = false
            return null
        }
        authState.value = true
        return current
    }

    override suspend fun clear() {
        stored = null
        clearCount++
        authState.value = false
    }

    override fun observeIsAuthenticated(): Flow<Boolean> = authState.asStateFlow()
}
