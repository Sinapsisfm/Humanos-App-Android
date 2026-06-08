package eco.humanos.android.data.auth.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import eco.humanos.android.data.auth.AuthRepository
import eco.humanos.android.data.auth.FirebaseAuthRepository
import eco.humanos.android.data.auth.GoogleCredentialManager
import eco.humanos.android.data.auth.GoogleCredentialManagerImpl
import javax.inject.Singleton

/**
 * Binds the authentication abstractions to their Firebase / Credential Manager
 * backed implementations.
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
}
