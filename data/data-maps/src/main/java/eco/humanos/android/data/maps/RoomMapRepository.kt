/**
 * data-maps / RoomMapRepository.kt
 *
 * Implementación Room del contrato `MapRepository` (la MISMA interfaz que el adaptador
 * in-memory). Persistencia en `maps.db`. "restore tras cierre" = reabrir la DB.
 *
 * ADAPTER DESACTIVADO: NO está cableado como repositorio por defecto (sin módulo Hilt). Su
 * correctitud de DAO se prueba vía Robolectric/instrumented; los mappers en JVM. Síncrono →
 * el consumidor futuro debe invocarlo fuera del main thread.
 */
package eco.humanos.android.data.maps

import android.content.Context
import eco.humanos.android.core.maps.MapCoverage
import eco.humanos.android.core.maps.MapRepository
import eco.humanos.android.core.maps.MapSnapshotCodec
import eco.humanos.android.core.maps.OfflineMapPack
import eco.humanos.android.core.maps.RouteGeometry

/** Factory que devuelve el contrato de dominio (no expone tipos Room al consumidor). */
fun createRoomMapRepository(context: Context): MapRepository =
    RoomMapRepository(createMapsDatabase(context).mapsDao())

class RoomMapRepository(private val dao: MapsDao) : MapRepository {

    override fun savePack(pack: OfflineMapPack) = dao.upsertPack(pack.toEntity())
    override fun pack(packId: String): OfflineMapPack? = dao.getPack(packId)?.toDomain()
    override fun packs(): List<OfflineMapPack> = dao.allPacks().map { it.toDomain() }
    override fun deletePack(packId: String) = dao.deletePack(packId)

    override fun saveRoute(route: RouteGeometry) = dao.upsertRoute(route.toEntity())
    override fun route(id: String): RouteGeometry? = dao.getRoute(id)?.toDomain()
    override fun routes(): List<RouteGeometry> = dao.allRoutes().map { it.toDomain() }

    override fun coverage(): MapCoverage = MapCoverage(packs().map { it.manifest.region })

    /** Usa el códec único → el blob es restaurable en cualquier repositorio (paridad). */
    override fun serialize(): String = MapSnapshotCodec.encode(this)
}
