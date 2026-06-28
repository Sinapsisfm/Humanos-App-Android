/**
 * data-outdoor / OutdoorMappers.kt
 *
 * Mapeo entidad Room ↔ dominio vía kotlinx.serialization (JSON). PUROS y JVM-testeables
 * (no requieren runtime de Room).
 */
package eco.humanos.android.data.outdoor

import eco.humanos.android.core.outdoor.domain.CampingPlan
import eco.humanos.android.core.outdoor.domain.Intent
import eco.humanos.android.core.outdoor.domain.OutdoorOuting
import eco.humanos.android.core.outdoor.domain.OutingEvent
import eco.humanos.android.core.outdoor.domain.OutingEventType
import eco.humanos.android.core.outdoor.domain.PackingInput
import eco.humanos.android.core.outdoor.domain.ScenarioKind
import eco.humanos.android.core.outdoor.domain.OutingStatus
import eco.humanos.android.core.outdoor.domain.SourceType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal val OUTDOOR_JSON = Json { encodeDefaults = true; ignoreUnknownKeys = true }

fun OutdoorOuting.toEntity(): OutdoorOutingEntity = OutdoorOutingEntity(
    id = id,
    title = title,
    intent = intent.name,
    scenario = scenario.name,
    status = status.name,
    rulesetVersion = rulesetVersion,
    inputJson = OUTDOOR_JSON.encodeToString(input),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun OutdoorOutingEntity.toDomain(): OutdoorOuting = OutdoorOuting(
    id = id,
    title = title,
    intent = Intent.valueOf(intent),
    scenario = ScenarioKind.valueOf(scenario),
    status = OutingStatus.valueOf(status),
    rulesetVersion = rulesetVersion,
    input = OUTDOOR_JSON.decodeFromString<PackingInput>(inputJson),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun CampingPlan.toEntity(outingId: String): OutdoorPlanEntity =
    OutdoorPlanEntity(outingId = outingId, planJson = OUTDOOR_JSON.encodeToString(this))

fun OutdoorPlanEntity.toDomain(): CampingPlan =
    OUTDOOR_JSON.decodeFromString<CampingPlan>(planJson)

fun OutingEvent.toEntity(): OutdoorEventEntity = OutdoorEventEntity(
    eventId = eventId,
    outingId = outingId,
    type = type.name,
    sourceType = sourceType.name,
    at = at,
    payloadJson = OUTDOOR_JSON.encodeToString(payload),
)

fun OutdoorEventEntity.toDomain(): OutingEvent = OutingEvent(
    eventId = eventId,
    outingId = outingId,
    type = OutingEventType.valueOf(type),
    sourceType = SourceType.valueOf(sourceType),
    at = at,
    payload = OUTDOOR_JSON.decodeFromString<Map<String, String>>(payloadJson),
)
