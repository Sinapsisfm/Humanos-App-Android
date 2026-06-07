package eco.humanos.android.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import eco.humanos.android.core.database.converter.Converters
import eco.humanos.android.core.database.dao.CaptureDao
import eco.humanos.android.core.database.dao.TaskDao
import eco.humanos.android.core.database.dao.TraceEventDao
import eco.humanos.android.core.database.entity.CaptureEntity
import eco.humanos.android.core.database.entity.TaskEntity
import eco.humanos.android.core.database.entity.TraceEventEntity

/**
 * The on-device Room database for HumanOS.
 *
 * Holds local persistence for captures, tasks, and the provenance trail.
 * Entities are deliberately separate from the pure-Kotlin domain models in
 * `core-model`; convert with the `toDomain()` / `toEntity()` mappers.
 */
@Database(
    entities = [CaptureEntity::class, TaskEntity::class, TraceEventEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class HumanosDatabase : RoomDatabase() {
    abstract fun captureDao(): CaptureDao
    abstract fun taskDao(): TaskDao
    abstract fun traceEventDao(): TraceEventDao
}

/**
 * Constants for the Room database.
 */
object DatabaseConstants {
    const val DATABASE_NAME = "humanos.db"
}
