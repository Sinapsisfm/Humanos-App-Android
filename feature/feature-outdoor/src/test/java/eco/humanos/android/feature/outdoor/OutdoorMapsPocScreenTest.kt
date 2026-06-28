/**
 * feature-outdoor / test / OutdoorMapsPocScreenTest.kt
 *
 * Evidencia de UI runtime de la POC R3 vía Compose test sobre Robolectric (sin emulador).
 * Verifica que el Canvas de mapa renderiza con datos sintéticos y que el aviso de
 * "DATOS SINTÉTICOS" está presente (no se afirma cobertura real).
 */
package eco.humanos.android.feature.outdoor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OutdoorMapsPocScreenTest {

    @get:Rule val composeRule = createComposeRule()

    @Test fun `renderiza el canvas del mapa POC con datos sinteticos`() {
        composeRule.setContent { OutdoorMapsPocRoute() }
        composeRule.onNodeWithTag(OUTDOOR_MAPS_POC_TAG).assertExists()
        composeRule.onNodeWithText("Mapa offline · POC").assertIsDisplayed()
    }

    @Test fun `muestra aviso de datos sinteticos y no afirma cobertura real`() {
        composeRule.setContent { OutdoorMapsPocRoute() }
        composeRule.onNodeWithText("DATOS SINTÉTICOS", substring = true).assertIsDisplayed()
    }
}
