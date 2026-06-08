package eco.humanos.android.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eco.humanos.android.core.model.capture.CaptureItem
import eco.humanos.android.data.capture.repository.CaptureRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val captureRepository: CaptureRepository,
) : ViewModel() {

    data class CaptureUiState(
        val textInput: String = "",
        val isSaving: Boolean = false,
        val savedMessage: String? = null,
        val captures: List<CaptureItem> = emptyList(),
    )

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            captureRepository.observeCaptures().collect { captures ->
                _uiState.update { it.copy(captures = captures) }
            }
        }
    }

    fun updateText(text: String) {
        _uiState.update { it.copy(textInput = text, savedMessage = null) }
    }

    /**
     * Append [fragment] to the current text (used by the voice/photo/file
     * shortcuts), inserting a separating space/newline when there's already text.
     */
    fun appendText(fragment: String) {
        if (fragment.isBlank()) return
        _uiState.update {
            val current = it.textInput
            val joined = if (current.isBlank()) fragment else "$current\n$fragment"
            it.copy(textInput = joined, savedMessage = null)
        }
    }

    fun saveCapture() {
        val text = _uiState.value.textInput
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            captureRepository.saveTextCapture(text)
            _uiState.update {
                it.copy(textInput = "", isSaving = false, savedMessage = "Captura guardada")
            }
        }
    }
}
