package eco.humanos.android.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import eco.humanos.android.core.database.entity.TraceEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * Append-only persistence contract for the provenance / audit trail.
 */
@Dao
interface TraceEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TraceEventEntity)

    @Query(
        "SELECT * FROM trace_events " +
            "WHERE entityType = :entityType AND entityId = :entityId " +
            "ORDER BY timestamp DESC",
    )
    fun observeForEntity(entityType: String, entityId: String): Flow<List<TraceEventEntity>>

    @Query("SELECT * FROM trace_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<TraceEventEntity>

    @Query("DELETE FROM trace_events WHERE timestamp < :timestamp")
    suspend fun pruneBefore(timestamp: Long)
}
