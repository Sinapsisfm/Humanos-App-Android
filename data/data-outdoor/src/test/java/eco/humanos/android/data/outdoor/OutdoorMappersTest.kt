package eco.humanos.android.data.outdoor

import com.google.common.truth.Truth.assertThat
import eco.humanos.android.core.outdoor.domain.AccessMode
import eco.humanos.android.core.outdoor.domain.Accommodation
import eco.humanos.android.core.outdoor.domain.AgeClass
import eco.humanos.android.core.outdoor.domain.Availability
import eco.humanos.android.core.outdoor.domain.DietaryRestriction
import eco.humanos.android.core.outdoor.domain.FacilityProfile
import eco.humanos.android.core.outdoor.domain.Intent
import eco.humanos.android.core.outdoor.domain.OutdoorOuting
import eco.humanos.android.core.outdoor.domain.OutdoorParticipant
import eco.humanos.android.core.outdoor.domain.OutingEvent
import eco.humanos.android.core.outdoor.domain.OutingEventType
import eco.humanos.android.core.outdoor.domain.OutingStatus
import eco.humanos.android.core.outdoor.domain.PackingInput
import eco.humanos.android.core.outdoor.domain.ParticipantRole
import eco.humanos.android.core.outdoor.domain.ScenarioKind
import eco.humanos.android.core.outdoor.domain.Season
import eco.humanos.android.core.outdoor.domain.SourceType
import eco.humanos.android.core.outdoor.packing.PackingEngine
import org.junit.Test

/** Mappers entidad↔dominio: round-trip exacto (JVM puro, sin runtime de Room). */
class OutdoorMappersTest {

    private fun input() = PackingInput(
        scenario = ScenarioKind.CAMPING, season = Season.INVIERNO, territory = "CL-AR", nights = 3,
        participants = listOf(
            OutdoorParticipant("a1", "A1", ParticipantRole.LEAD, AgeClass.ADULT),
            OutdoorParticipant("c1", "C1", ParticipantRole.MINOR, AgeClass.CHILD),
        ),
        facility = FacilityProfile(
            Accommodation.TENT, AccessMode.VEHICLE, Availability.LIMITED, false,
            Availability.NONE, false, false, false, Availability.LIMITED,
        ),
        dietary = listOf(DietaryRestriction("d1", "sin gluten")),
    )

    private fun outing() = OutdoorOuting(
        id = "o1", title = "Camping", intent = Intent.CAMPING_VACACIONES, scenario = ScenarioKind.CAMPING,
        status = OutingStatus.DRAFT, rulesetVersion = "camping-cl.v0.2.0", input = input(),
        createdAt = "2026-06-27T10:00:00.000Z", updatedAt = "2026-06-27T10:00:00.000Z",
    )

    @Test fun `outing round-trip exacto`() {
        val o = outing()
        assertThat(o.toEntity().toDomain()).isEqualTo(o)
    }

    @Test fun `plan round-trip exacto`() {
        val plan = PackingEngine.buildCampingPlan(input())
        assertThat(plan.toEntity("o1").toDomain()).isEqualTo(plan)
    }

    @Test fun `event round-trip exacto incluye payload`() {
        val e = OutingEvent("e1", "o1", OutingEventType.PACKING_GENERATED, SourceType.DETERMINISTIC_INFERENCE,
            "2026-06-27T10:01:00.000Z", mapOf("k" to "v", "n" to "2"))
        assertThat(e.toEntity().toDomain()).isEqualTo(e)
    }

    @Test fun `dietary sensible sobrevive el round-trip de la entidad (cifrar en reposo es responsabilidad del store)`() {
        val o = outing()
        val restored = o.toEntity().toDomain()
        assertThat(restored.input.dietary).hasSize(1)
        assertThat(restored.input.dietary[0].label).isEqualTo("sin gluten")
    }
}
