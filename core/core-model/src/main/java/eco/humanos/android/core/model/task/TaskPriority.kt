package eco.humanos.android.core.model.task

import kotlinx.serialization.Serializable

/**
 * Urgency level of a task, used for sorting and notification rules.
 */
@Serializable
enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}
