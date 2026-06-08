package eco.humanos.android.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eco.humanos.android.core.model.task.TaskItem
import eco.humanos.android.core.model.task.TaskStatus
import eco.humanos.android.data.tasks.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Tasks screen with an offline-first flow: the UI observes tasks
 * from Room via [TaskRepository.observeTasks], while writes (create / toggle /
 * delete) go through the repository, which is the single source of truth.
 * Observers react automatically once Room is updated.
 */
@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
) : ViewModel() {

    data class TasksUiState(
        val tasks: List<TaskItem> = emptyList(),
        val isLoading: Boolean = true,
        val newTaskTitle: String = "",
        val error: String? = null,
    )

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    init {
        observeTasks()
    }

    private fun observeTasks() {
        viewModelScope.launch {
            taskRepository.observeTasks().collect { tasks ->
                _uiState.update { it.copy(tasks = tasks, isLoading = false) }
            }
        }
    }

    /** Update the in-progress new-task title bound to the input field. */
    fun updateNewTaskTitle(text: String) {
        _uiState.update { it.copy(newTaskTitle = text) }
    }

    /**
     * Create a task from the current [TasksUiState.newTaskTitle] if non-blank,
     * then clear the field. No-op when the title is blank.
     */
    fun addTask() {
        val title = _uiState.value.newTaskTitle.trim()
        if (title.isBlank()) return

        viewModelScope.launch {
            runCatching { taskRepository.createTask(title = title) }
                .onSuccess {
                    _uiState.update { it.copy(newTaskTitle = "", error = null) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    /**
     * Flip a task between PENDING and DONE. When marking DONE, stamp
     * [TaskItem.completedAt]; when reopening, clear it. Bumps [TaskItem.updatedAt].
     */
    fun toggleTaskDone(task: TaskItem) {
        val now = System.currentTimeMillis()
        val markDone = task.status != TaskStatus.DONE
        val updated = task.copy(
            status = if (markDone) TaskStatus.DONE else TaskStatus.PENDING,
            completedAt = if (markDone) now else null,
            updatedAt = now,
        )

        viewModelScope.launch {
            runCatching { taskRepository.updateTask(updated) }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    /** Delete the task with the given [id]. */
    fun deleteTask(id: String) {
        viewModelScope.launch {
            runCatching { taskRepository.deleteTask(id) }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }
}
