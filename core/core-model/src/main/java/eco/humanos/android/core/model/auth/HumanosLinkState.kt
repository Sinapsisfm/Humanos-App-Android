package eco.humanos.android.core.model.auth

/**
 * Result of the Firebase → HumanOS bridge-token exchange, surfaced to the UI so
 * a silent bridge failure is never invisible again.
 *
 * The Firebase sign-in can succeed while the HumanOS exchange fails (bridge
 * outage, server error, account not provisioned, …). Before, that error was
 * swallowed and the user only saw a downstream "no bridge token" message with
 * no cause. This state carries the actual reason.
 *
 * - [Unknown]  — not attempted yet (signed out, or exchange disabled).
 * - [Linked]   — bridge token obtained and stored; HumanOS data calls will work.
 * - [Failed]   — sign-in worked but the exchange failed; [reason] is the cause
 *                (e.g. "HTTP 401 — Invalid or expired Firebase token").
 */
sealed interface HumanosLinkState {
    data object Unknown : HumanosLinkState
    data object Linked : HumanosLinkState
    data class Failed(val reason: String) : HumanosLinkState
}
