package eco.humanos.android.data.capture.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import eco.humanos.android.data.capture.repository.CaptureRepository
import eco.humanos.android.data.capture.repository.CaptureRepositoryImpl
import javax.inject.Singleton

/**
 * Hilt bindings for the capture data layer.
 *
 * Binds [CaptureRepositoryImpl] (which depends on the DAO from
 * core-database and the trace repository from core-observability) to the
 * [CaptureRepository] contract consumed by the feature layer.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CaptureDataModule {

    @Binds
    @Singleton
    abstract fun bindCaptureRepository(impl: CaptureRepositoryImpl): CaptureRepository
}
