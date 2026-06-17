package eco.humanos.android.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController

/**
 * Root composable for the humanOS app shell.
 *
 * Thin native shell (web-first · ADR-APK-THIN-SHELL): there is NO native bottom
 * navigation bar. The app opens HumanOS Web full-screen via the session bridge;
 * the native screens are deprecated and serve only as the sign-in gate /
 * fallback. Device-only capabilities (native Settings, Claude channel) are
 * reached from the web shell's discreet top actions, never a permanent bar.
 */
@Composable
fun HumanosApp() {
    val navController = rememberNavController()
    Scaffold { innerPadding ->
        HumanosNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
