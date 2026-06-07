package eco.humanos.android.integrations.quebot.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import eco.humanos.android.integrations.quebot.FakeQuebotGateway
import eco.humanos.android.integrations.quebot.QuebotGateway
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class QuebotModule {

    @Binds
    @Singleton
    abstract fun bindQuebotGateway(fake: FakeQuebotGateway): QuebotGateway
}
