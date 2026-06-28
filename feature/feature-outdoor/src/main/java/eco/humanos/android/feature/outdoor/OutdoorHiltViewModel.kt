/**
 * feature-outdoor / OutdoorHiltViewModel.kt
 *
 * ViewModel real de la app: consume el `OutdoorRepository` provisto por Hilt (selección
 * in-memory/Room según OUTDOOR_ROOM_ENABLED) → al activar Room, la pantalla persiste de
 * verdad. Conserva el reconocimiento de señales en `SavedStateHandle` (sobrevive
 * recreación de Activity y proceso muerto). Delega la lógica al `OutdoorPackingViewModel`
 * plano (determinístico y unit-testeable).
 *
 * Para la demo interna crea una salida de camping si el repositorio está vacío
 * (idempotente). Una integración completa recibiría el `outingId` por navegación.
 */
package eco.humanos.android.feature.outdoor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
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
import eco.humanos.android.core.outdoor.gate.GroupExperience
import eco.humanos.android.core.outdoor.gate.IsolationLevel
import eco.humanos.android.core.outdoor.gate.TripConditions
import eco.humanos.android.core.outdoor.gate.TripContext
import eco.humanos.android.core.outdoor.repository.OutdoorRepository
import eco.humanos.android.core.outdoor.service.Clock
import eco.humanos.android.core.outdoor.service.OutdoorService
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class OutdoorHiltViewModel @Inject constructor(
    repository: OutdoorRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val service = OutdoorService(repository, Clock { Instant.now().toString() })
    private val delegate: OutdoorPackingViewModel

    init {
        if (service.outingView(DEMO_OUTING_ID) == null) {
            service.createCampingOuting(DEMO_OUTING_ID, DEMO_TITLE, demoInput())
        }
        val saved = savedState.get<ArrayList<String>>(KEY_DISMISSED)?.toSet() ?: emptySet()
        delegate = OutdoorPackingViewModel(
            service = service,
            outingId = DEMO_OUTING_ID,
            tripContext = demoContext(),
            now = { Instant.now().toString() },
            repoLabel = repository::class.simpleName ?: "outdoor",
            initialDismissed = saved,
        )
    }

    val uiState: StateFlow<OutdoorPackingUiState> get() = delegate.uiState

    fun togglePacked(key: String, packed: Boolean) = delegate.togglePacked(key, packed)
    fun toggleBought(key: String, bought: Boolean) = delegate.toggleBought(key, bought)

    fun dismissSignal(code: String) {
        delegate.dismissSignal(code)
        // Persistir el reconocimiento → sobrevive recreación/proceso muerto.
        savedState[KEY_DISMISSED] = ArrayList(delegate.dismissedSnapshot())
    }

    private companion object {
        const val DEMO_OUTING_ID = "demo"
        const val DEMO_TITLE = "Camping de demostración"
        const val KEY_DISMISSED = "outdoor_dismissed"

        fun demoInput() = PackingInput(
            scenario = ScenarioKind.CAMPING, season = Season.OTONO, territory = "CL-AR", nights = 3,
            participants = listOf(
                OutdoorParticipant("a1", "Adulto 1", ParticipantRole.LEAD, AgeClass.ADULT),
                OutdoorParticipant("a2", "Adulto 2", ParticipantRole.ADULT, AgeClass.ADULT),
                OutdoorParticipant("c1", "Niño 1", ParticipantRole.MINOR, AgeClass.CHILD),
            ),
            facility = FacilityProfile(
                accommodation = Accommodation.TENT, access = AccessMode.VEHICLE,
                water = Availability.LIMITED, potableWater = false, electricity = Availability.NONE,
                toilets = false, showers = false, refrigeration = false, nearbyCommerce = Availability.LIMITED,
            ),
        )

        fun demoContext() = TripContext(
            conditions = TripConditions(cold = true, rain = true, isolation = IsolationLevel.MODERATE),
            experience = GroupExperience.INTERMEDIATE,
            weatherDataAgeHours = 6,
        )
    }
}
