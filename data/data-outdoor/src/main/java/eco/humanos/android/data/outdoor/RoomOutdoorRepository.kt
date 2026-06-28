/**
 * data-outdoor / RoomOutdoorRepository.kt
 *
 * Implementación Room del contrato `OutdoorRepository` (la MISMA interfaz que usa el
 * adaptador in-memory). Persistencia en dispositivo; "restore tras cierre forzado" =
 * reabrir la DB. Idempotencia de eventos vía INSERT IGNORE.
 *
 * GATE: NO está conectado como repositorio por defecto (sin módulo Hilt que lo provea).
 * Su correctitud de DAO/migración requiere pruebas instrumentadas (ver androidTest).
 * Los mappers sí están unit-testeados en JVM.
 */
package eco.humanos.android.data.outdoor

import android.content.Context
import eco.humanos.android.core.outdoor.domain.CampingPlan
import eco.humanos.android.core.outdoor.domain.OutdoorOuting
import eco.humanos.android.core.outdoor.domain.OutingEvent
import eco.humanos.android.core.outdoor.repository.ExportedOuting
import eco.humanos.android.core.outdoor.repository.OutdoorRepository
import eco.humanos.android.core.outdoor.repository.REPO_SCHEMA_VERSION
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/**
 * Factory que devuelve el contrato de dominio `OutdoorRepository` (no expone tipos Room).
 * Permite al app construir el adaptador Room sin tener androidx.room en su classpath.
 */
fun createRoomOutdoorRepository(context: Context): OutdoorRepository =
    RoomOutdoorRepository(createOutdoorDatabase(context).outdoorDao())

class RoomOutdoorRepository(private val dao: OutdoorDao) : OutdoorRepository {

    override fun putOuting(outing: OutdoorOuting) = dao.upsertOuting(outing.toEntity())

    override fun getOuting(id: String): OutdoorOuting? = dao.getOuting(id)?.toDomain()

    override fun listOutings(): List<OutdoorOuting> = dao.allOutings().map { it.toDomain() }

    override fun putPlan(outingId: String, plan: CampingPlan) = dao.upsertPlan(plan.toEntity(outingId))

    override fun getPlan(outingId: String): CampingPlan? = dao.getPlan(outingId)?.toDomain()

    override fun appendEvent(event: OutingEvent): Boolean = dao.insertEvent(event.toEntity()) != -1L

    override fun getEvents(outingId: String?): List<OutingEvent> = dao.events(outingId).map { it.toDomain() }

    override fun exportOuting(id: String, includeSensitive: Boolean): ExportedOuting? {
        val outing = getOuting(id) ?: return null
        val exported = if (includeSensitive) outing
        else outing.copy(input = outing.input.copy(dietary = emptyList()))
        return ExportedOuting(exported, getPlan(id), includeSensitive)
    }

    @Serializable
    private data class PlanEntry(val outingId: String, val plan: CampingPlan)

    @Serializable
    private data class RoomSnapshot(
        val schemaVersion: Int,
        val outings: List<OutdoorOuting>,
        val plans: List<PlanEntry>,
        val events: List<OutingEvent>,
    )

    override fun serialize(): String {
        val outings = listOutings()
        val plans = outings.mapNotNull { o -> getPlan(o.id)?.let { PlanEntry(o.id, it) } }
        val snapshot = RoomSnapshot(REPO_SCHEMA_VERSION, outings, plans, getEvents(null))
        return OUTDOOR_JSON.encodeToString(snapshot)
    }
}
