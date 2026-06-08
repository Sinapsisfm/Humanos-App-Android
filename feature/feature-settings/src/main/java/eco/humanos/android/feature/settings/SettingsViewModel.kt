package eco.humanos.android.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eco.humanos.android.core.model.auth.AuthState
import eco.humanos.android.data.auth.AuthRepository
import eco.humanos.android.data.auth.GoogleCredentialManager
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
    private val authRepository: AuthRepository,
    private val googleCredentialManager: GoogleCredentialManager,
) : ViewModel() {

    data class SettingsUiState(
        val humanosConnected: Boolean = false,
        val quebotConnected: Boolean = false,
        val isCheckingConnections: Boolean = true,
        val authState: AuthState = AuthState.Loading,
        val isSigningIn: Boolean = false,
        val authError: String? = null,
    )

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        checkConnections()
        observeAuth()
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

    private fun observeAuth() {
        viewModelScope.launch {
            authRepository.observeAuthState().collect { state ->
                _uiState.update { it.copy(authState = state) }
            }
        }
    }

    /**
     * Runs the Google Sign-In flow. [activityContext] must be an Activity
     * context because the Credential Manager shows UI anchored to it.
     */
    fun signIn(activityContext: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSigningIn = true, authError = null) }
            val result = googleCredentialManager.getGoogleIdToken(activityContext)
                .mapCatching { idToken -> authRepository.signInWithGoogle(idToken).getOrThrow() }
            _uiState.update {
                it.copy(
                    isSigningIn = false,
                    authError = result.exceptionOrNull()?.message,
                )
            }
            // On success, observeAuth() will receive the Authenticated state
            // from the Firebase AuthStateListener and update the UI.
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.update { it.copy(authError = null) }
        }
    }
}
