package eco.humanos.android.feature.tasks

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import eco.humanos.android.core.model.common.IntegrationSource
import eco.humanos.android.core.model.context.GovernanceState
import eco.humanos.android.core.model.task.EntityOrigin
import eco.humanos.android.core.model.task.TaskItem
import eco.humanos.android.core.model.task.TaskPriority
import eco.humanos.android.core.model.task.TaskStatus
import eco.humanos.android.data.tasks.repository.TaskRepository
import eco.humanos.android.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Pure-JVM unit tests for [TasksViewModel] using a hand-written fake
 * [TaskRepository] backed by a [MutableStateFlow]. No Robolectric / Android
 * runtime required. Mirrors the fake + runTest + Turbine style used by
 * DashboardViewModelTest and the data-layer tests.
 */
class TasksViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ── Fake ─────────────────────────────────────────────────────────────────

    /**
     * In-memory [TaskRepository]. [observeTasks] returns the backing flow;
     * mutations operate on that flow so collectors react immediately.
     */
    private class FakeTaskRepository(
        seed: List<TaskItem> = emptyList(),
    ) : TaskRepository {

        val state = MutableStateFlow(seed)

        override fun observeTasks(): Flow<List<TaskItem>> = state

        override suspend fun syncFromRemote(): Result<Int> = Result.success(state.value.size)

        override suspend fun createTask(
            title: String,
            description: String?,
            priority: TaskPriority,
        ): String {
            val id = "local-${state.value.size + 1}"
            state.update { it + task(id, title, description, priority) }
            return id
        }

        override suspend fun updateTask(task: TaskItem) {
            state.update { current -> current.map { if (it.id == task.id) task else it } }
        }

        override suspend fun deleteTask(id: String) {
            state.update { current -> current.filterNot { it.id == id } }
        }
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    fun `on init collects tasks and clears loading`() = runTest {
        val repo = FakeTaskRepository(seed = listOf(task("r1", "Task one")))

        val viewModel = TasksViewModel(repo)

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.tasks.map { it.id }).containsExactly("r1")
            assertThat(state.isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addTask creates a task from the typed title and clears the field`() = runTest {
        val repo = FakeTaskRepository()
        val viewModel = TasksViewModel(repo)

        viewModel.updateNewTaskTitle("Buy milk")
        assertThat(viewModel.uiState.value.newTaskTitle).isEqualTo("Buy milk")

        viewModel.addTask()

        assertThat(repo.state.value.map { it.title }).containsExactly("Buy milk")
        assertThat(repo.state.value.single().status).isEqualTo(TaskStatus.PENDING)
        assertThat(viewModel.uiState.value.tasks.map { it.title }).containsExactly("Buy milk")
        assertThat(viewModel.uiState.value.newTaskTitle).isEmpty()
    }

    @Test
    fun `addTask is a no-op when the title is blank`() = runTest {
        val repo = FakeTaskRepository()
        val viewModel = TasksViewModel(repo)

        viewModel.updateNewTaskTitle("   ")
        viewModel.addTask()

        assertThat(repo.state.value).isEmpty()
    }

    @Test
    fun `toggleTaskDone flips PENDING to DONE and stamps completedAt`() = runTest {
        val pending = task("r1", "Task one", status = TaskStatus.PENDING)
        val repo = FakeTaskRepository(seed = listOf(pending))
        val viewModel = TasksViewModel(repo)

        viewModel.toggleTaskDone(pending)

        val afterDone = repo.state.value.single()
        assertThat(afterDone.status).isEqualTo(TaskStatus.DONE)
        assertThat(afterDone.completedAt).isNotNull()

        // Toggling again reopens it and clears completedAt.
        viewModel.toggleTaskDone(afterDone)

        val afterReopen = repo.state.value.single()
        assertThat(afterReopen.status).isEqualTo(TaskStatus.PENDING)
        assertThat(afterReopen.completedAt).isNull()
    }

    @Test
    fun `deleteTask removes the task from the repository`() = runTest {
        val repo = FakeTaskRepository(
            seed = listOf(task("r1", "Task one"), task("r2", "Task two")),
        )
        val viewModel = TasksViewModel(repo)

        viewModel.deleteTask("r1")

        assertThat(repo.state.value.map { it.id }).containsExactly("r2")
        assertThat(viewModel.uiState.value.tasks.map { it.id }).containsExactly("r2")
    }

    companion object {
        private fun task(
            id: String,
            title: String,
            description: String? = null,
            priority: TaskPriority = TaskPriority.MEDIUM,
            status: TaskStatus = TaskStatus.PENDING,
        ) = TaskItem(
            id = id,
            remoteId = id,
            title = title,
            description = description,
            status = status,
            priority = priority,
            origin = EntityOrigin.IMPORTED,
            governanceState = GovernanceState.CONFIRMED,
            source = IntegrationSource.HUMANOS,
            createdAt = 1L,
            updatedAt = 1L,
        )
    }
}
