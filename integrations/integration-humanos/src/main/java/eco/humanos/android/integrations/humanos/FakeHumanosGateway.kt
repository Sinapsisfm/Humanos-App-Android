package eco.humanos.android.integrations.humanos

import eco.humanos.android.core.model.auth.HumanOSSession
import eco.humanos.android.core.model.common.IntegrationSource
import eco.humanos.android.core.model.context.GovernanceState
import eco.humanos.android.core.model.task.EntityOrigin
import eco.humanos.android.core.model.task.TaskItem
import eco.humanos.android.core.model.task.TaskPriority
import eco.humanos.android.core.model.task.TaskStatus
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * In-memory fake of [HumanosGateway] for development, previews, and tests.
 *
 * Returns realistic Chilean/Spanish sample data with simulated network
 * latency. No actual HTTP calls are made.
 */
class FakeHumanosGateway @Inject constructor() : HumanosGateway {

    private val now = System.currentTimeMillis()

    private val sampleTasks = listOf(
        TaskItem(
            id = "task-001",
            remoteId = "remote-001",
            title = "Revisar contrato de arriendo local Talca",
            description = "Contrato vence el 30 de junio. Revisar clausulas de renovacion.",
            status = TaskStatus.PENDING,
            priority = TaskPriority.HIGH,
            dueDate = now + 7 * 24 * 3600 * 1000L,
            tags = listOf("legal", "talca"),
            origin = EntityOrigin.MANUAL,
            governanceState = GovernanceState.CONFIRMED,
            source = IntegrationSource.HUMANOS,
            createdAt = now - 2 * 24 * 3600 * 1000L,
            updatedAt = now - 1 * 24 * 3600 * 1000L,
            syncedAt = now - 1 * 24 * 3600 * 1000L,
        ),
        TaskItem(
            id = "task-002",
            remoteId = "remote-002",
            title = "Preparar presentacion para directorio",
            description = "Incluir metricas Q2 y proyeccion Q3. Formato PPTX.",
            status = TaskStatus.IN_PROGRESS,
            priority = TaskPriority.CRITICAL,
            dueDate = now + 3 * 24 * 3600 * 1000L,
            tags = listOf("directorio", "presentacion"),
            origin = EntityOrigin.MANUAL,
            governanceState = GovernanceState.CONFIRMED,
            source = IntegrationSource.HUMANOS,
            createdAt = now - 5 * 24 * 3600 * 1000L,
            updatedAt = now,
            syncedAt = now,
        ),
        TaskItem(
            id = "task-003",
            remoteId = "remote-003",
            title = "Coordinar visita terreno sector sur",
            description = null,
            status = TaskStatus.PENDING,
            priority = TaskPriority.MEDIUM,
            tags = listOf("terreno"),
            origin = EntityOrigin.IMPORTED,
            governanceState = GovernanceState.CONFIRMED,
            source = IntegrationSource.QUEBOT,
            createdAt = now - 1 * 24 * 3600 * 1000L,
            updatedAt = now - 1 * 24 * 3600 * 1000L,
            syncedAt = now - 1 * 24 * 3600 * 1000L,
        ),
    )

    override suspend fun exchangeFirebaseToken(firebaseIdToken: String): Result<HumanOSSession> {
        delay(SIMULATED_LATENCY_MS)
        return Result.success(
            HumanOSSession(
                userId = "firebase-uid-fake-001",
                personId = "person-001",
                bearerToken = "fake-bearer-token-for-dev",
                expiresAt = now + 3600 * 1000L,
            ),
        )
    }

    override suspend fun fetchTasks(status: String?): Result<List<TaskItem>> {
        delay(SIMULATED_LATENCY_MS)
        val filtered = if (status != null) {
            sampleTasks.filter { it.status.name == status }
        } else {
            sampleTasks
        }
        return Result.success(filtered)
    }

    override suspend fun createTask(
        title: String,
        description: String?,
        priority: TaskPriority,
    ): Result<TaskItem> {
        delay(SIMULATED_LATENCY_MS)
        val task = TaskItem(
            id = "task-${System.currentTimeMillis()}",
            remoteId = "remote-${System.currentTimeMillis()}",
            title = title,
            description = description,
            status = TaskStatus.PENDING,
            priority = priority,
            origin = EntityOrigin.MANUAL,
            governanceState = GovernanceState.DRAFT,
            source = IntegrationSource.LOCAL,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        return Result.success(task)
    }

    override suspend fun fetchDailyReview(): Result<DailyReviewDto> {
        delay(SIMULATED_LATENCY_MS)
        return Result.success(
            DailyReviewDto(
                date = "2026-06-07",
                summary = "Tienes 3 tareas pendientes. La presentacion para directorio es critica y vence en 3 dias.",
                pendingTaskCount = 3,
                urgentItems = listOf(
                    "Presentacion directorio (vence 10 jun)",
                    "Contrato arriendo Talca (vence 30 jun)",
                ),
            ),
        )
    }

    override suspend fun checkConnectivity(): Boolean {
        delay(SIMULATED_LATENCY_MS / 2)
        return true
    }

    private companion object {
        const val SIMULATED_LATENCY_MS = 300L
    }
}
