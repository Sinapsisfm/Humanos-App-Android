package eco.humanos.android.integrations.humanos.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import eco.humanos.android.integrations.humanos.FakeHumanosGateway
import eco.humanos.android.integrations.humanos.HumanosGateway
import eco.humanos.android.integrations.humanos.HumanosTokenProvider
import eco.humanos.android.integrations.humanos.NullHumanosTokenProvider
import eco.humanos.android.integrations.humanos.RealHumanosGateway
import javax.inject.Singleton

/**
 * Binds the HumanOS gateway and its token provider.
 *
 * ## Gateway swap point (go-live)
 * The app ships with [FakeHumanosGateway] bound so it builds and runs entirely
 * offline. To switch to the real backend once
 * `POST /api/auth/mobile/exchange` is deployed and
 * [eco.humanos.android.core.model.common.IntegrationConfig.USE_REAL_HUMANOS_AUTH]
 * is `true`:
 *
 * 1. Change [bindHumanosGateway]'s parameter type from `FakeHumanosGateway`
 *    to [RealHumanosGateway] (the only edit needed here):
 *    ```
 *    abstract fun bindHumanosGateway(real: RealHumanosGateway): HumanosGateway
 *    ```
 * 2. Replace [bindHumanosTokenProvider]'s [NullHumanosTokenProvider] with the
 *    real provider exposed by `data-auth` (backed by the cached bridge session).
 *
 * Both [RealHumanosGateway] and its Retrofit dependencies (see
 * `HumanosNetworkModule`) are already in the graph, so the swap is a two-line
 * change with no new wiring.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class HumanosModule {

    // ── Gateway ──────────────────────────────────────────────────────────────
    // DEFAULT: fake gateway (offline). Swap to RealHumanosGateway to go live.
    @Binds
    @Singleton
    abstract fun bindHumanosGateway(fake: FakeHumanosGateway): HumanosGateway

    // ── Bridge token provider ────────────────────────────────────────────────
    // DEFAULT: null provider (no token). Swap to the data-auth-backed provider
    // when going live so RealHumanosGateway can authenticate API calls.
    @Binds
    @Singleton
    abstract fun bindHumanosTokenProvider(
        impl: NullHumanosTokenProvider,
    ): HumanosTokenProvider
}
