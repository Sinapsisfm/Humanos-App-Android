/**
 * data-maps / MapsMappers.kt
 *
 * Mapeo dominio ↔ entidad. El BLOB es el JSON canónico (MapJson), el MISMO que usa el códec
 * de snapshot → un pack persistido por Room es byte-idéntico al serializado en memoria.
 */
package eco.humanos.android.data.maps

import eco.humanos.android.core.maps.MapJson
import eco.humanos.android.core.maps.OfflineMapPack
import eco.humanos.android.core.maps.RouteGeometry
import kotlinx.serialization.encodeToString

fun OfflineMapPack.toEntity(): MapPackEntity =
    MapPackEntity(packId = manifest.packId, json = MapJson.encodeToString(this))

fun MapPackEntity.toDomain(): OfflineMapPack = MapJson.decodeFromString(json)

fun RouteGeometry.toEntity(): MapRouteEntity =
    MapRouteEntity(routeId = id, json = MapJson.encodeToString(this))

fun MapRouteEntity.toDomain(): RouteGeometry = MapJson.decodeFromString(json)
