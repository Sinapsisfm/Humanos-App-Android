package eco.humanos.android.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController

/**
 * Root composable for the humanOS app shell.
 *
 * Thin native shell (web-first · ADR-APK-THIN-SHELL): NO native bottom bar and
 * NO inset padding at this level — the NavHost is rendered edge-to-edge so each
 * screen owns its system insets EXACTLY ONCE (the WebView fills under the system
 * bars; the native sign-in gate / Settings apply their own systemBarsPadding).
 * This removes the dark top/bottom bands that came from double inset handling.
 */
@Composable
fun HumanosApp() {
    val navController = rememberNavController()
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        HumanosNavHost(navController = navController)
    }
}
