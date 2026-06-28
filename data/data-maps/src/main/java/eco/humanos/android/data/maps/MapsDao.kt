/**
 * data-maps / MapsDao.kt
 *
 * DAO Room para packs/rutas de mapas. Upsert idempotente por clave primaria (REPLACE).
 * Ordenado por clave para lecturas determinísticas.
 */
package eco.humanos.android.data.maps

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MapsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertPack(entity: MapPackEntity)

    @Query("SELECT * FROM map_pack WHERE packId = :packId")
    fun getPack(packId: String): MapPackEntity?

    @Query("SELECT * FROM map_pack ORDER BY packId")
    fun allPacks(): List<MapPackEntity>

    @Query("DELETE FROM map_pack WHERE packId = :packId")
    fun deletePack(packId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertRoute(entity: MapRouteEntity)

    @Query("SELECT * FROM map_route WHERE routeId = :routeId")
    fun getRoute(routeId: String): MapRouteEntity?

    @Query("SELECT * FROM map_route ORDER BY routeId")
    fun allRoutes(): List<MapRouteEntity>
}
