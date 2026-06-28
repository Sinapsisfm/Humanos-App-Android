/**
 * feature-outdoor / OutdoorPackingRoute.kt
 *
 * Punto de entrada componible para el flujo camping R1 (debug/laboratorio). Construye
 * un servicio in-memory + una salida de demostración y renderiza la pantalla. Mantiene
 * la navegación de la app como una sola línea (`OutdoorPackingRoute()`).
 *
 * Demo determinística (reloj fijo). NO usa red, NO usa datos reales, NO persiste a la DB
 * de la app (repo in-memory). Sustituible por una salida real + Room cuando se habiliten.
 */
package eco.humanos.android.feature.outdoor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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

@Composable
fun OutdoorPackingRoute(modifier: Modifier = Modifier) {
    val viewModel = remember { buildDemoViewModel() }
    val state by viewModel.uiState.collectAsState()
    OutdoorPackingScreen(
        state = state,
        onTogglePacked = viewModel::togglePacked,
        modifier = modifier,
    )
}

private fun buildDemoViewModel(): OutdoorPackingViewModel {
    val repo = InMemoryOutdoorRepository()
    // Reloj fijo: la demo es determinística (el core prohíbe relojes internos).
    val service = OutdoorService(repo, Clock { "2026-06-27T12:00:00.000Z" })
    service.createCampingOuting("demo", "Camping de demostración", demoInput())
    return OutdoorPackingViewModel(service, "demo")
}

private fun demoInput(): PackingInput = PackingInput(
    scenario = ScenarioKind.CAMPING,
    season = Season.OTONO,
    territory = "CL-AR",
    nights = 3,
    participants = listOf(
        OutdoorParticipant("a1", "Adulto 1", ParticipantRole.LEAD, AgeClass.ADULT),
        OutdoorParticipant("a2", "Adulto 2", ParticipantRole.ADULT, AgeClass.ADULT),
        OutdoorParticipant("c1", "Niño 1", ParticipantRole.MINOR, AgeClass.CHILD),
    ),
    facility = FacilityProfile(
        accommodation = Accommodation.TENT,
        access = AccessMode.VEHICLE,
        water = Availability.LIMITED,
        potableWater = false,
        electricity = Availability.NONE,
        toilets = false,
        showers = false,
        refrigeration = false,
        nearbyCommerce = Availability.LIMITED,
    ),
)
