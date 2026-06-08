package eco.humanos.android.feature.dashboard

import android.content.Context
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import eco.humanos.android.core.model.auth.AuthState
import eco.humanos.android.core.model.auth.HumanOSSession
import eco.humanos.android.core.model.auth.HumanosLinkState
import eco.humanos.android.core.model.common.IntegrationSource
import eco.humanos.android.core.model.context.GovernanceState
import eco.humanos.android.core.model.task.EntityOrigin
import eco.humanos.android.core.model.task.TaskItem
import eco.humanos.android.core.model.task.TaskPriority
import eco.humanos.android.core.model.task.TaskStatus
import eco.humanos.android.data.auth.AuthRepository
import eco.humanos.android.data.auth.GoogleCredentialManager
import eco.humanos.android.data.tasks.repository.TaskRepository
import eco.humanos.android.integrations.humanos.DailyReviewDto
import eco.humanos.android.integrations.humanos.HumanosGateway
import eco.humanos.android.integrations.humanos.dto.CheckInDto
import eco.humanos.android.integrations.humanos.dto.CheckInsEnvelope
import eco.humanos.android.integrations.humanos.dto.MobileSnapshotDto
import eco.humanos.android.integrations.humanos.dto.PersonDto
import eco.humanos.android.integrations.humanos.dto.SnapshotCountsDto
import eco.humanos.android.integrations.humanos.dto.SnapshotUserDto
import eco.humanos.android.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Pure-JVM unit tests for [DashboardViewModel]. Tasks come from a fake
 * [TaskRepository]; greeting / counts / today's check-in come from a fake
 * [HumanosGateway] snapshot. No Robolectric / Android runtime required.
 */
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ── Fakes ────────────────────────────────────────────────────────────────

    private class FakeTaskRepository(
        private val sampleTasks: List<TaskItem> = listOf(task("r1", "Task one"), task("r2", "Task two")),
    ) : TaskRepository {
        val state = MutableStateFlow<List<TaskItem>>(emptyList())
        var syncCount = 0

        override fun observeTasks(): Flow<List<TaskItem>> = state
        override suspend fun syncFromRemote(): Result<Int> {
            syncCount++
            state.update { current ->
                val ids = current.map { it.id }.toSet()
                current + sampleTasks.filterNot { it.id in ids }
            }
            return Result.success(sampleTasks.size)
        }
        override suspend fun createTask(title: String, description: String?, priority: TaskPriority): String = "x"
        override suspend fun updateTask(task: TaskItem) = Unit
        override suspend fun deleteTask(id: String) = Unit
    }

    private class FakeGateway(
        private val snapshot: Result<MobileSnapshotDto>,
    ) : HumanosGateway {
        override suspend fun exchangeFirebaseToken(firebaseIdToken: String): Result<HumanOSSession> = error("unused")
        override suspend fun fetchTasks(status: String?): Result<List<TaskItem>> = Result.success(emptyList())
        override suspend fun createTask(title: String, description: String?, priority: TaskPriority): Result<TaskItem> = error("unused")
        override suspend fun updateTaskStatus(taskId: String, status: String): Result<TaskItem> = error("unused")
        override suspend fun fetchDailyReview(): Result<DailyReviewDto> = error("unused")
        override suspend fun fetchSnapshot(): Result<MobileSnapshotDto> = snapshot
        override suspend fun fetchCheckIns(): Result<CheckInsEnvelope> = Result.success(CheckInsEnvelope())
        override suspend fun submitCheckIn(energy: Int, mood: Int, stress: Int, perceivedLoad: Int?, note: String?): Result<CheckInDto> =
            Result.success(CheckInDto(id = "c1", energy = energy, mood = mood, stress = stress))
        override suspend fun fetchPerson(): Result<PersonDto> = Result.success(PersonDto(id = "p1"))
        override suspend fun checkConnectivity(): Boolean = true
    }

    private class FakeAuthRepository : AuthRepository {
        override val humanosLinkState: StateFlow<HumanosLinkState> = MutableStateFlow(HumanosLinkState.Unknown)
        override fun observeAuthState(): Flow<AuthState> = flowOf(AuthState.Unauthenticated)
        override suspend fun signInWithGoogle(idToken: String): Result<AuthState.Authenticated> = error("unused")
        override suspend fun refreshHumanosToken(): Result<HumanOSSession> = error("unused")
        override suspend fun signOut() = Unit
        override suspend fun getFirebaseToken(): String? = null
        override suspend fun getHumanosToken(): String? = null
    }

    private class FakeCredentialManager : GoogleCredentialManager {
        override suspend fun getGoogleIdToken(activityContext: Context): Result<String> = error("unused")
    }

    private fun viewModel(
        repo: TaskRepository = FakeTaskRepository(),
        snapshot: Result<MobileSnapshotDto> = Result.success(sampleSnapshot()),
    ) = DashboardViewModel(repo, FakeGateway(snapshot), FakeAuthRepository(), FakeCredentialManager())

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    fun `on init collects tasks and ends loading`() = runTest {
        val repo = FakeTaskRepository()
        val vm = viewModel(repo = repo)

        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.tasks.map { it.id }).containsExactly("r1", "r2")
            assertThat(state.isLoading).isFalse()
            assertThat(state.error).isNull()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(repo.syncCount).isAtLeast(1)
    }

    @Test
    fun `snapshot populates greeting name and counts`() = runTest {
        val vm = viewModel()
        val state = vm.uiState.value
        assertThat(state.userName).isEqualTo("Felipe")
        assertThat(state.greeting).contains("Felipe")
        assertThat(state.tasksOpen).isEqualTo(5)
        assertThat(state.tasksOverdue).isEqualTo(2)
    }

    @Test
    fun `snapshot failure surfaces error and ends loading`() = runTest {
        val vm = viewModel(snapshot = Result.failure(IllegalStateException("network down")))
        val state = vm.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isEqualTo("network down")
    }

    companion object {
        private fun task(id: String, title: String) = TaskItem(
            id = id,
            remoteId = id,
            title = title,
            status = TaskStatus.PENDING,
            priority = TaskPriority.MEDIUM,
            origin = EntityOrigin.IMPORTED,
            governanceState = GovernanceState.CONFIRMED,
            source = IntegrationSource.HUMANOS,
            createdAt = 1L,
            updatedAt = 1L,
        )

        private fun sampleSnapshot() = MobileSnapshotDto(
            generatedAt = "2026-06-08T08:00:00.000Z",
            user = SnapshotUserDto(firstName = "Felipe", loadScore = 60, lifeAdminScore = 40),
            counts = SnapshotCountsDto(tasksOpen = 5, tasksOverdue = 2, tasksDueToday = 1),
            checkInToday = null,
            tasks = emptyList(),
        )
    }
}
