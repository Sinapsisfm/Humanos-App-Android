/**
 * feature-outdoor / OutdoorPackingViewModel.kt
 *
 * ViewModel R1 de la lista de empaque de camping. Plano (sin Hilt) para mantener el
 * módulo AISLADO y unit-testeable; usa OutdoorService + PackingPresentation del núcleo.
 * Síncrono (repo in-memory) → sin coroutines, testeable leyendo `uiState.value`.
 */
package eco.humanos.android.feature.outdoor

import androidx.lifecycle.ViewModel
import eco.humanos.android.core.outdoor.awareness.AwarenessSignal
import eco.humanos.android.core.outdoor.packing.PackingEngine
import eco.humanos.android.core.outdoor.presentation.CategoryGroup
import eco.humanos.android.core.outdoor.presentation.PackingPresentation
import eco.humanos.android.core.outdoor.presentation.PackingProgress
import eco.humanos.android.core.outdoor.presentation.PackingSummary
import eco.humanos.android.core.outdoor.service.OutdoorService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OutdoorPackingUiState(
    val title: String,
    val groups: List<CategoryGroup>,
    val signals: List<AwarenessSignal>,
    val progress: PackingProgress,
    val summary: PackingSummary,
)

class OutdoorPackingViewModel(
    private val service: OutdoorService,
    private val outingId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(buildState())
    val uiState: StateFlow<OutdoorPackingUiState> = _uiState.asStateFlow()

    fun togglePacked(key: String, packed: Boolean) {
        service.applyEdits(outingId, listOf(PackingEngine.UserEdit.SetPacked(key, packed)))
        _uiState.value = buildState()
    }

    fun toggleBought(key: String, bought: Boolean) {
        service.applyEdits(outingId, listOf(PackingEngine.UserEdit.SetBought(key, bought)))
        _uiState.value = buildState()
    }

    private fun buildState(): OutdoorPackingUiState {
        val view = service.outingView(outingId)
        if (view == null) {
            return OutdoorPackingUiState(
                title = "",
                groups = emptyList(),
                signals = emptyList(),
                progress = PackingProgress(0, 0),
                summary = PackingPresentation.summary(emptyList()),
            )
        }
        val items = view.plan.items
        return OutdoorPackingUiState(
            title = view.outing.title,
            groups = PackingPresentation.groupByCategory(items),
            signals = view.signals,
            progress = PackingPresentation.progress(items),
            summary = PackingPresentation.summary(items),
        )
    }
}
