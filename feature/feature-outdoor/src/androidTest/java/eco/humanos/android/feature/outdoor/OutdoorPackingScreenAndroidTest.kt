/**
 * feature-outdoor / androidTest / OutdoorPackingScreenAndroidTest.kt
 *
 * Render Compose ON-DEVICE (emulador/dispositivo real). Verifica que la pantalla Outdoor
 * renderiza el Trip Gate + AwarenessOS y no usa lenguaje de seguridad, en runtime Android.
 * Ejecutar: ./gradlew :feature:feature-outdoor:connectedDebugAndroidTest
 */
package eco.humanos.android.feature.outdoor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
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

@RunWith(AndroidJUnit4::class)
class OutdoorPackingScreenAndroidTest {

    @get:Rule val composeRule = createComposeRule()

    private fun state(): OutdoorPackingUiState {
        val repo = InMemoryOutdoorRepository()
        val service = OutdoorService(repo, Clock { "2026-06-28T12:00:00.000Z" })
        val input = PackingInput(
            scenario = ScenarioKind.CAMPING, season = Season.VERANO, territory = "CL-AR", nights = 3,
            participants = listOf(OutdoorParticipant("a1", "A1", ParticipantRole.ADULT, AgeClass.ADULT)),
            facility = FacilityProfile(
                Accommodation.TENT, AccessMode.VEHICLE, Availability.AVAILABLE, true,
                Availability.NONE, true, true, false, Availability.LIMITED,
            ),
        )
        service.createCampingOuting("o1", "Camping Pucón", input)
        return OutdoorPackingViewModel(service, "o1", now = { "2026-06-28T12:00:00.000Z" }).uiState.value
    }

    @Test fun renders_title_and_technical_detail_without_safety_language() {
        composeRule.setContent {
            OutdoorPackingScreen(state = state(), onTogglePacked = { _, _ -> }, onDismissSignal = {})
        }
        composeRule.onNodeWithText("Camping Pucón").assertIsDisplayed()
        composeRule.onNodeWithText("Detalle técnico").assertExists()
        val banned = composeRule.onAllNodesWithText("seguro", substring = true, ignoreCase = true).fetchSemanticsNodes()
        assert(banned.isEmpty()) { "La UI no debe afirmar 'seguro'" }
    }

    @Test fun shows_preparation_incomplete_signal() {
        composeRule.setContent {
            OutdoorPackingScreen(state = state(), onTogglePacked = { _, _ -> }, onDismissSignal = {})
        }
        composeRule.onAllNodesWithText("Preparación incompleta", substring = true).fetchSemanticsNodes()
            .let { assert(it.isNotEmpty()) { "Debe mostrarse el aviso de preparación incompleta" } }
    }
}
