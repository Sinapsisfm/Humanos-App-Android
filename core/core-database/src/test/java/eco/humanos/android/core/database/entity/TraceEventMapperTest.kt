package eco.humanos.android.core.database.entity

import com.google.common.truth.Truth.assertThat
import eco.humanos.android.core.model.common.IntegrationSource
import eco.humanos.android.core.model.common.TraceEvent
import org.junit.Test

/**
 * Round-trip tests for [TraceEvent] <-> [TraceEventEntity] mapping.
 */
class TraceEventMapperTest {

    @Test
    fun `fully populated TraceEvent survives round-trip`() {
        val original = TraceEvent(
            id = "trace-001",
            entityType = "TaskItem",
            entityId = "task-001",
            action = "UPDATE",
            source = IntegrationSource.HUMANOS,
            userId = "user-42",
            metadata = """{"field":"status","from":"PENDING","to":"DONE"}""",
            timestamp = 1_700_000_000_000L,
        )

        val roundTripped = original.toEntity().toDomain()

        assertThat(roundTripped).isEqualTo(original)
    }

    @Test
    fun `TraceEvent with null metadata survives round-trip`() {
        val original = TraceEvent(
            id = "trace-002",
            entityType = "CaptureItem",
            entityId = "capture-002",
            action = "CREATE",
            source = IntegrationSource.LOCAL,
            userId = "user-42",
            metadata = null,
            timestamp = 1_700_000_000_000L,
        )

        val roundTripped = original.toEntity().toDomain()

        assertThat(roundTripped).isEqualTo(original)
        assertThat(roundTripped.metadata).isNull()
    }

    @Test
    fun `toEntity stores source as its name string`() {
        val entity = TraceEvent(
            id = "trace-003",
            entityType = "TaskItem",
            entityId = "task-003",
            action = "DELETE",
            source = IntegrationSource.FIREBASE,
            userId = "user-1",
            timestamp = 0L,
        ).toEntity()

        assertThat(entity.source).isEqualTo("FIREBASE")
    }

    @Test
    fun `every IntegrationSource round-trips`() {
        for (source in IntegrationSource.entries) {
            val event = TraceEvent(
                id = "trace-$source",
                entityType = "Entity",
                entityId = "id",
                action = "ACTION",
                source = source,
                userId = "user",
                timestamp = 1L,
            )

            assertThat(event.toEntity().toDomain().source).isEqualTo(source)
        }
    }
}
