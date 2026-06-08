package eco.humanos.android.data.auth

import com.google.common.truth.Truth.assertThat
import eco.humanos.android.core.security.vault.HumanOSSession
import eco.humanos.android.core.security.vault.HumanOSSessionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Pure-JVM unit tests for [VaultHumanosTokenProvider] using a hand-written,
 * in-memory [HumanOSSessionStore] fake (no Keystore / Android runtime needed).
 *
 * Verifies the [eco.humanos.android.integrations.humanos.HumanosTokenProvider]
 * contract this class fulfils for `RealHumanosGateway`:
 * - returns the **raw** bridge JWT (no `"Bearer "` prefix — the gateway adds it),
 * - returns `null` when the vault is empty,
 * - returns `null` when the vault treats the session as expired/absent.
 */
class VaultHumanosTokenProviderTest {

    /**
     * Minimal in-memory vault. [get] honours the store contract: an expired
     * session is indistinguishable from no session (both return `null`).
     */
    private class FakeSessionStore(
        private val now: () -> Long = { System.currentTimeMillis() },
    ) : HumanOSSessionStore {
        private var stored: HumanOSSession? = null
        private val authState = MutableStateFlow(false)

        override suspend fun save(session: HumanOSSession) {
            stored = session
            authState.value = session.isValidAt(now())
        }

        override suspend fun get(): HumanOSSession? {
            val current = stored ?: return null
            if (!current.isValidAt(now())) {
                stored = null
                authState.value = false
                return null
            }
            return current
        }

        override suspend fun clear() {
            stored = null
            authState.value = false
        }

        override fun observeIsAuthenticated(): Flow<Boolean> = authState.asStateFlow()
    }

    private fun session(token: String, expiresAtEpochMs: Long) = HumanOSSession(
        bridgeToken = token,
        userId = "uid-1",
        personId = "person-1",
        expiresAtEpochMs = expiresAtEpochMs,
    )

    @Test
    fun `returns raw bridge token when a valid session is stored`() = runTest {
        val store = FakeSessionStore(now = { 1_000L })
        store.save(session(token = "bridge-jwt-abc", expiresAtEpochMs = 10_000L))
        val provider = VaultHumanosTokenProvider(store)

        val token = provider.currentBridgeToken()

        // Raw JWT, NOT prefixed — RealHumanosGateway.bearer() adds "Bearer ".
        assertThat(token).isEqualTo("bridge-jwt-abc")
        assertThat(token).doesNotContain("Bearer")
    }

    @Test
    fun `returns null when the vault is empty`() = runTest {
        val provider = VaultHumanosTokenProvider(FakeSessionStore())

        assertThat(provider.currentBridgeToken()).isNull()
    }

    @Test
    fun `returns null when the stored session has expired`() = runTest {
        // now (5_000) is past the session's expiry (1_000): vault reports absent.
        val store = FakeSessionStore(now = { 5_000L })
        store.save(session(token = "stale-jwt", expiresAtEpochMs = 1_000L))
        val provider = VaultHumanosTokenProvider(store)

        assertThat(provider.currentBridgeToken()).isNull()
    }
}
