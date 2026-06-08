package eco.humanos.android.integrations.humanos

import javax.inject.Inject

/**
 * Supplies the current HumanOS **bridge JWT** to network components that need
 * to send it as `Authorization: Bearer <token>`.
 *
 * This indirection breaks what would otherwise be a dependency cycle:
 * `data-auth` owns the token (it performs the exchange) but also needs
 * [HumanosApiService] to perform that exchange, while [RealHumanosGateway]
 * needs the resulting token to make authenticated calls. The gateway depends on
 * this small read-only interface instead of on `data-auth`.
 *
 * The default binding ([NullHumanosTokenProvider]) returns `null`, which is
 * correct while the integration is dormant. When the real bridge is switched
 * on, `data-auth` provides an implementation backed by the cached session.
 */
fun interface HumanosTokenProvider {
    /** The current bridge JWT, or null if not authenticated / not exchanged yet. */
    suspend fun currentBridgeToken(): String?
}

/**
 * Default no-op provider used while [eco.humanos.android.core.model.common.IntegrationConfig.USE_REAL_HUMANOS_AUTH]
 * is `false`. Always returns null so any accidental real call fails fast and
 * loudly rather than hitting the network with no credentials.
 */
class NullHumanosTokenProvider @Inject constructor() : HumanosTokenProvider {
    override suspend fun currentBridgeToken(): String? = null
}
