package eco.humanos.android.core.outdoor

import com.google.common.truth.Truth.assertThat
import eco.humanos.android.core.outdoor.awareness.AwarenessEvaluator
import eco.humanos.android.core.outdoor.domain.AccessMode
import eco.humanos.android.core.outdoor.domain.Accommodation
import eco.humanos.android.core.outdoor.domain.AgeClass
import eco.humanos.android.core.outdoor.domain.Availability
import eco.humanos.android.core.outdoor.domain.OutdoorParticipant
import eco.humanos.android.core.outdoor.domain.ParticipantRole
import eco.humanos.android.core.outdoor.domain.Season
import eco.humanos.android.core.outdoor.packing.PackingEngine
import org.junit.Test

/**
 * Barrido sobre el espacio de entradas: verifica INVARIANTES globales del engine y del
 * evaluador en ~miles de combinaciones (determinismo, cantidades válidas, trazabilidad,
 * no-duplicación, señales descartables y sin lenguaje de "seguridad").
 */
class RobustnessTest {

    private val banned = Regex("seguro|segura|a salvo|garantiza|certific", RegexOption.IGNORE_CASE)

    private fun participantSets(): List<List<OutdoorParticipant>> = listOf(
        emptyList(),
        listOf(OutdoorParticipant("a1", "A1", ParticipantRole.ADULT, AgeClass.ADULT)),
        listOf(
            OutdoorParticipant("a1", "A1", ParticipantRole.ADULT, AgeClass.ADULT),
            OutdoorParticipant("c1", "C1", ParticipantRole.MINOR, AgeClass.CHILD),
        ),
        listOf(
            OutdoorParticipant("a1", "A1", ParticipantRole.ADULT, AgeClass.ADULT),
            OutdoorParticipant("a2", "A2", ParticipantRole.ADULT, AgeClass.ADULT),
            OutdoorParticipant("t1", "T1", ParticipantRole.MINOR, AgeClass.TEEN),
            OutdoorParticipant("i1", "I1", ParticipantRole.MINOR, AgeClass.INFANT),
        ),
    )

    @Test fun `invariantes del engine y awareness en barrido amplio`() {
        var combos = 0
        for (season in Season.entries) {
            for (nights in listOf(0, 1, 3, 7, 12)) {
                for (acc in Accommodation.entries) {
                    for (potable in listOf(true, false)) {
                        for (fridge in listOf(true, false)) {
                            for (parts in participantSets()) {
                                combos++
                                val input = Fixtures.packingInput(
                                    nights = nights,
                                    participants = parts,
                                    season = season,
                                    facility = Fixtures.facility(
                                        accommodation = acc,
                                        access = AccessMode.VEHICLE,
                                        potableWater = potable,
                                        refrigeration = fridge,
                                        electricity = Availability.NONE,
                                    ),
                                )
                                val items = PackingEngine.generateItems(input)
                                // determinismo
                                assertThat(PackingEngine.generateItems(input)).isEqualTo(items)
                                // no-duplicación por key
                                val keys = items.map { it.key }
                                assertThat(keys).containsNoDuplicates()
                                // cantidades válidas + trazabilidad
                                for (it in items) {
                                    assertThat(it.quantity).isAtLeast(1)
                                    assertThat(it.sourceRuleId).isNotEmpty()
                                    assertThat(it.explanation).isNotEmpty()
                                }
                                // awareness: descartables y sin lenguaje de "seguridad"
                                val plan = PackingEngine.buildCampingPlan(input)
                                val outing = Fixtures.outing(input)
                                for (s in AwarenessEvaluator.evaluate(outing, plan.items)) {
                                    assertThat(s.dismissible).isTrue()
                                    assertThat(banned.containsMatchIn("${s.title} ${s.detail}")).isFalse()
                                }
                            }
                        }
                    }
                }
            }
        }
        assertThat(combos).isAtLeast(1000)
    }
}
