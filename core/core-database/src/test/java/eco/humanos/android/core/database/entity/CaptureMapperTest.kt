package eco.humanos.android.core.database.entity

import com.google.common.truth.Truth.assertThat
import eco.humanos.android.core.model.capture.CaptureItem
import eco.humanos.android.core.model.capture.CaptureType
import eco.humanos.android.core.model.capture.ProcessingStatus
import eco.humanos.android.core.model.common.IntegrationSource
import eco.humanos.android.core.model.common.PrivacyLevel
import org.junit.Test

/**
 * Round-trip tests for [CaptureItem] <-> [CaptureEntity] mapping.
 *
 * `toEntity().toDomain()` must reproduce the original domain object exactly,
 * including enum values and nullable optional fields.
 */
class CaptureMapperTest {

    @Test
    fun `fully populated CaptureItem survives round-trip`() {
        val original = CaptureItem(
            id = "capture-001",
            type = CaptureType.PHOTO,
            title = "Field photo",
            textContent = "A note attached to the photo",
            filePath = "/data/captures/capture-001.jpg",
            mimeType = "image/jpeg",
            fileSizeBytes = 204_800L,
            latitude = -35.426_5,
            longitude = -71.655_4,
            accuracy = 4.5f,
            source = IntegrationSource.HEALTH_CONNECT,
            privacyLevel = PrivacyLevel.VAULT,
            processingStatus = ProcessingStatus.PROCESSING,
            linkedContextNodeId = "context-node-77",
            createdAt = 1_700_000_000_000L,
            syncedAt = 1_700_000_500_000L,
        )

        val roundTripped = original.toEntity().toDomain()

        assertThat(roundTripped).isEqualTo(original)
    }

    @Test
    fun `CaptureItem with null optional fields survives round-trip`() {
        val original = CaptureItem(
            id = "capture-002",
            type = CaptureType.TEXT,
            title = null,
            textContent = null,
            filePath = null,
            mimeType = null,
            fileSizeBytes = null,
            latitude = null,
            longitude = null,
            accuracy = null,
            source = IntegrationSource.LOCAL,
            privacyLevel = PrivacyLevel.PRIVATE,
            processingStatus = ProcessingStatus.PENDING,
            linkedContextNodeId = null,
            createdAt = 1_700_000_000_000L,
            syncedAt = null,
        )

        val roundTripped = original.toEntity().toDomain()

        assertThat(roundTripped).isEqualTo(original)
    }

    @Test
    fun `toEntity stores enums as their name strings`() {
        val item = CaptureItem(
            id = "capture-003",
            type = CaptureType.SCREENSHOT,
            source = IntegrationSource.QUEBOT,
            privacyLevel = PrivacyLevel.PUBLIC,
            processingStatus = ProcessingStatus.DONE,
            createdAt = 0L,
        )

        val entity = item.toEntity()

        assertThat(entity.type).isEqualTo("SCREENSHOT")
        assertThat(entity.source).isEqualTo("QUEBOT")
        assertThat(entity.privacyLevel).isEqualTo("PUBLIC")
        assertThat(entity.processingStatus).isEqualTo("DONE")
    }

    @Test
    fun `every CaptureType round-trips through the mapper`() {
        for (type in CaptureType.entries) {
            val item = CaptureItem(
                id = "capture-$type",
                type = type,
                source = IntegrationSource.HUMANOS,
                privacyLevel = PrivacyLevel.PRIVATE,
                processingStatus = ProcessingStatus.PENDING,
                createdAt = 1L,
            )

            assertThat(item.toEntity().toDomain().type).isEqualTo(type)
        }
    }
}
