package eco.humanos.android.core.security.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import eco.humanos.android.core.security.vault.EncryptedHumanOSSessionStore
import eco.humanos.android.core.security.vault.HumanOSSessionStore
import javax.inject.Singleton

/**
 * Binds security abstractions to their Keystore-backed implementations.
 *
 * The [HumanOSSessionStore] ("Secure Vault") is bound as a singleton so the
 * encrypted prefs handle and its in-memory auth-state flow are shared across
 * the app (auth repository, observers, etc.).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindHumanOSSessionStore(
        impl: EncryptedHumanOSSessionStore,
    ): HumanOSSessionStore
}
