package eco.humanos.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import eco.humanos.android.feature.capture.CaptureScreen
import eco.humanos.android.feature.dashboard.DashboardScreen
import eco.humanos.android.feature.settings.SettingsScreen

/**
 * Root navigation host for the humanOS app.
 *
 * Wires each [TopLevelDestination] route to its corresponding feature
 * screen composable. Nested navigation graphs for detail screens will
 * be added in later phases.
 */
@Composable
fun HumanosNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.DASHBOARD.route,
        modifier = modifier,
    ) {
        composable(TopLevelDestination.DASHBOARD.route) {
            DashboardScreen()
        }
        composable(TopLevelDestination.CAPTURE.route) {
            CaptureScreen()
        }
        composable(TopLevelDestination.SETTINGS.route) {
            SettingsScreen()
        }
    }
}
