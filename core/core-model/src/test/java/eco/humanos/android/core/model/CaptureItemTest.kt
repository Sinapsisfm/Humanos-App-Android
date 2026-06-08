package eco.humanos.android.core.model

import com.google.common.truth.Truth.assertThat
import eco.humanos.android.core.model.capture.CaptureItem
import eco.humanos.android.core.model.capture.CaptureType
import eco.humanos.android.core.model.capture.ProcessingStatus
import eco.humanos.android.core.model.common.IntegrationSource
import eco.humanos.android.core.model.common.PrivacyLevel
import kotlinx.serialization.json.Json
import org.junit.Test

/**
 * Sanity tests for [CaptureItem] and its enums:
 * - the enum value sets are stable, and
 * - the model round-trips through kotlinx.serialization JSON unchanged.
 */
class CaptureItemTest {

    private val json = Json

    @Test
    fun `CaptureType exposes the expected values`() {
        assertThat(CaptureType.entries.map { it.name })
            .containsExactly("TEXT", "VOICE", "PHOTO", "FILE", "SCREENSHOT")
            .inOrder()
    }

    @Test
    fun `ProcessingStatus exposes the expected values`() {
        assertThat(ProcessingStatus.entries.map { it.name })
            .containsExactly("PENDING", "PROCESSING", "DONE", "FAILED")
            .inOrder()
    }

    @Test
    fun `IntegrationSource exposes the expected values`() {
        assertThat(IntegrationSource.entries.map { it.name })
            .containsExactly("HUMANOS", "QUEBOT", "LOCAL", "HEALTH_CONNECT", "FIREBASE")
            .inOrder()
    }

    @Test
    fun `PrivacyLevel exposes the expected values`() {
        assertThat(PrivacyLevel.entries.map { it.name })
            .containsExactly("PUBLIC", "PRIVATE", "VAULT")
            .inOrder()
    }

    @Test
    fun `fully populated CaptureItem round-trips through JSON`() {
        val original = CaptureItem(
            id = "capture-001",
            type = CaptureType.VOICE,
            title = "Voice memo",
            textContent = "transcript",
            filePath = "/data/voice.m4a",
            mimeType = "audio/mp4",
            fileSizeBytes = 99_999L,
            latitude = -33.45,
            longitude = -70.66,
            accuracy = 12.5f,
            source = IntegrationSource.HUMANOS,
            privacyLevel = PrivacyLevel.PRIVATE,
            processingStatus = ProcessingStatus.DONE,
            linkedContextNodeId = "node-1",
            createdAt = 1_700_000_000_000L,
            syncedAt = 1_700_000_100_000L,
        )

        val encoded = json.encodeToString(CaptureItem.serializer(), original)
        val decoded = json.decodeFromString(CaptureItem.serializer(), encoded)

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `CaptureItem with default null fields round-trips through JSON`() {
        val original = CaptureItem(
            id = "capture-002",
            type = CaptureType.TEXT,
            source = IntegrationSource.LOCAL,
            privacyLevel = PrivacyLevel.PUBLIC,
            processingStatus = ProcessingStatus.PENDING,
            createdAt = 0L,
        )

        val encoded = json.encodeToString(CaptureItem.serializer(), original)
        val decoded = json.decodeFromString(CaptureItem.serializer(), encoded)

        assertThat(decoded).isEqualTo(original)
        assertThat(decoded.title).isNull()
        assertThat(decoded.syncedAt).isNull()
    }

    @Test
    fun `enum values serialize by name`() {
        val item = CaptureItem(
            id = "capture-003",
            type = CaptureType.SCREENSHOT,
            source = IntegrationSource.FIREBASE,
            privacyLevel = PrivacyLevel.VAULT,
            processingStatus = ProcessingStatus.FAILED,
            createdAt = 0L,
        )

        val encoded = json.encodeToString(CaptureItem.serializer(), item)

        assertThat(encoded).contains("\"type\":\"SCREENSHOT\"")
        assertThat(encoded).contains("\"source\":\"FIREBASE\"")
        assertThat(encoded).contains("\"privacyLevel\":\"VAULT\"")
        assertThat(encoded).contains("\"processingStatus\":\"FAILED\"")
    }
}
