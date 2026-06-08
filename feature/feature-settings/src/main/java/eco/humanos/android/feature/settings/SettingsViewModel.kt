package eco.humanos.android.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import eco.humanos.android.core.model.auth.AuthState
import eco.humanos.android.core.update.UpdateChecker
import eco.humanos.android.core.update.UpdateInfo
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
    @ApplicationContext private val appContext: Context,
    private val humanosGateway: HumanosGateway,
    private val quebotGateway: QuebotGateway,
    private val authRepository: AuthRepository,
    private val googleCredentialManager: GoogleCredentialManager,
    private val updateChecker: UpdateChecker,
) : ViewModel() {

    data class SettingsUiState(
        val humanosConnected: Boolean = false,
        val quebotConnected: Boolean = false,
        val isCheckingConnections: Boolean = true,
        val authState: AuthState = AuthState.Loading,
        val isSigningIn: Boolean = false,
        val authError: String? = null,
        /** Installed app version (e.g. "0.1.0"), resolved from the package manager. */
        val currentVersionName: String = "",
        /** Non-null when a newer release is available on GitHub. */
        val availableUpdate: UpdateInfo? = null,
        /** True while a (manual or initial) update check is in flight. */
        val isCheckingUpdate: Boolean = false,
        /** Transient result of a manual check, e.g. "Estás en la última versión". */
        val updateCheckMessage: String? = null,
    )

    private val _uiState = MutableStateFlow(
        SettingsUiState(currentVersionName = resolveVersionName()),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        checkConnections()
        observeAuth()
        checkForUpdate()
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
     * Queries GitHub Releases for a newer build. Failures are swallowed by the
     * checker (returns null), so this never surfaces an error to the UI — the
     * update banner simply stays hidden.
     */
    private fun checkForUpdate(manual: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpdate = true, updateCheckMessage = null) }
            val update = updateChecker.checkForUpdate(_uiState.value.currentVersionName)
            _uiState.update {
                it.copy(
                    isCheckingUpdate = false,
                    availableUpdate = update,
                    updateCheckMessage = if (manual && update == null) {
                        "Estás en la última versión"
                    } else {
                        null
                    },
                )
            }
        }
    }

    /** Manual "Comprobar actualizaciones" action from Settings. */
    fun recheckUpdate() = checkForUpdate(manual = true)

    /**
     * Silent re-check run whenever the Settings screen is resumed, so the update
     * banner shows up without an app restart (init alone misses a release that
     * was published after the ViewModel was first created).
     */
    fun refreshUpdate() = checkForUpdate(manual = false)

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

    /**
     * Reads the installed version name from the package manager. Returns an empty
     * string on any failure — a missing package (impossible in practice) or a
     * non-Android test runtime where [appContext] is a stub. Falling back to empty
     * keeps construction side-effect-free and testable; the update check then
     * compares against "" (0.0.0), so an unknown current version conservatively
     * surfaces whatever the latest release is rather than crashing.
     */
    private fun resolveVersionName(): String =
        runCatching {
            appContext.packageManager
                .getPackageInfo(appContext.packageName, 0)
                .versionName
                .orEmpty()
        }.getOrDefault("")
}
