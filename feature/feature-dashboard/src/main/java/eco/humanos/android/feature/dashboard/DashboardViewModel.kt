package eco.humanos.android.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eco.humanos.android.core.model.task.TaskItem
import eco.humanos.android.integrations.humanos.HumanosGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val humanosGateway: HumanosGateway,
) : ViewModel() {

    data class DashboardUiState(
        val greeting: String = "Buenos dias",
        val tasks: List<TaskItem> = emptyList(),
        val isLoading: Boolean = true,
        val error: String? = null,
    )

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            humanosGateway.fetchTasks()
                .onSuccess { tasks ->
                    _uiState.update { it.copy(tasks = tasks, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
        }
    }

    fun refresh() = loadDashboard()
}
