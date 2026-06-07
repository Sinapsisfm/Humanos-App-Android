package eco.humanos.android.core.model.task

import eco.humanos.android.core.model.common.IntegrationSource
import eco.humanos.android.core.model.context.GovernanceState
import kotlinx.serialization.Serializable

/**
 * A user task that can originate from manual entry, AI inference,
 * QueBot conversations, or external imports.
 *
 * Tasks are first-class citizens in the context graph and can be linked
 * to context nodes for richer AI reasoning about workload and priorities.
 *
 * @property remoteId Server-side ID after first sync.
 * @property tags Free-form labels for filtering and grouping.
 * @property recurrence iCal RRULE string for repeating tasks, null if one-off.
 * @property linkedContextNodeId Optional link to a related context node.
 * @property createdAt Epoch millis when the task was created.
 * @property updatedAt Epoch millis of last local mutation.
 * @property syncedAt Epoch millis of last successful sync.
 */
@Serializable
data class TaskItem(
    val id: String,
    val remoteId: String? = null,
    val title: String,
    val description: String? = null,
    val status: TaskStatus,
    val priority: TaskPriority,
    val dueDate: Long? = null,
    val completedAt: Long? = null,
    val tags: List<String> = emptyList(),
    val origin: EntityOrigin,
    val governanceState: GovernanceState,
    val source: IntegrationSource,
    val recurrence: String? = null,
    val linkedContextNodeId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
)
