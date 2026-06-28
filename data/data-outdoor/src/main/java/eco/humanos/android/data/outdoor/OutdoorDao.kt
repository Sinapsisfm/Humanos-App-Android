/**
 * data-outdoor / OutdoorDao.kt
 *
 * DAO síncrono (coincide con el contrato síncrono `OutdoorRepository`). `appendEvent`
 * usa INSERT … OnConflict IGNORE → idempotente por `eventId` (CORE-009): devuelve el
 * rowId (-1 si ya existía).
 */
package eco.humanos.android.data.outdoor

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface OutdoorDao {
    @Upsert
    fun upsertOuting(entity: OutdoorOutingEntity)

    @Query("SELECT * FROM outdoor_outings WHERE id = :id")
    fun getOuting(id: String): OutdoorOutingEntity?

    @Query("SELECT * FROM outdoor_outings ORDER BY createdAt ASC, id ASC")
    fun allOutings(): List<OutdoorOutingEntity>

    @Upsert
    fun upsertPlan(entity: OutdoorPlanEntity)

    @Query("SELECT * FROM outdoor_plans WHERE outingId = :outingId")
    fun getPlan(outingId: String): OutdoorPlanEntity?

    /** Idempotente: ignora si el eventId ya existe; devuelve rowId (-1 si no insertó). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertEvent(entity: OutdoorEventEntity): Long

    @Query("SELECT * FROM outdoor_events WHERE (:outingId IS NULL OR outingId = :outingId) ORDER BY at ASC, eventId ASC")
    fun events(outingId: String?): List<OutdoorEventEntity>
}
