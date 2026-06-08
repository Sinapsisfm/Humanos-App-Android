package eco.humanos.android.core.database.entity

import com.google.common.truth.Truth.assertThat
import eco.humanos.android.core.model.common.IntegrationSource
import eco.humanos.android.core.model.context.GovernanceState
import eco.humanos.android.core.model.task.EntityOrigin
import eco.humanos.android.core.model.task.TaskItem
import eco.humanos.android.core.model.task.TaskPriority
import eco.humanos.android.core.model.task.TaskStatus
import org.junit.Test

/**
 * Round-trip tests for [TaskItem] <-> [TaskEntity] mapping.
 *
 * Exercises the `tags: List<String>` field (stored as JSON via a TypeConverter
 * at the Room layer) and preservation of every enum-backed column.
 */
class TaskMapperTest {

    @Test
    fun `fully populated TaskItem with multi-element tags survives round-trip`() {
        val original = TaskItem(
            id = "task-001",
            remoteId = "remote-001",
            title = "Prepare inspection report",
            description = "Compile evidence and submit to SAG",
            status = TaskStatus.IN_PROGRESS,
            priority = TaskPriority.HIGH,
            dueDate = 1_700_100_000_000L,
            completedAt = null,
            tags = listOf("urgent", "field", "sag", "q2"),
            origin = EntityOrigin.IMPORTED,
            governanceState = GovernanceState.CONFIRMED,
            source = IntegrationSource.QUEBOT,
            recurrence = "FREQ=WEEKLY;BYDAY=MO",
            linkedContextNodeId = "context-node-12",
            createdAt = 1_700_000_000_000L,
            updatedAt = 1_700_050_000_000L,
            syncedAt = 1_700_060_000_000L,
        )

        val roundTripped = original.toEntity().toDomain()

        assertThat(roundTripped).isEqualTo(original)
        assertThat(roundTripped.tags).containsExactly("urgent", "field", "sag", "q2").inOrder()
    }

    @Test
    fun `TaskItem with empty tags and null optionals survives round-trip`() {
        val original = TaskItem(
            id = "task-002",
            remoteId = null,
            title = "Quick note",
            description = null,
            status = TaskStatus.PENDING,
            priority = TaskPriority.LOW,
            dueDate = null,
            completedAt = null,
            tags = emptyList(),
            origin = EntityOrigin.MANUAL,
            governanceState = GovernanceState.DRAFT,
            source = IntegrationSource.LOCAL,
            recurrence = null,
            linkedContextNodeId = null,
            createdAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_000_000L,
            syncedAt = null,
        )

        val roundTripped = original.toEntity().toDomain()

        assertThat(roundTripped).isEqualTo(original)
        assertThat(roundTripped.tags).isEmpty()
    }

    @Test
    fun `single-element tag list survives round-trip`() {
        val original = baseTask().copy(id = "task-003", tags = listOf("solo"))

        val roundTripped = original.toEntity().toDomain()

        assertThat(roundTripped.tags).containsExactly("solo")
    }

    @Test
    fun `mapper preserves the tags list reference passed through unchanged`() {
        val tags = listOf("a", "b")
        val entity = baseTask().copy(tags = tags).toEntity()

        // The entity stores the list directly; Room's TypeConverter serializes it.
        assertThat(entity.tags).isEqualTo(tags)
    }

    @Test
    fun `toEntity stores enums as their name strings`() {
        val entity = baseTask().copy(
            status = TaskStatus.CANCELLED,
            priority = TaskPriority.CRITICAL,
            origin = EntityOrigin.INFERRED,
            governanceState = GovernanceState.DUPLICATE_CANDIDATE,
            source = IntegrationSource.FIREBASE,
        ).toEntity()

        assertThat(entity.status).isEqualTo("CANCELLED")
        assertThat(entity.priority).isEqualTo("CRITICAL")
        assertThat(entity.origin).isEqualTo("INFERRED")
        assertThat(entity.governanceState).isEqualTo("DUPLICATE_CANDIDATE")
        assertThat(entity.source).isEqualTo("FIREBASE")
    }

    @Test
    fun `every TaskStatus round-trips`() {
        for (status in TaskStatus.entries) {
            val item = baseTask().copy(status = status)
            assertThat(item.toEntity().toDomain().status).isEqualTo(status)
        }
    }

    @Test
    fun `every TaskPriority round-trips`() {
        for (priority in TaskPriority.entries) {
            val item = baseTask().copy(priority = priority)
            assertThat(item.toEntity().toDomain().priority).isEqualTo(priority)
        }
    }

    @Test
    fun `every EntityOrigin round-trips`() {
        for (origin in EntityOrigin.entries) {
            val item = baseTask().copy(origin = origin)
            assertThat(item.toEntity().toDomain().origin).isEqualTo(origin)
        }
    }

    @Test
    fun `every GovernanceState round-trips`() {
        for (state in GovernanceState.entries) {
            val item = baseTask().copy(governanceState = state)
            assertThat(item.toEntity().toDomain().governanceState).isEqualTo(state)
        }
    }

    private fun baseTask() = TaskItem(
        id = "task-base",
        title = "Base task",
        status = TaskStatus.PENDING,
        priority = TaskPriority.MEDIUM,
        origin = EntityOrigin.MANUAL,
        governanceState = GovernanceState.CONFIRMED,
        source = IntegrationSource.HUMANOS,
        createdAt = 1L,
        updatedAt = 2L,
    )
}
