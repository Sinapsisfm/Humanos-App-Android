package eco.humanos.android.core.model.task

import kotlinx.serialization.Serializable

/**
 * Lifecycle state of a task.
 */
@Serializable
enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    DONE,
    CANCELLED,
}
