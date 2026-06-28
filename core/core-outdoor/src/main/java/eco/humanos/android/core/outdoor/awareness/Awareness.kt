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
    /** Factor que originó la señal (trazabilidad). */
    val sourceFactor: String,
    /** Siempre descartable. */
    val dismissible: Boolean = true,
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
}
