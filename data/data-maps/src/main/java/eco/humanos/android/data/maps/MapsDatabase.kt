/**
 * data-maps / MapsDatabase.kt
 *
 * Base de datos Room SEPARADA para mapas (`maps.db`). Versión 1, `exportSchema=true` →
 * el esquema se versiona en `data/data-maps/schemas/`. DB nueva e independiente: NO migra
 * `HumanosDatabase` ni `outdoor.db`. Migraciones futuras de ESTA DB deben ser aditivas.
 *
 * ADAPTER DESACTIVADO: no hay módulo Hilt ni consumidor que lo provea por defecto. Sólo se
 * instancia en tests. Igual que en Outdoor, `MapRepository` es síncrono → al cablearlo en el
 * futuro hay que invocarlo FUERA del main thread (viewModelScope + Dispatchers.IO); NO usar
 * allowMainThreadQueries en producción. Sin allowMainThreadQueries aquí → fail-closed si se
 * usa mal en el main thread.
 */
package eco.humanos.android.data.maps

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

const val MAPS_DATABASE_NAME = "maps.db"

fun createMapsDatabase(context: Context): MapsDatabase =
    Room.databaseBuilder(context, MapsDatabase::class.java, MAPS_DATABASE_NAME).build()

@Database(
    entities = [MapPackEntity::class, MapRouteEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class MapsDatabase : RoomDatabase() {
    abstract fun mapsDao(): MapsDao
}
