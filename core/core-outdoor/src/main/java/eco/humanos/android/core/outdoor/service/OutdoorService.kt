/**
 * core-outdoor / service / OutdoorService.kt
 *
 * Capa de casos de uso que une motor + repositorio + awareness en operaciones que un
 * ViewModel nativo puede invocar. Pura (depende solo del contrato `OutdoorRepository`),
 * determinística (reloj inyectado vía `Clock`), sin Android.
 */
package eco.humanos.android.core.outdoor.service

import eco.humanos.android.core.outdoor.awareness.AwarenessEvaluator
import eco.humanos.android.core.outdoor.awareness.AwarenessSignal
import eco.humanos.android.core.outdoor.domain.CampingPlan
import eco.humanos.android.core.outdoor.domain.Intent
import eco.humanos.android.core.outdoor.domain.OutdoorOuting
import eco.humanos.android.core.outdoor.domain.OutingEvent
import eco.humanos.android.core.outdoor.domain.OutingEventType
import eco.humanos.android.core.outdoor.domain.OutingStatus
import eco.humanos.android.core.outdoor.domain.PackingInput
import eco.humanos.android.core.outdoor.domain.SourceType
import eco.humanos.android.core.outdoor.packing.PackingEngine
import eco.humanos.android.core.outdoor.packing.RULESET_VERSION
import eco.humanos.android.core.outdoor.repository.OutdoorRepository

/** Reloj inyectable (sin reloj interno → determinismo en tests). */
fun interface Clock {
    fun nowIso(): String
}

/** Vista combinada para la UI: salida + plan + señales situacionales. */
data class OutingView(
    val outing: OutdoorOuting,
    val plan: CampingPlan,
    /**
     * Señales básicas (omisiones de camping) sin timestamp (`at = null`). Para señales
     * situacionales con timestamp/regla/acción usar `AwarenessEvaluator.evaluateSituational`.
     */
    val signals: List<AwarenessSignal>,
)

class OutdoorService(
    private val repo: OutdoorRepository,
    private val clock: Clock,
) {
    /** Crea una salida de camping, genera el plan inicial y persiste (+ evento). */
    fun createCampingOuting(
        id: String,
        title: String,
        input: PackingInput,
        intent: Intent = Intent.CAMPING_VACACIONES,
    ): OutingView {
        val now = clock.nowIso()
        val outing = OutdoorOuting(
            id = id,
            title = title,
            intent = intent,
            scenario = input.scenario,
            status = OutingStatus.DRAFT,
            rulesetVersion = RULESET_VERSION,
            input = input,
            createdAt = now,
            updatedAt = now,
        )
        val plan = PackingEngine.buildCampingPlan(input)
        repo.putOuting(outing)
        repo.putPlan(id, plan)
        emit(id, "create", OutingEventType.OUTING_CREATED, SourceType.USER_DECLARATION, now)
        emit(id, "plan", OutingEventType.PACKING_GENERATED, SourceType.DETERMINISTIC_INFERENCE, now)
        return view(outing, plan)
    }

    /** Aplica ediciones del usuario al plan y re-persiste (idempotente por eventId). */
    fun applyEdits(outingId: String, edits: List<PackingEngine.UserEdit>): OutingView? {
        val outing = repo.getOuting(outingId) ?: return null
        val plan = repo.getPlan(outingId) ?: return null
        if (edits.isEmpty()) return view(outing, plan)
        val newItems = PackingEngine.applyUserEdits(plan.items, edits)
        val newPlan = plan.copy(items = newItems)
        val now = clock.nowIso()
        val updated = outing.copy(updatedAt = now)
        repo.putOuting(updated)
        repo.putPlan(outingId, newPlan)
        emit(outingId, "edit:${editKey(edits)}", OutingEventType.PACKING_ITEM_EDITED, SourceType.USER_DECLARATION, now)
        return view(updated, newPlan)
    }

    /** Recalcula el plan desde las reglas (mismo input) — útil tras cambio de reglas. */
    fun regeneratePlan(outingId: String): OutingView? {
        val outing = repo.getOuting(outingId) ?: return null
        val plan = PackingEngine.buildCampingPlan(outing.input)
        val now = clock.nowIso()
        repo.putPlan(outingId, plan)
        emit(outingId, "regen", OutingEventType.PACKING_GENERATED, SourceType.DETERMINISTIC_INFERENCE, now)
        return view(outing, plan)
    }

    /** Registra el reconocimiento/descarte de una señal en la bitácora (idempotente). */
    fun recordSignalDismissed(outingId: String, code: String): Boolean {
        if (repo.getOuting(outingId) == null) return false
        emit(outingId, "dismiss:$code", OutingEventType.AWARENESS_SIGNAL_DISMISSED, SourceType.USER_DECLARATION, clock.nowIso())
        return true
    }

    /** Señales situacionales sobre el plan actual (no certifican seguridad). */
    fun signals(outingId: String): List<AwarenessSignal> {
        val outing = repo.getOuting(outingId) ?: return emptyList()
        val plan = repo.getPlan(outingId) ?: return emptyList()
        return AwarenessEvaluator.evaluate(outing, plan.items)
    }

    /** Vista combinada actual de una salida. */
    fun outingView(outingId: String): OutingView? {
        val outing = repo.getOuting(outingId) ?: return null
        val plan = repo.getPlan(outingId) ?: return null
        return view(outing, plan)
    }

    private fun view(outing: OutdoorOuting, plan: CampingPlan) =
        OutingView(outing, plan, AwarenessEvaluator.evaluate(outing, plan.items))

    private fun editKey(edits: List<PackingEngine.UserEdit>): String =
        edits.joinToString(",") { it::class.simpleName ?: "edit" }

    /**
     * Emite un evento con `eventId` ÚNICO: incluye una secuencia por salida además del
     * timestamp, de modo que dos operaciones del mismo tipo en el mismo instante no
     * colisionen (no se pierde ningún evento de la bitácora).
     */
    private fun emit(outingId: String, tag: String, type: OutingEventType, source: SourceType, at: String) {
        val seq = repo.getEvents(outingId).size
        repo.appendEvent(
            OutingEvent(
                eventId = "$outingId:$tag:$at:$seq",
                outingId = outingId,
                type = type,
                sourceType = source,
                at = at,
            ),
        )
    }
}
