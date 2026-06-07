package eco.humanos.android.core.observability.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import eco.humanos.android.core.observability.InMemoryTraceRepository
import eco.humanos.android.core.observability.TraceRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ObservabilityModule {

    @Binds
    @Singleton
    abstract fun bindTraceRepository(impl: InMemoryTraceRepository): TraceRepository
}
