package eco.humanos.android.core.observability.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import eco.humanos.android.core.observability.RoomTraceRepository
import eco.humanos.android.core.observability.TraceRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ObservabilityModule {

    // Room-backed (durable) since Tanda 19. InMemoryTraceRepository remains
    // in the module for unit tests / fallback but is no longer bound.
    @Binds
    @Singleton
    abstract fun bindTraceRepository(impl: RoomTraceRepository): TraceRepository
}
