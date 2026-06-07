package eco.humanos.android.core.database.dao

/**
 * Phase 1 stub -- Room DAO interface for local capture persistence.
 *
 * Actual Room annotations (@Query, @Insert, @Delete, @Update) will be
 * added when the database schema is wired in a later phase. For now
 * this serves as a compile-time contract that higher layers can depend on.
 */
interface CaptureDao {
    // @Insert(onConflict = OnConflictStrategy.REPLACE)
    // suspend fun upsert(capture: CaptureEntity)

    // @Query("SELECT * FROM captures ORDER BY created_at DESC")
    // fun observeAll(): Flow<List<CaptureEntity>>

    // @Query("SELECT * FROM captures WHERE id = :captureId")
    // suspend fun getById(captureId: String): CaptureEntity?

    // @Delete
    // suspend fun delete(capture: CaptureEntity)
}
