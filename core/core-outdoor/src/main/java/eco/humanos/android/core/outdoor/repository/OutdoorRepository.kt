/**
 * core-outdoor / repository / OutdoorRepository.kt
 *
 * Contrato del repositorio local Outdoor + adaptador in-memory serializable
 * (CORE-002 persistencia, CORE-003 restore, CORE-009 idempotencia). Es la FRONTERA DE
 * PERSISTENCIA: en la app, un adaptador Room implementará la MISMA interfaz (D-005).
 *
 * Dos rutas con políticas distintas (D-010):
 *  - exportOuting → compartir/portar: EXCLUYE datos sensibles por defecto (opt-in).
 *  - serialize    → snapshot LOCAL AT-REST para restaurar: INCLUYE todo (cifrar en
 *                   reposo / no compartir; en Room iría en almacenamiento cifrado).
 */
package eco.humanos.android.core.outdoor.repository

import eco.humanos.android.core.outdoor.domain.CampingPlan
import eco.humanos.android.core.outdoor.domain.OutdoorOuting
import eco.humanos.android.core.outdoor.domain.OutingEvent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val REPO_SCHEMA_VERSION = 1

data class ExportedOuting(
    val outing: OutdoorOuting,
    val plan: CampingPlan?,
    val includesSensitive: Boolean,
)

interface OutdoorRepository {
    fun putOuting(outing: OutdoorOuting)
    fun getOuting(id: String): OutdoorOuting?
    fun listOutings(): List<OutdoorOuting>
    fun putPlan(outingId: String, plan: CampingPlan)
    fun getPlan(outingId: String): CampingPlan?
    /** Idempotente: false si el eventId ya existía (no duplica). */
    fun appendEvent(event: OutingEvent): Boolean
    fun getEvents(outingId: String? = null): List<OutingEvent>
    /** Exporta una salida; por defecto SIN datos sensibles (D-010, CORE-007). */
    fun exportOuting(id: String, includeSensitive: Boolean = false): ExportedOuting?
    /** Snapshot local at-rest completo (incluye sensibles; cifrar/no compartir). */
    fun serialize(): String
}

@Serializable
data class OutdoorSnapshot(
    val schemaVersion: Int,
    val outings: List<OutdoorOuting>,
    val plans: List<PlanEntry>,
    val events: List<OutingEvent>,
) {
    @Serializable
    data class PlanEntry(val outingId: String, val plan: CampingPlan)
}

/**
 * Códec ÚNICO de snapshot, compartido por todos los adaptadores (in-memory y Room) → un
 * blob serializado por cualquier repositorio es restaurable en cualquier otro (paridad
 * serialize/restore). Opera sobre la interfaz `OutdoorRepository`.
 */
object OutdoorSnapshotCodec {
    private val JSON = Json { encodeDefaults = true }

    fun encode(repo: OutdoorRepository): String {
        val outings = repo.listOutings()
        val plans = outings.mapNotNull { o -> repo.getPlan(o.id)?.let { OutdoorSnapshot.PlanEntry(o.id, it) } }
        val snapshot = OutdoorSnapshot(REPO_SCHEMA_VERSION, outings, plans, repo.getEvents(null))
        return JSON.encodeToString(OutdoorSnapshot.serializer(), snapshot)
    }

    fun decode(blob: String): OutdoorSnapshot {
        val s = JSON.decodeFromString(OutdoorSnapshot.serializer(), blob)
        require(s.schemaVersion == REPO_SCHEMA_VERSION) {
            "Esquema de repositorio incompatible: ${s.schemaVersion} != $REPO_SCHEMA_VERSION"
        }
        return s
    }

    /** Carga un snapshot en cualquier repositorio (vía la interfaz; idempotente en eventos). */
    fun load(repo: OutdoorRepository, blob: String) {
        val s = decode(blob)
        s.outings.forEach { repo.putOuting(it) }
        s.plans.forEach { repo.putPlan(it.outingId, it.plan) }
        s.events.forEach { repo.appendEvent(it) }
    }
}

class InMemoryOutdoorRepository : OutdoorRepository {
    private val outings = LinkedHashMap<String, OutdoorOuting>()
    private val plans = LinkedHashMap<String, CampingPlan>()
    private val events = LinkedHashMap<String, OutingEvent>() // eventId → event (idempotencia)

    override fun putOuting(outing: OutdoorOuting) { outings[outing.id] = outing }
    override fun getOuting(id: String): OutdoorOuting? = outings[id]
    override fun listOutings(): List<OutdoorOuting> =
        outings.values.sortedWith(compareBy({ it.createdAt }, { it.id }))

    override fun putPlan(outingId: String, plan: CampingPlan) { plans[outingId] = plan }
    override fun getPlan(outingId: String): CampingPlan? = plans[outingId]

    override fun appendEvent(event: OutingEvent): Boolean {
        if (events.containsKey(event.eventId)) return false
        events[event.eventId] = event
        return true
    }

    override fun getEvents(outingId: String?): List<OutingEvent> =
        events.values.filter { outingId == null || it.outingId == outingId }
            .sortedWith(compareBy({ it.at }, { it.eventId }))

    override fun exportOuting(id: String, includeSensitive: Boolean): ExportedOuting? {
        val outing = outings[id] ?: return null
        val exported = if (includeSensitive) outing
        else outing.copy(input = outing.input.copy(dietary = emptyList()))
        return ExportedOuting(exported, plans[id], includeSensitive)
    }

    override fun serialize(): String = OutdoorSnapshotCodec.encode(this)

    companion object {
        /** Reconstruye un repositorio in-memory desde un blob (CORE-003 / CORE-008). */
        fun restore(blob: String): InMemoryOutdoorRepository =
            InMemoryOutdoorRepository().also { OutdoorSnapshotCodec.load(it, blob) }
    }
}
