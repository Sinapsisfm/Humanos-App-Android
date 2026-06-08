package eco.humanos.android.data.auth.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import eco.humanos.android.data.auth.AuthRepository
import eco.humanos.android.data.auth.FirebaseAuthRepository
import eco.humanos.android.data.auth.GoogleCredentialManager
import eco.humanos.android.data.auth.GoogleCredentialManagerImpl
import eco.humanos.android.data.auth.VaultHumanosTokenProvider
import eco.humanos.android.integrations.humanos.HumanosTokenProvider
import javax.inject.Singleton

/**
 * Binds the authentication abstractions to their Firebase / Credential Manager
 * backed implementations.
 *
 * Also provides the **single** [HumanosTokenProvider] binding for the app:
 * [VaultHumanosTokenProvider], which reads the bridge JWT from the Secure Vault.
 * It lives here (not in `integration-humanos`'s `HumanosModule`) because
 * `data-auth` owns the bridge session. `HumanosModule` deliberately leaves the
 * provider unbound so there is no duplicate-binding conflict.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindGoogleCredentialManager(impl: GoogleCredentialManagerImpl): GoogleCredentialManager

    /**
     * The real bridge-token provider, backed by the Secure Vault. Replaces the
     * dormant `NullHumanosTokenProvider` now that the integration is live.
     */
    @Binds
    @Singleton
    abstract fun bindHumanosTokenProvider(impl: VaultHumanosTokenProvider): HumanosTokenProvider
}
