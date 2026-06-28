package eco.humanos.android.core.outdoor

import com.google.common.truth.Truth.assertThat
import eco.humanos.android.core.outdoor.domain.Accommodation
import eco.humanos.android.core.outdoor.domain.ScenarioKind
import eco.humanos.android.core.outdoor.packing.PackingEngine
import eco.humanos.android.core.outdoor.packing.ReturnListGenerator
import eco.humanos.android.core.outdoor.packing.rulesFor
import org.junit.Test

class ScenariosAndReturnTest {

    @Test fun `composicion camping + trekking agrega trekking sin duplicar compartidos`() {
        val input = Fixtures.packingInput()
        val rules = rulesFor(ScenarioKind.CAMPING, setOf(ScenarioKind.TREKKING))
        val items = PackingEngine.generateItems(input, rules)
        val keys = items.map { it.key }
        // ítems de trekking presentes
        assertThat(keys).contains("trekking.backpack")
        assertThat(keys).contains("trekking.navigation")
        // compartidos NO duplicados
        assertThat(keys.count { it == "safety.first_aid" }).isEqualTo(1)
        assertThat(keys.count { it == "water.treatment" }).isAtMost(1)
    }

    @Test fun `camping solo no incluye items de trekking`() {
        val items = PackingEngine.generateItems(Fixtures.packingInput(), rulesFor(ScenarioKind.CAMPING))
        assertThat(items.map { it.key }).doesNotContain("trekking.backpack")
    }

    @Test fun `composicion es determinística`() {
        val input = Fixtures.packingInput()
        val rules = rulesFor(ScenarioKind.CAMPING, setOf(ScenarioKind.TREKKING))
        assertThat(PackingEngine.generateItems(input, rules)).isEqualTo(PackingEngine.generateItems(input, rules))
    }

    @Test fun `lista de retorno cubre residuos, inventario y sitio`() {
        val tasks = ReturnListGenerator.build(Fixtures.packingInput())
        val ids = tasks.map { it.id }
        assertThat(ids).containsAtLeast("return.waste", "return.inventory", "return.site")
        // carpa → desmontaje
        assertThat(ids).contains("return.teardown")
    }

    @Test fun `cabaña sin carpa no genera desmontaje de carpa`() {
        val input = Fixtures.packingInput(facility = Fixtures.facility(accommodation = Accommodation.CABIN))
        val ids = ReturnListGenerator.build(input).map { it.id }
        assertThat(ids).doesNotContain("return.teardown")
    }
}
