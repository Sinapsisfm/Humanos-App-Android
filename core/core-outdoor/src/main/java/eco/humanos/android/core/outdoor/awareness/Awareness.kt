/**
 * core-outdoor / awareness / Awareness.kt
 *
 * Contrato de señales situacionales + evaluador determinístico local (Fase 4). Detecta
 * omisiones/contradicciones simples de camping. AVISOS explicables y descartables; NO
 * certifican seguridad, NO diagnostican. Reutiliza el patrón epistémico (confidence/stance).
 */
package eco.humanos.android.core.outdoor.awareness

import eco.humanos.android.core.outdoor.domain.OutdoorOuting
import eco.humanos.android.core.outdoor.domain.OutingPackingItem
import eco.humanos.android.core.outdoor.domain.PackingCategory
import eco.humanos.android.core.outdoor.domain.ScenarioKind
import eco.humanos.android.core.outdoor.gate.ComplexityClass
import eco.humanos.android.core.outdoor.gate.TripContext
import eco.humanos.android.core.outdoor.gate.TripGate
import eco.humanos.android.core.outdoor.presentation.PackingPresentation

const val AWARENESS_VERSION = "awareness-cl.v0.1.0"

enum class SignalSeverity { INFO, ADVERTENCIA, CRITICO }
enum class SignalStance { OPORTUNIDAD, AMENAZA, NEUTRAL }
enum class SignalConfidence { ALTA, MEDIA, DUDOSA }

data class AwarenessSignal(
    val code: String,
    val severity: SignalSeverity,
    val stance: SignalStance,
    val confidence: SignalConfidence,
    val title: String,
    val detail: String,
    /** Hechos/factor que originó la señal (trazabilidad). */
    val sourceFactor: String,
    /** Siempre descartable (el usuario puede reconocer/descartar con motivo). */
    val dismissible: Boolean = true,
    /** Regla/fuente que la produjo. */
    val ruleId: String = "",
    /** Acción sugerida (no autoritaria). */
    val action: String? = null,
    /** Timestamp ISO8601 inyectado (sin reloj interno); null en el evaluador R1 simple. */
    val at: String? = null,
    val rulesetVersion: String = AWARENESS_VERSION,
)

/** Frontera hacia un AwarenessOS de plataforma (sin runtime: no se afirma integración). */
interface AwarenessSink {
    fun publish(outingId: String, signals: List<AwarenessSignal>)
}

/** Sumidero nulo por defecto: el evaluador funciona sin plataforma. */
object NoopAwarenessSink : AwarenessSink {
    override fun publish(outingId: String, signals: List<AwarenessSignal>) { /* no-op */ }
}

object AwarenessEvaluator {

    private fun rank(s: SignalSeverity): Int = when (s) {
        SignalSeverity.CRITICO -> 3
        SignalSeverity.ADVERTENCIA -> 2
        SignalSeverity.INFO -> 1
    }

    private fun hasActive(items: List<OutingPackingItem>, pred: (OutingPackingItem) -> Boolean): Boolean =
        items.any { !it.removed && it.quantity > 0 && pred(it) }

    fun evaluate(outing: OutdoorOuting, items: List<OutingPackingItem>): List<AwarenessSignal> {
        val signals = ArrayList<AwarenessSignal>()
        val input = outing.input
        val youngChildren = input.participants.count { it.ageClass.name == "INFANT" || it.ageClass.name == "CHILD" }

        if (input.nights > 0 && !hasActive(items) { it.category == PackingCategory.LIGHTING }) {
            signals.add(AwarenessSignal(
                "camping.no_lighting", SignalSeverity.ADVERTENCIA, SignalStance.AMENAZA, SignalConfidence.ALTA,
                "Noches sin iluminación registrada",
                "La salida tiene ${input.nights} noche(s) pero no hay iluminación en la lista. Considerá agregar un farol o linterna.",
                "nights=${input.nights}; lighting_items=0",
            ))
        }
        if (youngChildren > 0 && !hasActive(items) { it.key == "clothing.warm_child" }) {
            signals.add(AwarenessSignal(
                "camping.children_no_warmth", SignalSeverity.ADVERTENCIA, SignalStance.AMENAZA, SignalConfidence.ALTA,
                "Niños sin abrigo registrado",
                "Hay $youngChildren niño(s) en el grupo y no se registró abrigo de niño. Los niños son más sensibles al frío.",
                "young_children=$youngChildren; child_warmth_items=0",
            ))
        }
        if (input.nights > 0 && !input.facility.refrigeration && !hasActive(items) { it.key == "food.cooler" }) {
            signals.add(AwarenessSignal(
                "camping.no_cold_source", SignalSeverity.ADVERTENCIA, SignalStance.AMENAZA, SignalConfidence.MEDIA,
                "Sin fuente de frío para los alimentos",
                "El sitio no tiene refrigeración y no hay cooler en la lista. Planificá conservación de alimentos sin cadena de frío.",
                "facility.refrigeration=false; cooler_items=0",
            ))
        }
        if (input.participants.isEmpty()) {
            signals.add(AwarenessSignal(
                "camping.no_participants", SignalSeverity.INFO, SignalStance.NEUTRAL, SignalConfidence.ALTA,
                "Sin participantes",
                "La salida no tiene participantes. Agregá al menos uno para calcular cantidades.",
                "participants=0",
            ))
        }
        if (!hasActive(items) { it.category == PackingCategory.WATER }) {
            signals.add(AwarenessSignal(
                "camping.no_water", SignalSeverity.ADVERTENCIA, SignalStance.AMENAZA, SignalConfidence.ALTA,
                "Sin agua en la lista", "No hay agua ni bidones en la lista. El agua es esencial.",
                "water_items=0",
            ))
        }
        if (!hasActive(items) { it.key == "safety.first_aid" }) {
            signals.add(AwarenessSignal(
                "camping.no_first_aid", SignalSeverity.ADVERTENCIA, SignalStance.AMENAZA, SignalConfidence.ALTA,
                "Sin botiquín en la lista",
                "No hay botiquín en la lista. Considerá incluir uno (contenido a definir con criterio propio).",
                "first_aid_items=0",
            ))
        }
        return signals.sortedWith(compareByDescending<AwarenessSignal> { rank(it.severity) }.thenBy { it.code })
    }

    /**
     * AwarenessOS Outdoor v0: señales situacionales ampliadas derivadas EXCLUSIVAMENTE de
     * hechos disponibles (lista + contexto + Trip Gate). Determinístico (timestamp `at`
     * inyectado). No diagnostica ni afirma "seguro". Cada señal lleva regla, acción,
     * hechos y versión.
     *
     * @param previousItems lista anterior para detectar cambio significativo vs plan.
     * @param at timestamp ISO8601 inyectado (sin reloj interno).
     */
    fun evaluateSituational(
        outing: OutdoorOuting,
        items: List<OutingPackingItem>,
        ctx: TripContext,
        previousItems: List<OutingPackingItem>? = null,
        secondary: Set<ScenarioKind> = emptySet(),
        at: String,
    ): List<AwarenessSignal> {
        val gate = TripGate.evaluate(outing, items, ctx, secondary)
        val out = mutableListOf<AwarenessSignal>()
        fun sig(
            code: String, sev: SignalSeverity, conf: SignalConfidence, title: String, detail: String,
            factor: String, ruleId: String, action: String?, stance: SignalStance = SignalStance.AMENAZA,
        ) {
            out += AwarenessSignal(code, sev, stance, conf, title, detail, factor, true, ruleId, action, at, AWARENESS_VERSION)
        }

        // Equipo crítico ausente (de blockers del gate).
        for (b in gate.blockingFindings.filter { it.code.startsWith("gate.missing.") }) {
            val what = b.code.removePrefix("gate.missing.")
            sig("aware.missing_equipment.$what", SignalSeverity.CRITICO, SignalConfidence.ALTA,
                "Equipo crítico ausente: $what", b.detail, b.factor, b.code, b.recommendation)
        }
        // Preparación incompleta (obligatorios sin empacar).
        val summary = PackingPresentation.summary(items)
        if (summary.mandatoryPending > 0) {
            sig("aware.prep_incomplete", SignalSeverity.ADVERTENCIA, SignalConfidence.ALTA,
                "Preparación incompleta", "${summary.mandatoryPending} ítem(s) obligatorio(s) sin empacar.",
                "mandatory_pending=${summary.mandatoryPending}", "rule.prep.mandatory_pending", "Empacar los obligatorios pendientes.")
        }
        // Meteo (vencida/ausente), batería, comms, capacidad del grupo (de warnings del gate).
        for (w in gate.warnings) {
            when (w.code) {
                "gate.weather.none", "gate.weather.stale" ->
                    sig("aware.${w.code.removePrefix("gate.")}", SignalSeverity.ADVERTENCIA, SignalConfidence.MEDIA, w.title, w.detail, w.factor, w.code, w.recommendation)
                "gate.battery.low" ->
                    sig("aware.battery_low", SignalSeverity.ADVERTENCIA, SignalConfidence.ALTA, w.title, w.detail, w.factor, w.code, w.recommendation)
                "gate.comms.none" ->
                    sig("aware.comms_insufficient", SignalSeverity.ADVERTENCIA, SignalConfidence.ALTA, w.title, w.detail, w.factor, w.code, w.recommendation)
                "gate.experience.gap" ->
                    sig("aware.group_capacity", SignalSeverity.ADVERTENCIA, SignalConfidence.MEDIA, "Capacidad del grupo vs salida", w.detail, w.factor, w.code, w.recommendation)
            }
        }
        // Menores con brecha de abrigo.
        val youngChildren = outing.input.participants.count { it.ageClass.name == "INFANT" || it.ageClass.name == "CHILD" }
        if (youngChildren > 0 && !items.any { !it.removed && it.quantity > 0 && it.key == "clothing.warm_child" }) {
            sig("aware.minors_gap", SignalSeverity.ADVERTENCIA, SignalConfidence.ALTA,
                "Menores con brecha de abrigo", "$youngChildren niño(s) sin abrigo de niño registrado.",
                "young_children=$youngChildren; child_warmth=0", "rule.minors.warmth", "Agregar abrigo por niño.")
        }
        // Exposición acumulada (>=3 condiciones adversas).
        val c = ctx.conditions
        val exposure = listOf(c.cold, c.rain, c.wind, c.snow).count { it }
        if (exposure >= 3) {
            sig("aware.exposure_accumulated", SignalSeverity.ADVERTENCIA, SignalConfidence.MEDIA,
                "Exposición acumulada", "$exposure condiciones adversas combinadas.",
                "adverse_conditions=$exposure", "rule.exposure.accumulated", "Reevaluar fecha, ruta o equipo.")
        }
        // Hora de retorno ausente en salidas no triviales.
        if (gate.complexityClass != ComplexityClass.SIMPLE && ctx.hardTurnaround == null) {
            sig("aware.no_turnaround", SignalSeverity.ADVERTENCIA, SignalConfidence.MEDIA,
                "Sin hora de retorno definida", "Salida ${gate.complexityClass} sin hora dura de retorno.",
                "turnaround=null; class=${gate.complexityClass}", "rule.return.turnaround", "Definir hora límite de retorno.")
        }
        // Cambio significativo respecto del plan original.
        if (previousItems != null) {
            val prev = previousItems.associate { it.key to it.quantity }
            val curr = items.associate { it.key to it.quantity }
            val changed = (prev.keys - curr.keys).size + (curr.keys - prev.keys).size +
                curr.count { (k, q) -> prev[k] != null && prev[k] != q }
            if (changed > 0) {
                sig("aware.plan_changed", SignalSeverity.INFO, SignalConfidence.ALTA,
                    "Cambio respecto del plan original", "$changed diferencia(s) en la lista respecto del plan previo.",
                    "diff_count=$changed", "rule.plan.changed", "Revisar que los cambios sean intencionales.",
                    stance = SignalStance.NEUTRAL)
            }
        }
        // Acumulación de demasiadas advertencias.
        val warnCount = out.count { it.severity == SignalSeverity.ADVERTENCIA }
        if (warnCount >= 4) {
            sig("aware.too_many_warnings", SignalSeverity.ADVERTENCIA, SignalConfidence.ALTA,
                "Acumulación de advertencias", "$warnCount advertencias simultáneas: reconsiderar el plan.",
                "warning_count=$warnCount", "rule.accumulation.warnings", "Reevaluar alcance, fecha o equipo.")
        }

        return out.sortedWith(compareByDescending<AwarenessSignal> { rank(it.severity) }.thenBy { it.code })
    }
}
