package eco.humanos.android.integrations.humanos.dto

import kotlinx.serialization.Serializable

/**
 * Request body for `POST /api/mobile/check-ins` — a wellbeing check-in.
 *
 * Matches the HumanOS `CheckIn` contract: energy / mood / stress are integer
 * scores **1..5** (not free text), upserted per day. `source` marks origin
 * ("api" for the mobile app).
 */
@Serializable
data class CreateCheckInDto(
    val energy: Int,
    val mood: Int,
    val stress: Int,
    val perceivedLoad: Int? = null,
    val note: String? = null,
    val source: String = "api",
)

/**
 * A recorded check-in as returned by HumanOS. `date` / `createdAt` are ISO-8601
 * strings (Prisma DateTime). All scores are 1..5.
 */
@Serializable
data class CheckInDto(
    val id: String,
    val userId: String? = null,
    val date: String? = null,
    val energy: Int = 0,
    val mood: Int = 0,
    val stress: Int = 0,
    val perceivedLoad: Int = 3,
    val note: String? = null,
    val source: String? = null,
    val createdAt: String? = null,
)

/** Envelope for `POST /api/mobile/check-ins` → `{ "checkIn": {...} }`. */
@Serializable
data class CheckInEnvelope(val checkIn: CheckInDto)

/**
 * Envelope for `GET /api/mobile/check-ins` → `{ "checkIns": [...], "today": {...}|null }`.
 */
@Serializable
data class CheckInsEnvelope(
    val checkIns: List<CheckInDto> = emptyList(),
    val today: CheckInDto? = null,
)
