package eco.humanos.android.data.auth

import eco.humanos.android.core.security.vault.HumanOSSessionStore
import eco.humanos.android.integrations.humanos.HumanosTokenProvider
import javax.inject.Inject

/**
 * Real [HumanosTokenProvider] backed by the Secure Vault.
 *
 * Supplies the current HumanOS **bridge JWT** to network components (notably
 * `RealHumanosGateway`) that need to authenticate API calls. The token is read
 * on demand from the [HumanOSSessionStore], the single source of truth for the
 * bridge session — it is persisted there by `FirebaseAuthRepository` after the
 * Firebase -> HumanOS exchange.
 *
 * ## Contract
 * Returns the **raw** JWT (no `"Bearer "` prefix); callers add the prefix when
 * building the `Authorization` header (see `RealHumanosGateway.bearer`). This
 * matches the [HumanosTokenProvider] interface ("the current bridge JWT").
 *
 * ## Expiry & absence
 * [HumanOSSessionStore.get] already treats an expired session as absent (returns
 * `null`), so an expired bridge token surfaces here as `null` — the gateway then
 * fails the call cleanly (caught by its `runCatching`) rather than sending a
 * stale credential. No bridge token yet (exchange not done / bridge outage)
 * likewise yields `null`.
 *
 * This binding replaces the dormant `NullHumanosTokenProvider`; it is wired in
 * `data-auth`'s `AuthModule` so there is exactly one `HumanosTokenProvider` in
 * the graph.
 */
class VaultHumanosTokenProvider @Inject constructor(
    private val sessionStore: HumanOSSessionStore,
) : HumanosTokenProvider {

    override suspend fun currentBridgeToken(): String? =
        sessionStore.get()?.bridgeToken
}
