package eco.humanos.android.integrations.humanos.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import eco.humanos.android.integrations.humanos.HumanosGateway
import eco.humanos.android.integrations.humanos.RealHumanosGateway
import javax.inject.Singleton

/**
 * Binds the HumanOS gateway to its real, network-backed implementation.
 *
 * ## Live (real backend)
 * As of go-live —
 * [eco.humanos.android.core.model.common.IntegrationConfig.USE_REAL_HUMANOS_AUTH]
 * `= true` and `POST /api/auth/mobile/exchange` deployed — [RealHumanosGateway]
 * is bound here and talks to the real HumanOS REST API via `HumanosApiService`
 * (Retrofit stack in `HumanosNetworkModule`).
 *
 * ## Token provider lives in `data-auth`
 * [RealHumanosGateway] needs the **bridge JWT** for authenticated calls; that
 * token is owned by `data-auth` (which performs the Firebase -> bridge exchange
 * and caches the session in the Secure Vault). The `HumanosTokenProvider`
 * binding is therefore provided by `data-auth`'s `AuthModule`
 * (`VaultHumanosTokenProvider`) — **not here** — so the graph has exactly one
 * binding for it. Binding it here too would be a duplicate-binding Hilt error.
 *
 * ## Reverting to offline fakes (dev only)
 * To run fully offline again, rebind `FakeHumanosGateway` here, flip the flag
 * back to `false`, and restore a `NullHumanosTokenProvider` binding (removing
 * `data-auth`'s `VaultHumanosTokenProvider` binding to avoid a duplicate).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class HumanosModule {

    // LIVE: real network gateway. (Rebind FakeHumanosGateway to go offline.)
    @Binds
    @Singleton
    abstract fun bindHumanosGateway(real: RealHumanosGateway): HumanosGateway

    // NOTE: HumanosTokenProvider is intentionally NOT bound here. Its single
    // binding (VaultHumanosTokenProvider) lives in data-auth's AuthModule, which
    // reads the bridge token from the Secure Vault. Adding a binding here would
    // be a duplicate-binding Hilt error.
}
