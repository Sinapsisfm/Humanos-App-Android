/**
 * data-outdoor / OutdoorDatabase.kt
 *
 * Base de datos Room SEPARADA para Outdoor (`outdoor.db`). Versión 1, `exportSchema=true`
 * → el esquema se versiona en `data/data-outdoor/schemas/`. Al ser una DB nueva e
 * independiente, NO requiere migración de la `HumanosDatabase` existente (cero riesgo
 * sobre datos actuales). Las migraciones futuras de ESTA DB deben ser aditivas.
 */
package eco.humanos.android.data.outdoor

import androidx.room.Database
import androidx.room.RoomDatabase

const val OUTDOOR_DATABASE_NAME = "outdoor.db"

@Database(
    entities = [
        OutdoorOutingEntity::class,
        OutdoorPlanEntity::class,
        OutdoorEventEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class OutdoorDatabase : RoomDatabase() {
    abstract fun outdoorDao(): OutdoorDao
}
