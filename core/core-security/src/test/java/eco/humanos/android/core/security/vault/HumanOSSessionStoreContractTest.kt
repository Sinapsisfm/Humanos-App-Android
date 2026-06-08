package eco.humanos.android.core.security.vault

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Contract tests for [HumanOSSessionStore], driven through the pure-JVM
 * [FakeHumanOSSessionStore]. These assert the behaviour every implementation
 * (including the Keystore-backed [EncryptedHumanOSSessionStore]) must satisfy:
 *
 * - `save` then `get` returns the same session.
 * - an expired session is indistinguishable from "no session" (`get` -> null).
 * - `clear` wipes the session (`get` -> null).
 * - `observeIsAuthenticated` reflects save / expiry / clear transitions.
 *
 * The real `EncryptedSharedPreferences` implementation needs an Android runtime
 * (Keystore) and is therefore exercised at runtime / instrumentation rather
 * than here; this contract pins the semantics the impl is written against.
 */
class HumanOSSessionStoreContractTest {

    private fun session(
        bridgeToken: String = "bridge-jwt-xyz",
        expiresAtEpochMs: Long,
    ) = HumanOSSession(
        bridgeToken = bridgeToken,
        userId = "uid-123",
        personId = "person-456",
        email = "felipe@humanos.eco",
        displayName = "Felipe",
        expiresAtEpochMs = expiresAtEpochMs,
    )

    @Test
    fun `save then get returns the stored session`() = runTest {
        val store = FakeHumanOSSessionStore(clock = { 1_000L })
        val saved = session(expiresAtEpochMs = 10_000L)

        store.save(saved)

        assertThat(store.get()).isEqualTo(saved)
    }

    @Test
    fun `get returns null when nothing was ever saved`() = runTest {
        val store = FakeHumanOSSessionStore(clock = { 1_000L })

        assertThat(store.get()).isNull()
    }

    @Test
    fun `get returns null for an expired session`() = runTest {
        var now = 1_000L
        val store = FakeHumanOSSessionStore(clock = { now })
        // Valid for a while, then time advances past expiry.
        store.save(session(expiresAtEpochMs = 5_000L))
        assertThat(store.get()).isNotNull()

        now = 5_001L // strictly past expiresAtEpochMs

        assertThat(store.get()).isNull()
    }

    @Test
    fun `get returns null for a session expiring exactly now`() = runTest {
        // expiresAtEpochMs is a hard boundary: valid requires expiresAt > now.
        val store = FakeHumanOSSessionStore(clock = { 5_000L })
        store.save(session(expiresAtEpochMs = 5_000L))

        assertThat(store.get()).isNull()
    }

    @Test
    fun `clear removes the session`() = runTest {
        val store = FakeHumanOSSessionStore(clock = { 1_000L })
        store.save(session(expiresAtEpochMs = 10_000L))
        assertThat(store.get()).isNotNull()

        store.clear()

        assertThat(store.get()).isNull()
        assertThat(store.clearCount).isEqualTo(1)
    }

    @Test
    fun `observeIsAuthenticated reflects save then clear`() = runTest {
        val store = FakeHumanOSSessionStore(clock = { 1_000L })

        store.observeIsAuthenticated().test {
            // Initial state: no session.
            assertThat(awaitItem()).isFalse()

            store.save(session(expiresAtEpochMs = 10_000L))
            assertThat(awaitItem()).isTrue()

            store.clear()
            assertThat(awaitItem()).isFalse()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeIsAuthenticated emits false after a saved session expires and is read`() = runTest {
        var now = 1_000L
        val store = FakeHumanOSSessionStore(clock = { now })
        store.save(session(expiresAtEpochMs = 5_000L))

        // While valid, the flow reports authenticated.
        assertThat(store.observeIsAuthenticated().first()).isTrue()

        // Advance past expiry and force an expiry check via get().
        now = 5_001L
        assertThat(store.get()).isNull()

        // The expiry purge flips the observed state to false.
        assertThat(store.observeIsAuthenticated().first()).isFalse()
    }
}
