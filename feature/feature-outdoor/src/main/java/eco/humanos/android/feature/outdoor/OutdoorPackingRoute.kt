/**
 * feature-outdoor / OutdoorPackingRoute.kt
 *
 * Punto de entrada componible del flujo Outdoor (debug/laboratorio, flag OUTDOOR_R1_ENABLED).
 * Obtiene el ViewModel desde Hilt → consume el `OutdoorRepository` SELECCIONADO (in-memory
 * por default; Room si OUTDOOR_ROOM_ENABLED). Así la selección de repositorio y el adaptador
 * Room llegan de verdad a la UI. El reconocimiento de señales se conserva vía SavedStateHandle.
 */
package eco.humanos.android.feature.outdoor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun OutdoorPackingRoute(
    modifier: Modifier = Modifier,
    viewModel: OutdoorHiltViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    OutdoorPackingScreen(
        state = state,
        onTogglePacked = viewModel::togglePacked,
        onDismissSignal = viewModel::dismissSignal,
        modifier = modifier,
    )
}
