package eco.humanos.android.integrations.humanos.dto

import eco.humanos.android.core.model.auth.HumanOSSession
import kotlinx.serialization.Serializable

/**
 * Response body of `POST /api/auth/mobile/exchange`.
 *
 * The Firebase ID token is sent in the `Authorization: Bearer <token>` header
 * (never the body, to avoid accidental logging) and HumanOS returns the
 * "bridge" JWT used as the Bearer for every subsequent API call.
 *
 * Contract source: `docs/03_INTEGRATIONS/MOBILE_AUTH_ENDPOINT_SPEC.md`.
 *
 * @property token Bridge JWT (HS256) to use as `Authorization: Bearer` on API calls.
 * @property expiresIn Token lifetime in seconds (spec: 900 = 15 min).
 * @property userId Canonical HumanOS user id.
 * @property personId HumanOS Person id, null if the user has no linked Person.
 * @property email Verified email from the Firebase token.
 * @property displayName Display name from the Firebase token, may be null.
 * @property orgIds Organisations the user belongs to.
 */
@Serializable
data class MobileExchangeResponse(
    val token: String,
    val expiresIn: Long,
    val userId: String,
    val personId: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val orgIds: List<String> = emptyList(),
)

/**
 * Optional request body for the exchange call. The Firebase token itself
 * travels in the header, so this carries only device/telemetry hints.
 */
@Serializable
data class MobileExchangeRequest(
    val deviceId: String? = null,
    val platform: String = "android",
)

/**
 * Map the exchange response to the persisted [HumanOSSession].
 *
 * @param nowMillis current epoch millis, used to compute the absolute
 *   [HumanOSSession.expiresAt] from the relative [MobileExchangeResponse.expiresIn].
 */
fun MobileExchangeResponse.toHumanOSSession(nowMillis: Long): HumanOSSession =
    HumanOSSession(
        userId = userId,
        personId = personId,
        bearerToken = token,
        expiresAt = nowMillis + expiresIn * 1_000L,
    )
