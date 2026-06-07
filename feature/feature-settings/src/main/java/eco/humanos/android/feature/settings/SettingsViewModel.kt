package eco.humanos.android.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eco.humanos.android.integrations.humanos.HumanosGateway
import eco.humanos.android.integrations.quebot.QuebotGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val humanosGateway: HumanosGateway,
    private val quebotGateway: QuebotGateway,
) : ViewModel() {

    data class SettingsUiState(
        val humanosConnected: Boolean = false,
        val quebotConnected: Boolean = false,
        val isCheckingConnections: Boolean = true,
    )

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        checkConnections()
    }

    private fun checkConnections() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingConnections = true) }
            val humanosOk = humanosGateway.checkConnectivity()
            val quebotStatus = quebotGateway.checkStatus()
            _uiState.update {
                it.copy(
                    humanosConnected = humanosOk,
                    quebotConnected = quebotStatus.getOrNull()?.healthy == true,
                    isCheckingConnections = false,
                )
            }
        }
    }
}
