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
import eco.humanos.android.core.outdoor.gate.ComplexityClass
import eco.humanos.android.core.outdoor.gate.TripConditions
import eco.humanos.android.core.outdoor.gate.TripContext
import eco.humanos.android.core.outdoor.repository.InMemoryOutdoorRepository
import eco.humanos.android.core.outdoor.service.Clock
import eco.humanos.android.core.outdoor.service.OutdoorService
import org.junit.Test

class OutdoorPackingViewModelTest {

    private val AT = "2026-06-27T12:00:00.000Z"

    private fun input() = PackingInput(
        scenario = ScenarioKind.CAMPING, season = Season.VERANO, territory = "CL-AR", nights = 3,
        participants = listOf(
            OutdoorParticipant("a1", "Adulto 1", ParticipantRole.ADULT, AgeClass.ADULT),
            OutdoorParticipant("c1", "Niño 1", ParticipantRole.MINOR, AgeClass.CHILD),
        ),
        facility = FacilityProfile(
            Accommodation.TENT, AccessMode.VEHICLE, Availability.AVAILABLE, true,
            Availability.NONE, true, true, false, Availability.LIMITED,
        ),
    )

    private fun vm(
        ctx: TripContext = TripContext(),
        initialDismissed: Set<String> = emptySet(),
    ): OutdoorPackingViewModel {
        val repo = InMemoryOutdoorRepository()
        val service = OutdoorService(repo, Clock { AT })
        service.createCampingOuting("o1", "Camping Pucón", input())
        return OutdoorPackingViewModel(service, "o1", ctx, now = { AT }, initialDismissed = initialDismissed)
    }

    @Test fun `estado inicial tiene gate, grupos y complejidad simple`() {
        val s = vm().uiState.value
        assertThat(s.title).isEqualTo("Camping Pucón")
        assertThat(s.groups).isNotEmpty()
        assertThat(s.gate).isNotNull()
        assertThat(s.gate!!.complexityClass).isEqualTo(ComplexityClass.SIMPLE)
        assertThat(s.degraded.name).isEqualTo("NONE")
        assertThat(s.repoLabel).isEqualTo("in-memory")
    }

    @Test fun `togglePacked incrementa el progreso`() {
        val v = vm()
        val before = v.uiState.value.progress.packed
        v.togglePacked("safety.first_aid", true)
        assertThat(v.uiState.value.progress.packed).isEqualTo(before + 1)
    }

    @Test fun `outing inexistente produce estado degradado`() {
        val repo = InMemoryOutdoorRepository()
        val service = OutdoorService(repo, Clock { AT })
        val s = OutdoorPackingViewModel(service, "missing", now = { AT }).uiState.value
        assertThat(s.degraded.name).isEqualTo("NO_OUTING")
        assertThat(s.gate).isNull()
        assertThat(s.groups).isEmpty()
    }

    @Test fun `dismiss quita la señal y la recuerda (restaurable)`() {
        val v = vm()
        assertThat(v.uiState.value.signals.map { it.code }).contains("aware.prep_incomplete")
        v.dismissSignal("aware.prep_incomplete")
        assertThat(v.uiState.value.signals.map { it.code }).doesNotContain("aware.prep_incomplete")
        assertThat(v.uiState.value.dismissedCodes).contains("aware.prep_incomplete")
        assertThat(v.dismissedSnapshot()).contains("aware.prep_incomplete")
    }

    @Test fun `dismissed inicial restaura el reconocimiento`() {
        val v = vm(initialDismissed = setOf("aware.prep_incomplete"))
        assertThat(v.uiState.value.signals.map { it.code }).doesNotContain("aware.prep_incomplete")
    }

    @Test fun `ni findings ni señales usan lenguaje de seguridad`() {
        val v = vm(ctx = TripContext(conditions = TripConditions(cold = true, snow = true)))
        val s = v.uiState.value
        val banned = Regex("seguro|segura|a salvo|garantiza|certific", RegexOption.IGNORE_CASE)
        val texts = buildList {
            s.gate?.findings?.forEach { add("${it.title} ${it.detail} ${it.recommendation ?: ""}") }
            s.signals.forEach { add("${it.title} ${it.detail} ${it.action ?: ""}") }
        }
        for (t in texts) assertThat(banned.containsMatchIn(t)).isFalse()
    }
}
