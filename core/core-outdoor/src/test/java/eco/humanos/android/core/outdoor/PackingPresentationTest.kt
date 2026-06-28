package eco.humanos.android.core.outdoor

import com.google.common.truth.Truth.assertThat
import eco.humanos.android.core.outdoor.packing.PackingEngine
import eco.humanos.android.core.outdoor.presentation.PackingPresentation
import org.junit.Test

class PackingPresentationTest {

    private fun items() = PackingEngine.buildCampingPlan(Fixtures.packingInput()).items

    @Test fun `groupByCategory excluye removidos y mantiene categorias`() {
        val edited = PackingEngine.applyUserEdits(items(), listOf(PackingEngine.UserEdit.Remove("sun_rain.sun")))
        val groups = PackingPresentation.groupByCategory(edited)
        assertThat(groups).isNotEmpty()
        val allKeys = groups.flatMap { g -> g.items.map { it.key } }
        assertThat(allKeys).doesNotContain("sun_rain.sun")
        // sin categorías vacías
        assertThat(groups.all { it.items.isNotEmpty() }).isTrue()
    }

    @Test fun `pendingPurchases excluye comprados`() {
        val edited = PackingEngine.applyUserEdits(items(), listOf(PackingEngine.UserEdit.SetBought("food.rations", true)))
        val pending = PackingPresentation.pendingPurchases(edited)
        assertThat(pending.any { it.key == "food.rations" }).isFalse()
    }

    @Test fun `progress cuenta empacados sobre activos`() {
        val base = items()
        val p0 = PackingPresentation.progress(base)
        assertThat(p0.packed).isEqualTo(0)
        assertThat(p0.total).isEqualTo(PackingPresentation.active(base).size)
        val edited = PackingEngine.applyUserEdits(base, listOf(PackingEngine.UserEdit.SetPacked("safety.first_aid", true)))
        assertThat(PackingPresentation.progress(edited).packed).isEqualTo(1)
    }

    @Test fun `summary - obligatorios pendientes baja al empacar`() {
        val base = items()
        val s0 = PackingPresentation.summary(base)
        assertThat(s0.mandatory).isGreaterThan(0)
        assertThat(s0.mandatoryPending).isEqualTo(s0.mandatory)
        val edited = PackingEngine.applyUserEdits(base, listOf(PackingEngine.UserEdit.SetPacked("safety.first_aid", true)))
        assertThat(PackingPresentation.summary(edited).mandatoryPending).isEqualTo(s0.mandatory - 1)
    }

    @Test fun `byResponsible agrupa sin asignar bajo null`() {
        val edited = PackingEngine.applyUserEdits(items(), listOf(
            PackingEngine.UserEdit.SetResponsible("safety.first_aid", "a1"),
        ))
        val byResp = PackingPresentation.byResponsible(edited)
        assertThat(byResp["a1"]!!.any { it.key == "safety.first_aid" }).isTrue()
        assertThat(byResp.containsKey(null)).isTrue()
    }
}
