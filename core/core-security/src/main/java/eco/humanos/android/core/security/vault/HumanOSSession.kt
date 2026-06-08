package eco.humanos.android.core.security.vault

import kotlinx.serialization.Serializable

/**
 * The decrypted, in-memory shape of the active HumanOS backend session as held
 * by the [HumanOSSessionStore] (the "Secure Vault").
 *
 * This is a **security boundary** type, distinct from
 * `eco.humanos.android.core.model.auth.HumanOSSession` (the API/domain DTO).
 * It is the only representation of the bridge token that the vault serializes,
 * encrypts, and persists. It is **never** written to Room and its
 * [bridgeToken] value is **never** logged.
 *
 * Instances are short-lived: read on demand from [HumanOSSessionStore.get],
 * used to authorize a request, and dropped. The persisted form is the encrypted
 * JSON of this class inside Keystore-backed `EncryptedSharedPreferences`.
 *
 * @property bridgeToken HumanOS bridge JWT used as the API bearer token. Sensitive.
 * @property userId Firebase UID of the authenticated user.
 * @property personId HumanOS Person ID, or null until the user is linked.
 * @property email Account email, if known. Used only to rehydrate UI state.
 * @property displayName Account display name, if known. Used only for UI state.
 * @property expiresAtEpochMs Epoch millis at which [bridgeToken] expires. A
 *   session whose [expiresAtEpochMs] is `< now` is treated as absent.
 */
@Serializable
data class HumanOSSession(
    val bridgeToken: String,
    val userId: String,
    val personId: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val expiresAtEpochMs: Long,
) {
    /** Whether this session is still valid at [nowEpochMs]. */
    fun isValidAt(nowEpochMs: Long): Boolean = expiresAtEpochMs > nowEpochMs
}
