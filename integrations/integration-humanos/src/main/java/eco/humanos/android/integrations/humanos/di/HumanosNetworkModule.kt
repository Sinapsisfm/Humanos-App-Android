package eco.humanos.android.integrations.humanos.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import eco.humanos.android.core.model.common.IntegrationConfig
import eco.humanos.android.core.network.NetworkConstants
import eco.humanos.android.integrations.humanos.HumanosApiService
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Provides the Retrofit stack for the HumanOS API.
 *
 * These bindings are always available in the graph, but nothing consumes
 * [HumanosApiService] at runtime until either:
 *  - [IntegrationConfig.USE_REAL_HUMANOS_AUTH] is `true` (FirebaseAuthRepository
 *    then calls the exchange endpoint), or
 *  - `HumanosModule` is switched to bind `RealHumanosGateway`.
 *
 * So while the flag is `false` (default) these providers are inert — no network
 * traffic is generated and the fake gateway remains the source of truth.
 */
@Module
@InstallIn(SingletonComponent::class)
object HumanosNetworkModule {

    /**
     * Lenient JSON: ignore unknown server fields (forward-compatible) and
     * tolerate absent optionals. Matches the defensive DTO mapping.
     */
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides
    @Singleton
    @Named("humanos")
    fun provideHumanosOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // BASIC avoids logging Authorization headers / token bodies.
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .connectTimeout(NetworkConstants.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NetworkConstants.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(NetworkConstants.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    @Named("humanos")
    fun provideHumanosRetrofit(
        @Named("humanos") client: OkHttpClient,
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(IntegrationConfig.HUMANOS_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideHumanosApiService(
        @Named("humanos") retrofit: Retrofit,
    ): HumanosApiService = retrofit.create(HumanosApiService::class.java)
}
