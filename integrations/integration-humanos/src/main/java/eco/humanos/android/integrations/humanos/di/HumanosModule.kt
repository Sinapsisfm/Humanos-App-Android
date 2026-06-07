package eco.humanos.android.integrations.humanos.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import eco.humanos.android.integrations.humanos.FakeHumanosGateway
import eco.humanos.android.integrations.humanos.HumanosGateway
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HumanosModule {

    @Binds
    @Singleton
    abstract fun bindHumanosGateway(fake: FakeHumanosGateway): HumanosGateway
}
