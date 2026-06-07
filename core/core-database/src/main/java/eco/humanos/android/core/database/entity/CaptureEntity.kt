package eco.humanos.android.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import eco.humanos.android.core.model.capture.CaptureItem
import eco.humanos.android.core.model.capture.CaptureType
import eco.humanos.android.core.model.capture.ProcessingStatus
import eco.humanos.android.core.model.common.IntegrationSource
import eco.humanos.android.core.model.common.PrivacyLevel

/**
 * Room persistence mirror of [CaptureItem].
 *
 * Enums are stored as their `.name` strings so the domain model can evolve
 * independently of the on-disk representation. Mapping happens via
 * [toDomain] / [toEntity].
 */
@Entity(tableName = "captures")
data class CaptureEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String?,
    val textContent: String?,
    val filePath: String?,
    val mimeType: String?,
    val fileSizeBytes: Long?,
    val latitude: Double?,
    val longitude: Double?,
    val accuracy: Float?,
    val source: String,
    val privacyLevel: String,
    val processingStatus: String,
    val linkedContextNodeId: String?,
    val createdAt: Long,
    val syncedAt: Long?,
)

fun CaptureEntity.toDomain(): CaptureItem = CaptureItem(
    id = id,
    type = CaptureType.valueOf(type),
    title = title,
    textContent = textContent,
    filePath = filePath,
    mimeType = mimeType,
    fileSizeBytes = fileSizeBytes,
    latitude = latitude,
    longitude = longitude,
    accuracy = accuracy,
    source = IntegrationSource.valueOf(source),
    privacyLevel = PrivacyLevel.valueOf(privacyLevel),
    processingStatus = ProcessingStatus.valueOf(processingStatus),
    linkedContextNodeId = linkedContextNodeId,
    createdAt = createdAt,
    syncedAt = syncedAt,
)

fun CaptureItem.toEntity(): CaptureEntity = CaptureEntity(
    id = id,
    type = type.name,
    title = title,
    textContent = textContent,
    filePath = filePath,
    mimeType = mimeType,
    fileSizeBytes = fileSizeBytes,
    latitude = latitude,
    longitude = longitude,
    accuracy = accuracy,
    source = source.name,
    privacyLevel = privacyLevel.name,
    processingStatus = processingStatus.name,
    linkedContextNodeId = linkedContextNodeId,
    createdAt = createdAt,
    syncedAt = syncedAt,
)
