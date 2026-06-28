/**
 * feature-outdoor / test / OutdoorRestoreTest.kt
 *
 * Niveles de continuidad (Fase K):
 *  - Recomposición: el estado es estable (StateFlow).
 *  - Recreación de Activity: el VM se reconstruye con `initialDismissed` (mismo store).
 *  - Proceso muerto: el almacén persistente (serialize/restore, equivalente a Room) revive
 *    salida/plan/eventos; los reconocimientos se restauran vía `initialDismissed`.
 *  - Reconstrucción sin DUPLICAR eventos ni findings.
 *
 * Datos puramente visuales (acordeón "detalle técnico") NO se persisten.
 */
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

class OutdoorRestoreTest {

    private val AT = "2026-06-27T12:00:00.000Z"

    private fun input() = PackingInput(
        scenario = ScenarioKind.CAMPING, season = Season.VERANO, territory = "CL-AR", nights = 3,
        participants = listOf(OutdoorParticipant("a1", "A1", ParticipantRole.ADULT, AgeClass.ADULT)),
        facility = FacilityProfile(
            Accommodation.TENT, AccessMode.VEHICLE, Availability.AVAILABLE, true,
            Availability.NONE, true, true, false, Availability.LIMITED,
        ),
    )

    @Test fun `recomposicion - el estado tiene contenido y es estable`() {
        val repo = InMemoryOutdoorRepository()
        val service = OutdoorService(repo, Clock { AT })
        service.createCampingOuting("o1", "Camping", input())
        val vm = OutdoorPackingViewModel(service, "o1", now = { AT })
        val s = vm.uiState.value
        // contenido real (no solo "value == value"): título, grupos y gate presentes
        assertThat(s.title).isEqualTo("Camping")
        assertThat(s.groups).isNotEmpty()
        assertThat(s.gate).isNotNull()
        // estable entre lecturas (recomposición no recomputa ni muta)
        assertThat(vm.uiState.value).isSameInstanceAs(s)
    }

    @Test fun `recreacion de Activity - initialDismissed conserva el reconocimiento`() {
        val repo = InMemoryOutdoorRepository()
        val service = OutdoorService(repo, Clock { AT })
        service.createCampingOuting("o1", "Camping", input())
        val vm1 = OutdoorPackingViewModel(service, "o1", now = { AT })
        vm1.dismissSignal("aware.prep_incomplete")
        val snapshot = vm1.dismissedSnapshot()

        // Activity recreada: mismo store (service), nuevo VM con el snapshot.
        val vm2 = OutdoorPackingViewModel(service, "o1", now = { AT }, initialDismissed = snapshot)
        assertThat(vm2.uiState.value.signals.map { it.code }).doesNotContain("aware.prep_incomplete")
    }

    @Test fun `proceso muerto - almacen persistente revive salida y no duplica eventos`() {
        val repo = InMemoryOutdoorRepository()
        val service = OutdoorService(repo, Clock { AT })
        service.createCampingOuting("o1", "Camping", input())
        val vm1 = OutdoorPackingViewModel(service, "o1", now = { AT })
        vm1.dismissSignal("aware.prep_incomplete")
        val snapshot = vm1.dismissedSnapshot()
        val eventsBefore = repo.getEvents("o1").size
        val gateBefore = vm1.uiState.value.gate

        // Proceso muerto + reapertura: el almacén persistente (Room equiv.) revive vía blob.
        val revived = InMemoryOutdoorRepository.restore(repo.serialize())
        val service2 = OutdoorService(revived, Clock { AT })
        val vm2 = OutdoorPackingViewModel(service2, "o1", now = { AT }, initialDismissed = snapshot)

        val s2 = vm2.uiState.value
        assertThat(s2.title).isEqualTo("Camping")
        assertThat(revived.getEvents("o1").size).isEqualTo(eventsBefore) // sin duplicar
        assertThat(s2.signals.map { it.code }).doesNotContain("aware.prep_incomplete") // reconocimiento restaurado
        // findings reconstruidos idénticos (no se acumulan ni duplican)
        assertThat(s2.gate!!.findings).isEqualTo(gateBefore!!.findings)
    }
}
