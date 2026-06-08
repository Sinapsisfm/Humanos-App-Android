package eco.humanos.android.data.tasks.repository

import eco.humanos.android.core.model.task.TaskItem
import eco.humanos.android.core.model.task.TaskPriority
import kotlinx.coroutines.flow.Flow

/**
 * Domain-facing contract for observing and mutating [TaskItem]s.
 *
 * Bridges the task UI layer to the Room-backed local store using the
 * offline-first cache pattern: the UI always observes Room (the single
 * source of truth), while [syncFromRemote] refreshes Room from the
 * HumanOS backend. Every write also emits a
 * [eco.humanos.android.core.model.common.TraceEvent] through the
 * observability layer so the action lands in the audit trail.
 */
interface TaskRepository {

    /** Observe all stored tasks as a cold flow, newest first. */
    fun observeTasks(): Flow<List<TaskItem>>

    /**
     * Fetch tasks from the remote gateway and upsert them into Room.
     *
     * Remote is treated as the source of fresh data; Room remains the
     * source of truth for the UI. Returns the number of tasks synced on
     * success, or the propagated failure when the remote call fails.
     */
    suspend fun syncFromRemote(): Result<Int>

    /**
     * Persist a new locally-created task and return its generated id.
     *
     * The task is created with PENDING status, MANUAL origin, LOCAL
     * source, and CONFIRMED governance state, then stored and recorded
     * in the audit trail.
     */
    suspend fun createTask(
        title: String,
        description: String? = null,
        priority: TaskPriority = TaskPriority.MEDIUM,
    ): String

    /** Upsert an existing [task], replacing the stored copy. */
    suspend fun updateTask(task: TaskItem)

    /**
     * Mark [task] done/undone, syncing the change to HumanOS so it matches the
     * web. If the task isn't on the server yet (a local-only task created
     * offline), it is promoted — created remotely first — then the status is
     * applied, and the local-only copy is replaced by the server copy. Returns
     * the propagated failure if the remote call fails.
     */
    suspend fun setDone(task: TaskItem, done: Boolean): Result<Unit>

    /** Delete the task with the given [id], if it exists. */
    suspend fun deleteTask(id: String)
}
