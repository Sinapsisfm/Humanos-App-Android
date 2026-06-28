package eco.humanos.android.core.outdoor

import com.google.common.truth.Truth.assertThat
import eco.humanos.android.core.outdoor.awareness.AwarenessEvaluator
import eco.humanos.android.core.outdoor.domain.AgeClass
import eco.humanos.android.core.outdoor.domain.ParticipantRole
import eco.humanos.android.core.outdoor.packing.PackingEngine
import org.junit.Test

class AwarenessEvaluatorTest {

    @Test fun `plan completo no dispara faltantes basicos`() {
        val input = Fixtures.packingInput()
        val plan = PackingEngine.buildCampingPlan(input)
        val codes = AwarenessEvaluator.evaluate(Fixtures.outing(input), plan.items).map { it.code }
        assertThat(codes).doesNotContain("camping.no_lighting")
        assertThat(codes).doesNotContain("camping.no_water")
        assertThat(codes).doesNotContain("camping.no_first_aid")
    }

    @Test fun `noches sin iluminacion dispara senal`() {
        val input = Fixtures.packingInput()
        val plan = PackingEngine.buildCampingPlan(input)
        val noLight = PackingEngine.applyUserEdits(plan.items, listOf(
            PackingEngine.UserEdit.Remove("lighting.lantern"),
            PackingEngine.UserEdit.Remove("lighting.headlamp"),
        ))
        val s = AwarenessEvaluator.evaluate(Fixtures.outing(input), noLight).firstOrNull { it.code == "camping.no_lighting" }
        assertThat(s).isNotNull()
        assertThat(s!!.dismissible).isTrue()
        assertThat(s.sourceFactor).contains("nights=")
    }

    @Test fun `ninos sin abrigo dispara senal`() {
        val input = Fixtures.packingInput(participants = listOf(
            Fixtures.participant("a1", AgeClass.ADULT),
            Fixtures.participant("c1", AgeClass.CHILD, ParticipantRole.MINOR),
        ))
        val plan = PackingEngine.buildCampingPlan(input)
        val noWarmth = PackingEngine.applyUserEdits(plan.items, listOf(PackingEngine.UserEdit.Remove("clothing.warm_child")))
        val codes = AwarenessEvaluator.evaluate(Fixtures.outing(input), noWarmth).map { it.code }
        assertThat(codes).contains("camping.children_no_warmth")
    }

    @Test fun `refrigeracion requerida sin frio dispara senal`() {
        val input = Fixtures.packingInput(facility = Fixtures.facility(refrigeration = false))
        val plan = PackingEngine.buildCampingPlan(input)
        val noCooler = PackingEngine.applyUserEdits(plan.items, listOf(PackingEngine.UserEdit.Remove("food.cooler")))
        val codes = AwarenessEvaluator.evaluate(Fixtures.outing(input), noCooler).map { it.code }
        assertThat(codes).contains("camping.no_cold_source")
    }

    @Test fun `ninguna senal declara o garantiza seguridad (SCN-007) sobre varios avisos`() {
        val input = Fixtures.packingInput(participants = emptyList())
        val signals = AwarenessEvaluator.evaluate(Fixtures.outing(input), emptyList())
        assertThat(signals.size).isAtLeast(3)
        val banned = Regex("seguro|segura|a salvo|garantiza|certific", RegexOption.IGNORE_CASE)
        for (s in signals) {
            assertThat(banned.containsMatchIn("${s.title} ${s.detail}")).isFalse()
        }
    }

    @Test fun `es deterministico`() {
        val input = Fixtures.packingInput()
        val plan = PackingEngine.buildCampingPlan(input)
        val a = AwarenessEvaluator.evaluate(Fixtures.outing(input), plan.items)
        val b = AwarenessEvaluator.evaluate(Fixtures.outing(input), plan.items)
        assertThat(a).isEqualTo(b)
    }
}
