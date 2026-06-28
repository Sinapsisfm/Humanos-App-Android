package eco.humanos.android.core.outdoor

import com.google.common.truth.Truth.assertThat
import eco.humanos.android.core.outdoor.domain.ScenarioKind
import eco.humanos.android.core.outdoor.gate.ComplexityClass
import eco.humanos.android.core.outdoor.gate.FindingSeverity
import eco.humanos.android.core.outdoor.gate.GroupExperience
import eco.humanos.android.core.outdoor.gate.IsolationLevel
import eco.humanos.android.core.outdoor.gate.ReadinessState
import eco.humanos.android.core.outdoor.gate.TRIP_GATE_VERSION
import eco.humanos.android.core.outdoor.gate.TripConditions
import eco.humanos.android.core.outdoor.gate.TripContext
import eco.humanos.android.core.outdoor.gate.TripGate
import eco.humanos.android.core.outdoor.gate.TripGateResult
import eco.humanos.android.core.outdoor.packing.PackingEngine
import eco.humanos.android.core.outdoor.packing.rulesFor
import kotlinx.serialization.json.Json
import org.junit.Test

class TripGateTest {

    private fun campingItems(season: eco.humanos.android.core.outdoor.domain.Season = eco.humanos.android.core.outdoor.domain.Season.VERANO) =
        PackingEngine.buildCampingPlan(Fixtures.packingInput(season = season)).items

    private fun trekkingItems() =
        PackingEngine.buildCampingPlan(Fixtures.packingInput(), rulesFor(ScenarioKind.CAMPING, setOf(ScenarioKind.TREKKING))).items

    @Test fun `camping familiar simple es SIMPLE y no alta montaña`() {
        val r = TripGate.evaluate(Fixtures.outing(), campingItems(),
            TripContext(experience = GroupExperience.INTERMEDIATE))
        assertThat(r.complexityClass).isEqualTo(ComplexityClass.SIMPLE)
        assertThat(r.blockingFindings).isEmpty()
        assertThat(r.readinessState).isEqualTo(ReadinessState.GREEN)
    }

    @Test fun `altitud sola no activa alta montaña`() {
        val r = TripGate.evaluate(
            Fixtures.outing(), campingItems(),
            TripContext(conditions = TripConditions(altitudeMeters = 3000), experience = GroupExperience.INTERMEDIATE),
        )
        assertThat(r.complexityClass).isNotEqualTo(ComplexityClass.HIGH_MOUNTAIN)
        assertThat(r.complexityFactors.any { it.contains("no activa alta montaña") }).isTrue()
    }

    @Test fun `nieve fuerza al menos ADVANCED y exige navegacion`() {
        val r = TripGate.evaluate(
            Fixtures.outing(), campingItems(),
            TripContext(conditions = TripConditions(snow = true), experience = GroupExperience.EXPERIENCED, weatherDataAgeHours = 1),
        )
        assertThat(r.complexityClass).isAnyOf(ComplexityClass.ADVANCED, ComplexityClass.HIGH_MOUNTAIN)
        // navegación requerida para ADVANCED y ausente en el plan de camping
        assertThat(r.blockingFindings.any { it.code == "gate.missing.navigation" }).isTrue()
        assertThat(r.readinessState).isEqualTo(ReadinessState.RED)
    }

    @Test fun `menores mas frio suben la complejidad`() {
        val r = TripGate.evaluate(
            Fixtures.outing(), campingItems(),
            TripContext(conditions = TripConditions(cold = true), experience = GroupExperience.INTERMEDIATE),
        )
        // base camping SIMPLE + (menores + frío) → al menos MODERATE
        assertThat(r.complexityClass).isAnyOf(ComplexityClass.MODERATE, ComplexityClass.ADVANCED)
        assertThat(r.complexityFactors.any { it.contains("menores") }).isTrue()
    }

    @Test fun `retorno antes del inicio es blocker`() {
        val r = TripGate.evaluate(
            Fixtures.outing(), trekkingItems(), secondary = setOf(ScenarioKind.TREKKING),
            ctx = TripContext(
                startTime = "2026-07-01T09:00:00Z", hardTurnaround = "2026-07-01T08:00:00Z",
                weatherDataAgeHours = 1, experience = GroupExperience.EXPERIENCED,
            ),
        )
        assertThat(r.blockingFindings.any { it.code == "gate.time.incoherent" }).isTrue()
        assertThat(r.readinessState).isEqualTo(ReadinessState.RED)
    }

    @Test fun `equipo critico faltante (agua) es blocker y RED`() {
        val noWater = PackingEngine.applyUserEdits(campingItems(), listOf(
            PackingEngine.UserEdit.Remove("water.storage"),
        ))
        val r = TripGate.evaluate(Fixtures.outing(), noWater, TripContext(experience = GroupExperience.INTERMEDIATE))
        assertThat(r.blockingFindings.any { it.code == "gate.missing.water" }).isTrue()
        assertThat(r.pendingRequirements).contains("equipo:water")
        assertThat(r.readinessState).isEqualTo(ReadinessState.RED)
    }

    @Test fun `meteo vencida es warning (sin blocker)`() {
        val r = TripGate.evaluate(
            Fixtures.outing(), trekkingItems(), secondary = setOf(ScenarioKind.TREKKING),
            ctx = TripContext(weatherDataAgeHours = 48, experience = GroupExperience.EXPERIENCED),
        )
        assertThat(r.warnings.any { it.code == "gate.weather.stale" }).isTrue()
        assertThat(r.blockingFindings).isEmpty()
        assertThat(r.readinessState).isEqualTo(ReadinessState.YELLOW)
    }

    @Test fun `sin participantes el estado es GRIS`() {
        val outing = Fixtures.outing(Fixtures.packingInput(participants = emptyList()))
        val r = TripGate.evaluate(outing, campingItems(), TripContext(experience = GroupExperience.INTERMEDIATE))
        assertThat(r.readinessState).isEqualTo(ReadinessState.GRAY)
    }

    @Test fun `nunca afirma seguridad en los textos (SCN-007)`() {
        val banned = Regex("seguro|segura|a salvo|garantiza|certific", RegexOption.IGNORE_CASE)
        val r = TripGate.evaluate(Fixtures.outing(), campingItems(),
            TripContext(conditions = TripConditions(snow = true, isolation = IsolationLevel.HIGH)))
        for (f in r.findings) {
            assertThat(banned.containsMatchIn("${f.title} ${f.detail} ${f.recommendation ?: ""}")).isFalse()
        }
    }

    @Test fun `idempotencia`() {
        val ctx = TripContext(conditions = TripConditions(snow = true), experience = GroupExperience.NOVICE)
        assertThat(TripGate.evaluate(Fixtures.outing(), campingItems(), ctx))
            .isEqualTo(TripGate.evaluate(Fixtures.outing(), campingItems(), ctx))
    }

    @Test fun `serializacion y restauracion del resultado`() {
        val json = Json { encodeDefaults = true }
        val r = TripGate.evaluate(Fixtures.outing(), campingItems(),
            TripContext(conditions = TripConditions(cold = true), experience = GroupExperience.INTERMEDIATE))
        val blob = json.encodeToString(TripGateResult.serializer(), r)
        val restored = json.decodeFromString(TripGateResult.serializer(), blob)
        assertThat(restored).isEqualTo(r)
        assertThat(r.rulesetVersion).isEqualTo(TRIP_GATE_VERSION)
    }

    @Test fun `invariantes en barrido amplio`() {
        var combos = 0
        for (scenario in listOf(ScenarioKind.CAMPING, ScenarioKind.TREKKING, ScenarioKind.SNOW_MOUNTAIN)) {
            for (snow in listOf(false, true)) {
                for (cold in listOf(false, true)) {
                    for (iso in IsolationLevel.entries) {
                        for (alt in listOf(null, 1500, 2200, 3000)) {
                            for (exp in GroupExperience.entries) {
                                combos++
                                val outing = Fixtures.outing(Fixtures.packingInput().copy(scenario = scenario))
                                val ctx = TripContext(
                                    conditions = TripConditions(snow = snow, cold = cold, isolation = iso, altitudeMeters = alt),
                                    experience = exp, weatherDataAgeHours = 2,
                                )
                                val r = TripGate.evaluate(outing, campingItems(), ctx)
                                // determinismo
                                assertThat(TripGate.evaluate(outing, campingItems(), ctx)).isEqualTo(r)
                                // blockers ⊆ findings; si hay blockers → RED
                                assertThat(r.findings).containsAtLeastElementsIn(r.blockingFindings)
                                if (r.blockingFindings.isNotEmpty()) {
                                    assertThat(r.readinessState).isEqualTo(ReadinessState.RED)
                                }
                                // altitud sola (sin nieve ni aislamiento alto) nunca da alta montaña
                                if (alt != null && alt < 2500 && !snow && iso != IsolationLevel.HIGH && scenario != ScenarioKind.SNOW_MOUNTAIN) {
                                    assertThat(r.complexityClass).isNotEqualTo(ComplexityClass.HIGH_MOUNTAIN)
                                }
                                assertThat(r.rulesetVersion).isEqualTo(TRIP_GATE_VERSION)
                            }
                        }
                    }
                }
            }
        }
        assertThat(combos).isAtLeast(500)
    }
}
