/**
 * feature-outdoor / test / OutdoorPackingScreenTest.kt
 *
 * Evidencia de UI runtime vía Compose test sobre Robolectric (sin emulador). Renderiza la
 * pantalla con un estado real (derivado del ViewModel) y verifica que el Trip Gate y las
 * señales se muestran, sin lenguaje de "seguridad".
 */
package eco.humanos.android.feature.outdoor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OutdoorPackingScreenTest {

    @get:Rule val composeRule = createComposeRule()

    private fun state(): OutdoorPackingUiState {
        val repo = InMemoryOutdoorRepository()
        val service = OutdoorService(repo, Clock { "2026-06-27T12:00:00.000Z" })
        val input = PackingInput(
            scenario = ScenarioKind.CAMPING, season = Season.VERANO, territory = "CL-AR", nights = 3,
            participants = listOf(OutdoorParticipant("a1", "A1", ParticipantRole.ADULT, AgeClass.ADULT)),
            facility = FacilityProfile(
                Accommodation.TENT, AccessMode.VEHICLE, Availability.AVAILABLE, true,
                Availability.NONE, true, true, false, Availability.LIMITED,
            ),
        )
        service.createCampingOuting("o1", "Camping Pucón", input)
        return OutdoorPackingViewModel(service, "o1", now = { "2026-06-27T12:00:00.000Z" }).uiState.value
    }

    @Test fun `renderiza titulo, preparacion y detalle tecnico sin lenguaje de seguridad`() {
        composeRule.setContent {
            OutdoorPackingScreen(state = state(), onTogglePacked = { _, _ -> }, onDismissSignal = {})
        }
        composeRule.onNodeWithText("Camping Pucón").assertIsDisplayed()
        composeRule.onNodeWithText("Detalle técnico").assertExists()
        // No debe existir lenguaje que afirme seguridad.
        composeRule.onAllNodesWithText("seguro", substring = true, ignoreCase = true)
            .fetchSemanticsNodes().also { assert(it.isEmpty()) { "La UI no debe afirmar 'seguro'" } }
    }

    @Test fun `muestra aviso de preparacion incompleta`() {
        composeRule.setContent {
            OutdoorPackingScreen(state = state(), onTogglePacked = { _, _ -> }, onDismissSignal = {})
        }
        // Con plan sin empacar, AwarenessOS marca preparación incompleta.
        composeRule.onAllNodesWithText("Preparación incompleta", substring = true).onFirst().assertExists()
    }
}
