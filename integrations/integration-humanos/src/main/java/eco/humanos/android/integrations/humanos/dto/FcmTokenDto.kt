package eco.humanos.android.integrations.humanos.dto

import kotlinx.serialization.Serializable

/**
 * Request body for `POST /api/mobile/fcm-token` — registers the device's current
 * Firebase Cloud Messaging token so the backend can target this install with
 * push notifications. Authenticated with the **bridge JWT** like every other
 * `mobile/` route. The server responds `{ "ok": true }` ([AckResponse]).
 */
@Serializable
data class FcmTokenRequest(
    val token: String,
)

/**
 * Generic acknowledgement envelope for write endpoints that just confirm
 * success, e.g. `POST /api/mobile/fcm-token` → `{ "ok": true }`. `ok` defaults
 * to `false` so a missing/garbled field is treated as a non-success rather than
 * throwing during deserialization.
 */
@Serializable
data class AckResponse(
    val ok: Boolean = false,
)
