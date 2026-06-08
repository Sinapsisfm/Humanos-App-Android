package eco.humanos.android.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eco.humanos.android.core.model.task.TaskItem
import eco.humanos.android.data.tasks.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the dashboard with an offline-first flow: the UI observes tasks
 * from Room via [TaskRepository.observeTasks], while [TaskRepository.syncFromRemote]
 * refreshes Room from the HumanOS gateway. Observers react automatically
 * once Room is updated.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
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
        observeTasks()
        refresh()
    }

    private fun observeTasks() {
        viewModelScope.launch {
            taskRepository.observeTasks().collect { tasks ->
                _uiState.update { it.copy(tasks = tasks) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            taskRepository.syncFromRemote()
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
}
