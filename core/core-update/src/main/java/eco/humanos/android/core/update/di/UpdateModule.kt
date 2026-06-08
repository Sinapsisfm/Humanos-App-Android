package eco.humanos.android.core.update.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import eco.humanos.android.core.update.GitHubUpdateChecker
import eco.humanos.android.core.update.UpdateChecker
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Wires the update-checking stack:
 *  - a dedicated [OkHttpClient] (qualified `@Named("update")` so it never clashes
 *    with the API clients in other modules), and
 *  - the [UpdateChecker] → [GitHubUpdateChecker] binding.
 *
 * The client uses short timeouts: a stalled update check should fail fast and
 * quietly rather than hold up the Settings screen.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class UpdateModule {

    @Binds
    @Singleton
    abstract fun bindUpdateChecker(impl: GitHubUpdateChecker): UpdateChecker

    companion object {
        @Provides
        @Singleton
        @Named("update")
        fun provideUpdateOkHttpClient(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
    }
}
