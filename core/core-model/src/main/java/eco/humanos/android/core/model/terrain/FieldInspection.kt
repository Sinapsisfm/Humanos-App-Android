package eco.humanos.android.core.model.terrain

import kotlinx.serialization.Serializable

/**
 * A field inspection, site visit, audit, or incident report.
 *
 * Represents on-the-ground work performed by empresa/terrain users.
 * Inspections collect structured observations and [EvidenceItem] attachments,
 * and can be linked into the context graph for AI-powered follow-up.
 *
 * @property type The kind of field activity: "inspection", "visit", "audit", or "incident".
 * @property status Workflow state: "draft", "in_progress", "completed", or "submitted".
 * @property findings Free-text summary of observations and conclusions.
 * @property linkedContextNodeId Optional link to a related context node.
 * @property createdAt Epoch millis when the inspection was created.
 * @property updatedAt Epoch millis of last local mutation.
 * @property syncedAt Epoch millis of last successful sync.
 */
@Serializable
data class FieldInspection(
    val id: String,
    val title: String,
    val description: String? = null,
    val type: String,
    val status: String,
    val location: GeoPoint? = null,
    val address: String? = null,
    val assignedTo: String? = null,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val findings: String? = null,
    val linkedContextNodeId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
)
