package eco.humanos.android.feature.web

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eco.humanos.android.core.model.common.IntegrationConfig
import eco.humanos.android.data.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Builds the session-bridge URL for an embedded web module (ADR-0006): fetches a
 * fresh HumanOS bridge token and hands the WebView a `/mobile-login` URL that
 * logs in and redirects to the module — no Google OAuth in the WebView.
 */
@HiltViewModel
class WebViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    data class State(
        val isLoading: Boolean = true,
        val url: String? = null,
        val title: String = "",
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun load(moduleKey: String) {
        val module = WebModule.fromKey(moduleKey)
        if (module == null) {
            _state.update { it.copy(isLoading = false, error = "Módulo desconocido.") }
            return
        }
        _state.update { it.copy(isLoading = true, title = module.label, error = null) }
        viewModelScope.launch {
            val token = runCatching { authRepository.getHumanosToken() }.getOrNull()
            if (token.isNullOrBlank()) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "No hay sesión HumanOS. Entra con Google en Inicio y vuelve.",
                    )
                }
                return@launch
            }
            // Token in the fragment (client-only, never server-logged);
            // /mobile-login exchanges it for a web session and redirects to `next`.
            val url = "${IntegrationConfig.HUMANOS_WEB_BASE}/mobile-login#token=$token&next=${module.path}"
            _state.update { it.copy(isLoading = false, url = url) }
        }
    }
}
