package eco.humanos.android.core.model.common

import kotlinx.serialization.Serializable

/**
 * Controls where AI inference runs for a given operation.
 *
 * - [LOCAL_ONLY] — on-device models only (offline capable).
 * - [REMOTE_ONLY] — always delegates to the HumanOS backend / Anthropic API.
 * - [HYBRID] — tries local first, falls back to remote.
 * - [OFF] — no AI processing.
 */
@Serializable
enum class AiExecutionMode {
    LOCAL_ONLY,
    REMOTE_ONLY,
    HYBRID,
    OFF,
}
