package eco.humanos.android.core.outdoor

import com.google.common.truth.Truth.assertThat
import eco.humanos.android.core.outdoor.domain.AgeClass
import eco.humanos.android.core.outdoor.domain.ItemClassification
import eco.humanos.android.core.outdoor.domain.PackingCategory
import eco.humanos.android.core.outdoor.packing.CAMPING_RULES
import eco.humanos.android.core.outdoor.packing.PackingEngine
import eco.humanos.android.core.outdoor.packing.PackingRule
import eco.humanos.android.core.outdoor.packing.RULESET_VERSION
import org.junit.Test

class PackingEngineTest {

    @Test fun `determinismo - misma entrada misma salida`() {
        val input = Fixtures.packingInput()
        assertThat(PackingEngine.generateItems(input)).isEqualTo(PackingEngine.generateItems(input))
    }

    @Test fun `version de ruleset estable`() {
        assertThat(RULESET_VERSION).isEqualTo("camping-cl.v0.1.0")
        assertThat(PackingEngine.buildCampingPlan(Fixtures.packingInput()).rulesetVersion).isEqualTo(RULESET_VERSION)
    }

    @Test fun `trazabilidad - cada item con sourceRuleId y explicacion`() {
        for (item in PackingEngine.generateItems(Fixtures.packingInput())) {
            assertThat(item.sourceRuleId).isNotEmpty()
            assertThat(item.explanation).isNotEmpty()
        }
    }

    @Test fun `cantidades - raciones = personas x dias (1-2-5 personas, 1-3-10 dias)`() {
        fun rations(people: Int, nights: Int): Int {
            val parts = (0 until people).map { Fixtures.participant("p$it", AgeClass.ADULT) }
            val items = PackingEngine.generateItems(Fixtures.packingInput(nights = nights, participants = parts))
            return items.first { it.key == "food.rations" }.quantity
        }
        assertThat(rations(1, 0)).isEqualTo(1)
        assertThat(rations(2, 2)).isEqualTo(6)
        assertThat(rations(5, 9)).isEqualTo(50)
    }

    @Test fun `no duplicacion - dos reglas misma key se fusionan`() {
        val dup = PackingRule(
            id = "safety.first_aid", category = PackingCategory.SAFETY,
            classification = ItemClassification.RECOMMENDED, name = "Botiquín (trekking)", unit = "botiquín",
            applies = { true }, quantity = { 1 }, explain = { "trekking" },
        )
        val items = PackingEngine.generateItems(Fixtures.packingInput(), CAMPING_RULES + dup)
        val firstAid = items.filter { it.key == "safety.first_aid" }
        assertThat(firstAid).hasSize(1)
        assertThat(firstAid[0].classification).isEqualTo(ItemClassification.MANDATORY)
    }

    @Test fun `fusion coherente y order-independent`() {
        val bigKit = PackingRule(
            id = "safety.first_aid", category = PackingCategory.SAFETY,
            classification = ItemClassification.MANDATORY, name = "Botiquín grande", unit = "kit-grande",
            applies = { true }, quantity = { 3 }, explain = { "Botiquín ampliado por grupo numeroso." },
        )
        val input = Fixtures.packingInput()
        val ab = PackingEngine.generateItems(input, CAMPING_RULES + bigKit).first { it.key == "safety.first_aid" }
        val ba = PackingEngine.generateItems(input, listOf(bigKit) + CAMPING_RULES).first { it.key == "safety.first_aid" }
        assertThat(ab).isEqualTo(ba)
        assertThat(ab.quantity).isEqualTo(3)
        assertThat(ab.unit).isEqualTo("kit-grande")
        assertThat(ab.explanation).isEqualTo("Botiquín ampliado por grupo numeroso.")
    }

    @Test fun `sin agua potable agrega potabilizacion y sin refrigeracion agrega cooler`() {
        val noPotable = PackingEngine.generateItems(Fixtures.packingInput(facility = Fixtures.facility(potableWater = false)))
        assertThat(noPotable.any { it.key == "water.treatment" }).isTrue()
        val noFridge = PackingEngine.generateItems(Fixtures.packingInput(facility = Fixtures.facility(refrigeration = false)))
        assertThat(noFridge.any { it.key == "food.cooler" }).isTrue()
        val withFridge = PackingEngine.generateItems(Fixtures.packingInput(facility = Fixtures.facility(refrigeration = true)))
        assertThat(withFridge.any { it.key == "food.cooler" }).isFalse()
    }

    @Test fun `diff - cambiar noches marca raciones como changed`() {
        val prev = PackingEngine.generateItems(Fixtures.packingInput(nights = 3))
        val next = PackingEngine.generateItems(Fixtures.packingInput(nights = 7))
        val diff = PackingEngine.computeDiff(prev, next)
        assertThat(diff.changed.map { it.key }).contains("food.rations")
    }

    @Test fun `edicion humana - remove soft-delete, add manual, comprar no elimina`() {
        val plan = PackingEngine.buildCampingPlan(Fixtures.packingInput())
        val edited = PackingEngine.applyUserEdits(plan.items, listOf(
            PackingEngine.UserEdit.Remove("sun_rain.sun", "ya tengo"),
            PackingEngine.UserEdit.AddManual("Guitarra", PackingCategory.ENTERTAINMENT),
            PackingEngine.UserEdit.SetBought("food.rations", true),
        ))
        val sun = edited.first { it.key == "sun_rain.sun" }
        assertThat(sun.removed).isTrue()
        assertThat(sun.removalReason).isEqualTo("ya tengo")
        val manual = edited.first { it.name == "Guitarra" }
        assertThat(manual.origin.name).isEqualTo("MANUAL")
        val rations = edited.first { it.key == "food.rations" }
        assertThat(rations.bought).isTrue()
        assertThat(rations.removed).isFalse()
    }
}
