package eco.humanos.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import eco.humanos.android.feature.capture.CaptureScreen
import eco.humanos.android.feature.dashboard.DashboardScreen
import eco.humanos.android.feature.settings.SettingsScreen
import eco.humanos.android.feature.tasks.TasksScreen
import eco.humanos.android.feature.web.WebModulesScreen
import eco.humanos.android.feature.web.WebViewScreen

/**
 * Root navigation host (thin native shell · web-first).
 *
 * Start = DASHBOARD, used ONLY as an auth gate: as soon as the HumanOS session
 * is ready it hands off to the full-screen HumanOS Web shell (`web/home`),
 * popping itself so "back" never returns to a native home. The native
 * Tasks/Capture/Modules screens stay compiled but are deprecated and no longer
 * reachable from a native bar. Native Settings + the Claude channel are reached
 * from the web shell's discreet top actions (onOpenSettings / onOpenChat).
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
            DashboardScreen(
                onReady = {
                    navController.navigate("web/home") {
                        popUpTo(TopLevelDestination.DASHBOARD.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(TopLevelDestination.TASKS.route) {
            TasksScreen()
        }
        composable(TopLevelDestination.CAPTURE.route) {
            CaptureScreen()
        }
        composable(TopLevelDestination.MODULES.route) {
            WebModulesScreen(
                onOpen = { moduleKey -> navController.navigate("web/$moduleKey") },
            )
        }
        composable("web/{moduleKey}") { backStackEntry ->
            WebViewScreen(
                moduleKey = backStackEntry.arguments?.getString("moduleKey").orEmpty(),
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(TopLevelDestination.SETTINGS.route) },
                onOpenChat = { navController.navigate("web/chat") },
            )
        }
        composable(TopLevelDestination.SETTINGS.route) {
            SettingsScreen()
        }
    }
}
