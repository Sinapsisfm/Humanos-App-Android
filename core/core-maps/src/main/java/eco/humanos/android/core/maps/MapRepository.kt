/**
 * core-maps / MapRepository.kt
 *
 * Puerto de almacenamiento (dominio). El POC usa InMemoryMapRepository; un adapter Room
 * o de archivos lo implementaría después SIN tocar el dominio (mismo patrón que
 * OutdoorRepository → RoomOutdoorRepository). Síncrono y determinístico.
 */
package eco.humanos.android.core.maps

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

interface MapRepository {
    /** Guarda/actualiza un pack offline (idempotente por packId). */
    fun savePack(pack: OfflineMapPack)
    fun pack(packId: String): OfflineMapPack?
    fun packs(): List<OfflineMapPack>
    fun deletePack(packId: String)

    /** Rutas sueltas (p.ej. importadas de un GPX) fuera de un pack. */
    fun saveRoute(route: RouteGeometry)
    fun route(id: String): RouteGeometry?
    fun routes(): List<RouteGeometry>

    /** Cobertura agregada = unión de las regiones de todos los packs guardados. */
    fun coverage(): MapCoverage

    /** Snapshot serializable (para restaurar tras process death / persistir). */
    fun serialize(): String
}

@Serializable
private data class MapSnapshot(val packs: List<OfflineMapPack>, val routes: List<RouteGeometry>)

class InMemoryMapRepository : MapRepository {
    private val packsById = LinkedHashMap<String, OfflineMapPack>()
    private val routesById = LinkedHashMap<String, RouteGeometry>()

    override fun savePack(pack: OfflineMapPack) { packsById[pack.manifest.packId] = pack }
    override fun pack(packId: String): OfflineMapPack? = packsById[packId]
    override fun packs(): List<OfflineMapPack> = packsById.values.toList()
    override fun deletePack(packId: String) { packsById.remove(packId) }

    override fun saveRoute(route: RouteGeometry) { routesById[route.id] = route }
    override fun route(id: String): RouteGeometry? = routesById[id]
    override fun routes(): List<RouteGeometry> = routesById.values.toList()

    override fun coverage(): MapCoverage = MapCoverage(packsById.values.map { it.manifest.region })

    override fun serialize(): String =
        MapJson.encodeToString(MapSnapshot(packs(), routes()))

    companion object {
        /** Restaura desde un snapshot serializado (process death). */
        fun restore(snapshot: String): InMemoryMapRepository {
            val data = MapJson.decodeFromString<MapSnapshot>(snapshot)
            val repo = InMemoryMapRepository()
            data.packs.forEach(repo::savePack)
            data.routes.forEach(repo::saveRoute)
            return repo
        }
    }
}
