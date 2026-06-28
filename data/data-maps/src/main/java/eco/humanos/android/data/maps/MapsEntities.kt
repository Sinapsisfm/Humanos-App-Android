/**
 * data-maps / MapsEntities.kt
 *
 * Almacenamiento de mapas (R3, ADAPTER AISLADO/DESACTIVADO). Guarda packs y rutas como
 * BLOBs JSON canónicos (mismo formato MapJson que el códec de snapshot) → paridad total con
 * el adaptador in-memory. Sin tiles, sin proveedor, sin red. DB separada `maps.db`.
 */
package eco.humanos.android.data.maps

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "map_pack")
data class MapPackEntity(
    @PrimaryKey val packId: String,
    val json: String,
)

@Entity(tableName = "map_route")
data class MapRouteEntity(
    @PrimaryKey val routeId: String,
    val json: String,
)
