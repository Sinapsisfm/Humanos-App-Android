package eco.humanos.android.data.tasks.repository

import eco.humanos.android.core.database.dao.TaskDao
import eco.humanos.android.core.database.entity.toDomain
import eco.humanos.android.core.database.entity.toEntity
import eco.humanos.android.core.model.common.IntegrationSource
import eco.humanos.android.core.model.common.TraceEvent
import eco.humanos.android.core.model.context.GovernanceState
import eco.humanos.android.core.model.task.EntityOrigin
import eco.humanos.android.core.model.task.TaskItem
import eco.humanos.android.core.model.task.TaskPriority
import eco.humanos.android.core.model.task.TaskStatus
import eco.humanos.android.core.observability.TraceRepository
import eco.humanos.android.integrations.humanos.HumanosGateway
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

/**
 * Room-backed, offline-first implementation of [TaskRepository].
 *
 * The UI observes Room through [observeTasks]; [syncFromRemote] pulls from
 * the [HumanosGateway] and upserts the result into Room, so observers react
 * automatically. Every write mirrors into the audit trail via
 * [TraceRepository].
 */
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val humanosGateway: HumanosGateway,
    private val traceRepository: TraceRepository,
) : TaskRepository {

    override fun observeTasks(): Flow<List<TaskItem>> =
        taskDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun syncFromRemote(): Result<Int> =
        humanosGateway.fetchTasks().map { tasks ->
            tasks.forEach { taskDao.upsert(it.toEntity()) }

            traceRepository.logEvent(
                TraceEvent(
                    id = UUID.randomUUID().toString(),
                    entityType = "task",
                    entityId = "*",
                    action = "tasks_synced",
                    source = IntegrationSource.HUMANOS,
                    userId = "local-user",
                    metadata = "count=${tasks.size}",
                    timestamp = System.currentTimeMillis(),
                ),
            )

            tasks.size
        }

    override suspend fun createTask(
        title: String,
        description: String?,
        priority: TaskPriority,
    ): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val task = TaskItem(
            id = id,
            title = title,
            description = description,
            status = TaskStatus.PENDING,
            priority = priority,
            origin = EntityOrigin.MANUAL,
            governanceState = GovernanceState.CONFIRMED,
            source = IntegrationSource.LOCAL,
            createdAt = now,
            updatedAt = now,
        )

        taskDao.upsert(task.toEntity())

        traceRepository.logEvent(
            TraceEvent(
                id = UUID.randomUUID().toString(),
                entityType = "task",
                entityId = id,
                action = "created",
                source = IntegrationSource.LOCAL,
                userId = "local-user",
                metadata = null,
                timestamp = now,
            ),
        )

        return id
    }

    override suspend fun updateTask(task: TaskItem) {
        taskDao.upsert(task.toEntity())
    }

    override suspend fun deleteTask(id: String) {
        taskDao.deleteById(id)
    }
}
