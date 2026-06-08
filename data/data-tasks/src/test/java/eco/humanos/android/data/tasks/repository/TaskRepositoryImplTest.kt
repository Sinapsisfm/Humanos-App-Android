package eco.humanos.android.data.tasks.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import eco.humanos.android.core.database.dao.TaskDao
import eco.humanos.android.core.database.entity.TaskEntity
import eco.humanos.android.core.model.common.IntegrationSource
import eco.humanos.android.core.model.common.TraceEvent
import eco.humanos.android.core.model.context.GovernanceState
import eco.humanos.android.core.model.task.EntityOrigin
import eco.humanos.android.core.model.task.TaskItem
import eco.humanos.android.core.model.task.TaskPriority
import eco.humanos.android.core.model.task.TaskStatus
import eco.humanos.android.core.observability.TraceRepository
import eco.humanos.android.integrations.humanos.DailyReviewDto
import eco.humanos.android.integrations.humanos.HumanosGateway
import eco.humanos.android.core.model.auth.HumanOSSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Pure-JVM unit tests for [TaskRepositoryImpl] using hand-written fakes for the
 * DAO, the HumanOS gateway, and the trace repository. No Robolectric / Android
 * runtime required.
 */
class TaskRepositoryImplTest {

    // ── Fakes ────────────────────────────────────────────────────────────────

    /** In-memory [TaskDao] backed by a [MutableStateFlow] so emissions are observable. */
    private class FakeTaskDao : TaskDao {
        private val state = MutableStateFlow<List<TaskEntity>>(emptyList())

        override fun observeAll(): Flow<List<TaskEntity>> = state

        override fun observeByStatus(status: String): Flow<List<TaskEntity>> =
            state.map { list -> list.filter { it.status == status } }

        override suspend fun getById(id: String): TaskEntity? =
            state.value.firstOrNull { it.id == id }

        override suspend fun upsert(entity: TaskEntity) {
            state.update { current ->
                current.filterNot { it.id == entity.id } + entity
            }
        }

        override suspend fun deleteById(id: String) {
            state.update { current -> current.filterNot { it.id == id } }
        }
    }

    /** Fake gateway that returns a canned [fetchTasks] result. */
    private class FakeHumanosGateway(
        private val tasksResult: Result<List<TaskItem>>,
    ) : HumanosGateway {
        override suspend fun exchangeFirebaseToken(firebaseIdToken: String): Result<HumanOSSession> =
            error("not used in these tests")

        override suspend fun fetchTasks(status: String?): Result<List<TaskItem>> = tasksResult

        override suspend fun createTask(
            title: String,
            description: String?,
            priority: TaskPriority,
        ): Result<TaskItem> = error("not used in these tests")

        override suspend fun fetchDailyReview(): Result<DailyReviewDto> =
            error("not used in these tests")

        override suspend fun checkConnectivity(): Boolean = true
    }

    /** Fake trace repository that records logged events in memory. */
    private class FakeTraceRepository : TraceRepository {
        val logged = mutableListOf<TraceEvent>()

        override suspend fun logEvent(event: TraceEvent) {
            logged += event
        }

        override fun observeEventsForEntity(entityType: String, entityId: String) =
            error("not used in these tests")

        override suspend fun getRecentEvents(limit: Int): List<TraceEvent> = logged

        override suspend fun pruneEventsBefore(timestampMillis: Long) = Unit
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun remoteTask(id: String, title: String) = TaskItem(
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

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    fun `syncFromRemote upserts remote tasks into Room and they appear in observeTasks`() = runTest {
        val remote = listOf(
            remoteTask("r1", "Task one"),
            remoteTask("r2", "Task two"),
            remoteTask("r3", "Task three"),
        )
        val trace = FakeTraceRepository()
        val repo = TaskRepositoryImpl(
            taskDao = FakeTaskDao(),
            humanosGateway = FakeHumanosGateway(Result.success(remote)),
            traceRepository = trace,
        )

        val result = repo.syncFromRemote()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(3)

        repo.observeTasks().test {
            val emitted = awaitItem()
            assertThat(emitted.map { it.id }).containsExactly("r1", "r2", "r3")
            cancelAndIgnoreRemainingEvents()
        }

        // A sync trace event was recorded with the count metadata.
        assertThat(trace.logged).hasSize(1)
        assertThat(trace.logged.single().action).isEqualTo("tasks_synced")
        assertThat(trace.logged.single().metadata).isEqualTo("count=3")
    }

    @Test
    fun `syncFromRemote propagates failure and writes nothing`() = runTest {
        val boom = IllegalStateException("network down")
        val trace = FakeTraceRepository()
        val repo = TaskRepositoryImpl(
            taskDao = FakeTaskDao(),
            humanosGateway = FakeHumanosGateway(Result.failure(boom)),
            traceRepository = trace,
        )

        val result = repo.syncFromRemote()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(boom)
        assertThat(repo.observeTasks().first()).isEmpty()
        assertThat(trace.logged).isEmpty()
    }

    @Test
    fun `createTask adds a PENDING MANUAL LOCAL task observable via observeTasks`() = runTest {
        val trace = FakeTraceRepository()
        val repo = TaskRepositoryImpl(
            taskDao = FakeTaskDao(),
            humanosGateway = FakeHumanosGateway(Result.success(emptyList())),
            traceRepository = trace,
        )

        val id = repo.createTask(title = "Buy groceries", description = "milk and eggs")

        repo.observeTasks().test {
            val emitted = awaitItem()
            assertThat(emitted).hasSize(1)
            val task = emitted.single()
            assertThat(task.id).isEqualTo(id)
            assertThat(task.title).isEqualTo("Buy groceries")
            assertThat(task.description).isEqualTo("milk and eggs")
            assertThat(task.status).isEqualTo(TaskStatus.PENDING)
            assertThat(task.origin).isEqualTo(EntityOrigin.MANUAL)
            assertThat(task.source).isEqualTo(IntegrationSource.LOCAL)
            assertThat(task.governanceState).isEqualTo(GovernanceState.CONFIRMED)
            assertThat(task.priority).isEqualTo(TaskPriority.MEDIUM)
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(trace.logged.single().action).isEqualTo("created")
    }

    @Test
    fun `deleteTask removes a previously created task`() = runTest {
        val repo = TaskRepositoryImpl(
            taskDao = FakeTaskDao(),
            humanosGateway = FakeHumanosGateway(Result.success(emptyList())),
            traceRepository = FakeTraceRepository(),
        )

        val id = repo.createTask(title = "Temp")
        assertThat(repo.observeTasks().first()).hasSize(1)

        repo.deleteTask(id)

        assertThat(repo.observeTasks().first()).isEmpty()
    }
}
