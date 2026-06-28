/**
 * data-outdoor / OutdoorDatabase.kt
 *
 * Base de datos Room SEPARADA para Outdoor (`outdoor.db`). Versión 1, `exportSchema=true`
 * → el esquema se versiona en `data/data-outdoor/schemas/`. Al ser una DB nueva e
 * independiente, NO requiere migración de la `HumanosDatabase` existente (cero riesgo
 * sobre datos actuales). Las migraciones futuras de ESTA DB deben ser aditivas.
 */
package eco.humanos.android.data.outdoor

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

const val OUTDOOR_DATABASE_NAME = "outdoor.db"

/**
 * Factory de la DB Outdoor. Mantiene el uso de Room DENTRO de data-outdoor (el app no
 * importa androidx.room). Migraciones futuras (aditivas) se agregan aquí.
 *
 * NOTA DE ACTIVACIÓN (NIT-2 del review): `OutdoorRepository` es SÍNCRONO por contrato y
 * `RoomOutdoorRepository` ejecuta queries Room de forma síncrona. Room prohíbe queries en
 * el main thread. Hoy Room está OFF por defecto (OUTDOOR_ROOM_ENABLED=false), así que no
 * aplica. Al ACTIVAR Room hay que invocar el repositorio FUERA del main thread (p.ej. el
 * ViewModel debe usar viewModelScope + Dispatchers.IO); NO usar allowMainThreadQueries en
 * producción. Este factory deja la DB lista; la disciplina de hilo es responsabilidad del
 * consumidor al momento de la activación.
 */
fun createOutdoorDatabase(context: Context): OutdoorDatabase =
    Room.databaseBuilder(context, OutdoorDatabase::class.java, OUTDOOR_DATABASE_NAME).build()

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
