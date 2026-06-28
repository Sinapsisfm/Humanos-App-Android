package eco.humanos.android.feature.outdoor

import com.google.common.truth.Truth.assertThat
import eco.humanos.android.core.outdoor.domain.AccessMode
import eco.humanos.android.core.outdoor.domain.Accommodation
import eco.humanos.android.core.outdoor.domain.AgeClass
import eco.humanos.android.core.outdoor.domain.Availability
import eco.humanos.android.core.outdoor.domain.FacilityProfile
import eco.humanos.android.core.outdoor.domain.OutdoorParticipant
import eco.humanos.android.core.outdoor.domain.PackingInput
import eco.humanos.android.core.outdoor.domain.ParticipantRole
import eco.humanos.android.core.outdoor.domain.ScenarioKind
import eco.humanos.android.core.outdoor.domain.Season
import eco.humanos.android.core.outdoor.repository.InMemoryOutdoorRepository
import eco.humanos.android.core.outdoor.service.Clock
import eco.humanos.android.core.outdoor.service.OutdoorService
import org.junit.Test

class OutdoorPackingViewModelTest {

    private fun input() = PackingInput(
        scenario = ScenarioKind.CAMPING,
        season = Season.VERANO,
        territory = "CL-AR",
        nights = 3,
        participants = listOf(
            OutdoorParticipant("a1", "Adulto 1", ParticipantRole.ADULT, AgeClass.ADULT),
            OutdoorParticipant("c1", "Niño 1", ParticipantRole.MINOR, AgeClass.CHILD),
        ),
        facility = FacilityProfile(
            Accommodation.TENT, AccessMode.VEHICLE, Availability.AVAILABLE, true,
            Availability.NONE, true, true, false, Availability.LIMITED,
        ),
    )

    private fun vm(): OutdoorPackingViewModel {
        val repo = InMemoryOutdoorRepository()
        val service = OutdoorService(repo, Clock { "2026-06-27T12:00:00.000Z" })
        service.createCampingOuting("o1", "Camping Pucón", input())
        return OutdoorPackingViewModel(service, "o1")
    }

    @Test fun `estado inicial tiene titulo, grupos y progreso cero`() {
        val state = vm().uiState.value
        assertThat(state.title).isEqualTo("Camping Pucón")
        assertThat(state.groups).isNotEmpty()
        assertThat(state.progress.packed).isEqualTo(0)
        assertThat(state.progress.total).isGreaterThan(0)
    }

    @Test fun `togglePacked incrementa el progreso`() {
        val vm = vm()
        val before = vm.uiState.value.progress.packed
        vm.togglePacked("safety.first_aid", true)
        assertThat(vm.uiState.value.progress.packed).isEqualTo(before + 1)
    }

    @Test fun `outing inexistente produce estado vacio`() {
        val repo = InMemoryOutdoorRepository()
        val service = OutdoorService(repo, Clock { "2026-06-27T12:00:00.000Z" })
        val state = OutdoorPackingViewModel(service, "missing").uiState.value
        assertThat(state.groups).isEmpty()
        assertThat(state.title).isEmpty()
    }
}
