package eco.humanos.android.core.outdoor

import com.google.common.truth.Truth.assertThat
import eco.humanos.android.core.outdoor.domain.Season
import eco.humanos.android.core.outdoor.packing.PackingEngine
import org.junit.Test

class SeasonRulesTest {

    private fun keys(season: Season) =
        PackingEngine.generateItems(Fixtures.packingInput(season = season)).map { it.key }

    @Test fun `verano no activa la capa fria`() {
        val k = keys(Season.VERANO)
        assertThat(k).doesNotContain("winter.waterproof_shell")
        assertThat(k).doesNotContain("winter.gloves_hat")
        assertThat(k).doesNotContain("winter.insulation")
    }

    @Test fun `invierno activa toda la capa fria`() {
        val k = keys(Season.INVIERNO)
        assertThat(k).containsAtLeast(
            "winter.waterproof_shell", "winter.gloves_hat", "winter.insulation", "winter.traction",
        )
    }

    @Test fun `otono activa impermeable pero no items exclusivos de invierno`() {
        val k = keys(Season.OTONO)
        assertThat(k).contains("winter.waterproof_shell") // coldSeason
        assertThat(k).doesNotContain("winter.gloves_hat") // solo invierno
        assertThat(k).doesNotContain("winter.insulation") // solo invierno
    }

    @Test fun `cantidad de shell impermeable = personas`() {
        val items = PackingEngine.generateItems(Fixtures.packingInput(season = Season.INVIERNO))
        val shell = items.first { it.key == "winter.waterproof_shell" }
        assertThat(shell.quantity).isEqualTo(Fixtures.packingInput().participants.size)
    }

    @Test fun `capa fria es determinística`() {
        val a = PackingEngine.generateItems(Fixtures.packingInput(season = Season.INVIERNO))
        val b = PackingEngine.generateItems(Fixtures.packingInput(season = Season.INVIERNO))
        assertThat(a).isEqualTo(b)
    }
}
