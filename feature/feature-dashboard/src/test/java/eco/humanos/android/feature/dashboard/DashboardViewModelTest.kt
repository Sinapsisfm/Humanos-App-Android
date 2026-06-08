package eco.humanos.android.feature.dashboard

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
 * Pure-JVM unit tests for [DashboardViewModel] using a hand-written fake
 * [TaskRepository] backed by a [MutableStateFlow]. No Robolectric / Android
 * runtime required. Mirrors the fake + runTest + Turbine style used by the
 * data-layer tests (TaskRepositoryImplTest).
 */
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ── Fake ─────────────────────────────────────────────────────────────────

    /**
     * In-memory [TaskRepository]. [observeTasks] returns the backing flow;
     * [syncFromRemote] appends [sampleTasks] to that flow and returns the count;
     * [createTask] appends a single task.
     */
    private class FakeTaskRepository(
        private val sampleTasks: List<TaskItem> = listOf(
            task("r1", "Task one"),
            task("r2", "Task two"),
        ),
        private val syncResult: (List<TaskItem>) -> Result<Int> = { Result.success(it.size) },
    ) : TaskRepository {

        val state = MutableStateFlow<List<TaskItem>>(emptyList())
        var syncCount = 0

        override fun observeTasks(): Flow<List<TaskItem>> = state

        override suspend fun syncFromRemote(): Result<Int> {
            syncCount++
            return syncResult(sampleTasks).onSuccess {
                state.update { current ->
                    val existing = current.map { it.id }.toSet()
                    current + sampleTasks.filterNot { it.id in existing }
                }
            }
        }

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
            state.update { current -> current.filterNot { it.id == task.id } + task }
        }

        override suspend fun deleteTask(id: String) {
            state.update { current -> current.filterNot { it.id == id } }
        }
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    fun `on init collects tasks and syncs so state becomes non-empty and not loading`() = runTest {
        val repo = FakeTaskRepository()

        val viewModel = DashboardViewModel(repo)

        viewModel.uiState.test {
            // With UnconfinedTestDispatcher init runs eagerly; the latest state
            // reflects the synced tasks and the finished loading.
            val state = awaitItem()
            assertThat(state.tasks.map { it.id }).containsExactly("r1", "r2")
            assertThat(state.isLoading).isFalse()
            assertThat(state.error).isNull()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(repo.syncCount).isEqualTo(1)
    }

    @Test
    fun `refresh re-syncs from remote`() = runTest {
        val repo = FakeTaskRepository()
        val viewModel = DashboardViewModel(repo)

        // init already synced once.
        assertThat(repo.syncCount).isEqualTo(1)

        viewModel.refresh()

        assertThat(repo.syncCount).isEqualTo(2)
        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.tasks.map { it.id }).containsExactly("r1", "r2")
    }

    @Test
    fun `refresh failure surfaces error message and ends loading`() = runTest {
        val boom = IllegalStateException("network down")
        val repo = FakeTaskRepository(syncResult = { Result.failure(boom) })

        val viewModel = DashboardViewModel(repo)

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isEqualTo("network down")
        assertThat(state.tasks).isEmpty()
    }

    companion object {
        private fun task(
            id: String,
            title: String,
            description: String? = null,
            priority: TaskPriority = TaskPriority.MEDIUM,
        ) = TaskItem(
            id = id,
            remoteId = id,
            title = title,
            description = description,
            status = TaskStatus.PENDING,
            priority = priority,
            origin = EntityOrigin.IMPORTED,
            governanceState = GovernanceState.CONFIRMED,
            source = IntegrationSource.HUMANOS,
            createdAt = 1L,
            updatedAt = 1L,
        )
    }
}
