/**
 * app / di / OutdoorDataModule.kt
 *
 * Binding Hilt de `OutdoorRepository` con SELECCIÓN:
 *  - default = `InMemoryOutdoorRepository` (repositorio actual) — comportamiento de release
 *    sin cambios.
 *  - `RoomOutdoorRepository` SOLO si `BuildConfig.OUTDOOR_ROOM_ENABLED` (flag interno,
 *    default false en todos los build types) → Room NO es default hasta tener evidencia.
 *  - en tests, este módulo se reemplaza vía `@TestInstallIn` con un fake.
 *
 * No hay segundo service locator: Hilt resuelve la composición. `core-outdoor` no se acopla
 * a Android/Room/Hilt (la interfaz vive en core; los adapters en data/core).
 */
package eco.humanos.android.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import eco.humanos.android.BuildConfig
import eco.humanos.android.core.outdoor.repository.InMemoryOutdoorRepository
import eco.humanos.android.core.outdoor.repository.OutdoorRepository
import eco.humanos.android.data.outdoor.createRoomOutdoorRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OutdoorDataModule {

    @Provides
    @Singleton
    fun provideOutdoorRepository(
        @ApplicationContext context: Context,
    ): OutdoorRepository = selectOutdoorRepository(
        roomEnabled = BuildConfig.OUTDOOR_ROOM_ENABLED,
        room = { createRoomOutdoorRepository(context) },
        inMemory = { InMemoryOutdoorRepository() },
    )
}
