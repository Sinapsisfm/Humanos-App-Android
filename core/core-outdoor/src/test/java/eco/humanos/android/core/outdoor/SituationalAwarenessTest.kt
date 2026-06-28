package eco.humanos.android.core.outdoor

import com.google.common.truth.Truth.assertThat
import eco.humanos.android.core.outdoor.awareness.AWARENESS_VERSION
import eco.humanos.android.core.outdoor.awareness.AwarenessEvaluator
import eco.humanos.android.core.outdoor.gate.IsolationLevel
import eco.humanos.android.core.outdoor.gate.TripConditions
import eco.humanos.android.core.outdoor.gate.TripContext
import eco.humanos.android.core.outdoor.packing.PackingEngine
import org.junit.Test

class SituationalAwarenessTest {

    private val AT = "2026-06-27T12:00:00.000Z"
    private fun items() = PackingEngine.buildCampingPlan(Fixtures.packingInput()).items

    @Test fun `equipo critico ausente genera senal critica`() {
        val noWater = PackingEngine.applyUserEdits(items(), listOf(PackingEngine.UserEdit.Remove("water.storage")))
        val signals = AwarenessEvaluator.evaluateSituational(Fixtures.outing(), noWater, TripContext(), at = AT)
        val s = signals.firstOrNull { it.code == "aware.missing_equipment.water" }
        assertThat(s).isNotNull()
        assertThat(s!!.severity.name).isEqualTo("CRITICO")
        assertThat(s.at).isEqualTo(AT)
        assertThat(s.rulesetVersion).isEqualTo(AWARENESS_VERSION)
        assertThat(s.ruleId).isNotEmpty()
        assertThat(s.action).isNotNull()
    }

    @Test fun `preparacion incompleta cuando hay obligatorios sin empacar`() {
        val signals = AwarenessEvaluator.evaluateSituational(Fixtures.outing(), items(), TripContext(), at = AT)
        assertThat(signals.map { it.code }).contains("aware.prep_incomplete")
    }

    @Test fun `exposicion acumulada con tres o mas condiciones`() {
        val ctx = TripContext(conditions = TripConditions(cold = true, rain = true, wind = true))
        val signals = AwarenessEvaluator.evaluateSituational(Fixtures.outing(), items(), ctx, at = AT)
        assertThat(signals.map { it.code }).contains("aware.exposure_accumulated")
    }

    @Test fun `sin hora de retorno en salida no trivial`() {
        val ctx = TripContext(conditions = TripConditions(snow = true)) // → ADVANCED
        val signals = AwarenessEvaluator.evaluateSituational(Fixtures.outing(), items(), ctx, at = AT)
        assertThat(signals.map { it.code }).contains("aware.no_turnaround")
    }

    @Test fun `cambio respecto del plan original`() {
        val base = items()
        val edited = PackingEngine.applyUserEdits(base, listOf(
            PackingEngine.UserEdit.AddManual("Guitarra", eco.humanos.android.core.outdoor.domain.PackingCategory.ENTERTAINMENT),
        ))
        val signals = AwarenessEvaluator.evaluateSituational(Fixtures.outing(), edited, TripContext(), previousItems = base, at = AT)
        assertThat(signals.map { it.code }).contains("aware.plan_changed")
    }

    @Test fun `todas descartables, con timestamp y sin lenguaje de seguridad`() {
        val ctx = TripContext(conditions = TripConditions(cold = true, snow = true, isolation = IsolationLevel.HIGH))
        val signals = AwarenessEvaluator.evaluateSituational(Fixtures.outing(), items(), ctx, at = AT)
        val banned = Regex("seguro|segura|a salvo|garantiza|certific", RegexOption.IGNORE_CASE)
        assertThat(signals).isNotEmpty()
        for (s in signals) {
            assertThat(s.dismissible).isTrue()
            assertThat(s.at).isEqualTo(AT)
            assertThat(banned.containsMatchIn("${s.title} ${s.detail} ${s.action ?: ""}")).isFalse()
        }
    }

    @Test fun `es determinístico con el mismo timestamp`() {
        val ctx = TripContext(conditions = TripConditions(snow = true))
        val a = AwarenessEvaluator.evaluateSituational(Fixtures.outing(), items(), ctx, at = AT)
        val b = AwarenessEvaluator.evaluateSituational(Fixtures.outing(), items(), ctx, at = AT)
        assertThat(a).isEqualTo(b)
    }
}
