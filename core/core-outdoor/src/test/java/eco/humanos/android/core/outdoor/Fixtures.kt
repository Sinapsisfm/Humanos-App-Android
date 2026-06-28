package eco.humanos.android.core.outdoor

import eco.humanos.android.core.outdoor.domain.AccessMode
import eco.humanos.android.core.outdoor.domain.Accommodation
import eco.humanos.android.core.outdoor.domain.AgeClass
import eco.humanos.android.core.outdoor.domain.Availability
import eco.humanos.android.core.outdoor.domain.FacilityProfile
import eco.humanos.android.core.outdoor.domain.Intent
import eco.humanos.android.core.outdoor.domain.OutdoorOuting
import eco.humanos.android.core.outdoor.domain.OutdoorParticipant
import eco.humanos.android.core.outdoor.domain.OutingStatus
import eco.humanos.android.core.outdoor.domain.PackingInput
import eco.humanos.android.core.outdoor.domain.ParticipantRole
import eco.humanos.android.core.outdoor.domain.ScenarioKind
import eco.humanos.android.core.outdoor.domain.Season

/** Fixtures sintéticos NO sensibles (sin datos personales reales). */
object Fixtures {
    fun participant(id: String, ageClass: AgeClass, role: ParticipantRole = ParticipantRole.ADULT) =
        OutdoorParticipant(id = id, displayName = "P-$id", role = role, ageClass = ageClass)

    fun facility(
        accommodation: Accommodation = Accommodation.TENT,
        access: AccessMode = AccessMode.VEHICLE,
        water: Availability = Availability.AVAILABLE,
        potableWater: Boolean = true,
        electricity: Availability = Availability.NONE,
        toilets: Boolean = true,
        showers: Boolean = true,
        refrigeration: Boolean = false,
        nearbyCommerce: Availability = Availability.LIMITED,
    ) = FacilityProfile(accommodation, access, water, potableWater, electricity, toilets, showers, refrigeration, nearbyCommerce)

    fun packingInput(
        nights: Int = 3,
        participants: List<OutdoorParticipant> = listOf(
            participant("a1", AgeClass.ADULT),
            participant("a2", AgeClass.ADULT),
            participant("c1", AgeClass.CHILD, ParticipantRole.MINOR),
        ),
        facility: FacilityProfile = facility(),
        season: Season = Season.VERANO,
        dietary: List<eco.humanos.android.core.outdoor.domain.DietaryRestriction> = emptyList(),
    ) = PackingInput(
        scenario = ScenarioKind.CAMPING, season = season, territory = "CL-AR",
        nights = nights, participants = participants, facility = facility, dietary = dietary,
    )

    fun outing(input: PackingInput = packingInput()) = OutdoorOuting(
        id = "outing-1", title = "Camping de prueba", intent = Intent.CAMPING_VACACIONES,
        scenario = ScenarioKind.CAMPING, status = OutingStatus.DRAFT, rulesetVersion = "camping-cl.v0.1.0",
        input = input, createdAt = "2026-06-27T10:00:00.000Z", updatedAt = "2026-06-27T10:00:00.000Z",
    )
}
