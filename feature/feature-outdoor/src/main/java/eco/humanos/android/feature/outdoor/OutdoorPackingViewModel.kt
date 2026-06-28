/**
 * feature-outdoor / OutdoorPackingViewModel.kt
 *
 * ViewModel R1/R2 de la pantalla Outdoor. Plano (sin Hilt) para mantener el módulo
 * unit-testeable. Combina el plan de camping (packing) con el Trip Gate y las señales
 * situacionales de AwarenessOS. Síncrono (repo in-memory/Room síncrono) → testeable
 * leyendo `uiState.value`. El reconocimiento/descarte de señales se conserva en el VM
 * (y se restaura vía `initialDismissed`, Fase K).
 */
package eco.humanos.android.feature.outdoor

import androidx.lifecycle.ViewModel
import eco.humanos.android.core.outdoor.awareness.AwarenessEvaluator
import eco.humanos.android.core.outdoor.awareness.AwarenessSignal
import eco.humanos.android.core.outdoor.domain.ScenarioKind
import eco.humanos.android.core.outdoor.gate.TripContext
import eco.humanos.android.core.outdoor.gate.TripGate
import eco.humanos.android.core.outdoor.gate.TripGateResult
import eco.humanos.android.core.outdoor.packing.PackingEngine
import eco.humanos.android.core.outdoor.presentation.CategoryGroup
import eco.humanos.android.core.outdoor.presentation.PackingPresentation
import eco.humanos.android.core.outdoor.presentation.PackingProgress
import eco.humanos.android.core.outdoor.presentation.PackingSummary
import eco.humanos.android.core.outdoor.service.OutdoorService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DegradedReason { NONE, NO_OUTING }

data class OutdoorPackingUiState(
    val title: String,
    val groups: List<CategoryGroup>,
    val progress: PackingProgress,
    val summary: PackingSummary,
    /** Trip Gate (null solo si no hay salida). */
    val gate: TripGateResult?,
    /** Señales situacionales (descartables) ya filtradas por reconocimiento. */
    val signals: List<AwarenessSignal>,
    val dismissedCodes: Set<String>,
    val degraded: DegradedReason,
    /** Etiqueta del origen de datos (para estado degradado/fallback). */
    val repoLabel: String,
)

class OutdoorPackingViewModel(
    private val service: OutdoorService,
    private val outingId: String,
    private val tripContext: TripContext = TripContext(),
    private val secondary: Set<ScenarioKind> = emptySet(),
    private val now: () -> String = { "2026-06-27T12:00:00.000Z" },
    private val repoLabel: String = "in-memory",
    initialDismissed: Set<String> = emptySet(),
) : ViewModel() {

    private val dismissed = LinkedHashSet(initialDismissed)

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

    /** Reconoce/descarta una señal descartable; persiste en el VM (restaurable). */
    fun dismissSignal(code: String) {
        dismissed.add(code)
        _uiState.value = buildState()
    }

    /** Para restaurar el estado de reconocimiento (Fase K). */
    fun dismissedSnapshot(): Set<String> = LinkedHashSet(dismissed)

    private fun buildState(): OutdoorPackingUiState {
        val view = service.outingView(outingId)
            ?: return OutdoorPackingUiState(
                title = "", groups = emptyList(),
                progress = PackingProgress(0, 0), summary = PackingPresentation.summary(emptyList()),
                gate = null, signals = emptyList(), dismissedCodes = dismissed,
                degraded = DegradedReason.NO_OUTING, repoLabel = repoLabel,
            )
        val items = view.plan.items
        val gate = TripGate.evaluate(view.outing, items, tripContext, secondary)
        val situational = AwarenessEvaluator
            .evaluateSituational(view.outing, items, tripContext, secondary = secondary, at = now())
            .filter { it.code !in dismissed }
        return OutdoorPackingUiState(
            title = view.outing.title,
            groups = PackingPresentation.groupByCategory(items),
            progress = PackingPresentation.progress(items),
            summary = PackingPresentation.summary(items),
            gate = gate,
            signals = situational,
            dismissedCodes = LinkedHashSet(dismissed),
            degraded = DegradedReason.NONE,
            repoLabel = repoLabel,
        )
    }
}
