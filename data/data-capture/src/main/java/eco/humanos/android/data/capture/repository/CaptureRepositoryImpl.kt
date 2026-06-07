package eco.humanos.android.data.capture.repository

import eco.humanos.android.core.database.dao.CaptureDao
import eco.humanos.android.core.database.entity.toDomain
import eco.humanos.android.core.database.entity.toEntity
import eco.humanos.android.core.model.capture.CaptureItem
import eco.humanos.android.core.model.capture.CaptureType
import eco.humanos.android.core.model.capture.ProcessingStatus
import eco.humanos.android.core.model.common.IntegrationSource
import eco.humanos.android.core.model.common.PrivacyLevel
import eco.humanos.android.core.model.common.TraceEvent
import eco.humanos.android.core.observability.TraceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

/**
 * Room-backed implementation of [CaptureRepository].
 *
 * Persists captures through [CaptureDao] and mirrors every write into the
 * audit trail via [TraceRepository], keeping the Capture feature offline-first.
 */
class CaptureRepositoryImpl @Inject constructor(
    private val captureDao: CaptureDao,
    private val traceRepository: TraceRepository,
) : CaptureRepository {

    override fun observeCaptures(): Flow<List<CaptureItem>> =
        captureDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveTextCapture(
        text: String,
        privacyLevel: PrivacyLevel,
    ): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val capture = CaptureItem(
            id = id,
            type = CaptureType.TEXT,
            title = null,
            textContent = text,
            source = IntegrationSource.LOCAL,
            privacyLevel = privacyLevel,
            processingStatus = ProcessingStatus.DONE,
            createdAt = now,
        )

        captureDao.upsert(capture.toEntity())

        traceRepository.logEvent(
            TraceEvent(
                id = UUID.randomUUID().toString(),
                entityType = "capture",
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

    override suspend fun deleteCapture(id: String) {
        captureDao.deleteById(id)
    }
}
