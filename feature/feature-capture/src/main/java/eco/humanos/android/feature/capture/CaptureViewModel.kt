package eco.humanos.android.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eco.humanos.android.core.model.common.IntegrationSource
import eco.humanos.android.core.model.common.TraceEvent
import eco.humanos.android.core.observability.TraceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val traceRepository: TraceRepository,
) : ViewModel() {

    data class CaptureUiState(
        val textInput: String = "",
        val isSaving: Boolean = false,
        val savedMessage: String? = null,
    )

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    fun updateText(text: String) {
        _uiState.update { it.copy(textInput = text, savedMessage = null) }
    }

    fun saveCapture() {
        val text = _uiState.value.textInput
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            traceRepository.logEvent(
                TraceEvent(
                    id = java.util.UUID.randomUUID().toString(),
                    entityType = "capture",
                    entityId = java.util.UUID.randomUUID().toString(),
                    action = "created",
                    source = IntegrationSource.LOCAL,
                    userId = "local-user",
                    metadata = null,
                    timestamp = System.currentTimeMillis(),
                ),
            )
            _uiState.update {
                it.copy(textInput = "", isSaving = false, savedMessage = "Captura guardada")
            }
        }
    }
}
