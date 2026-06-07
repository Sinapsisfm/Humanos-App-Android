package eco.humanos.android.core.model.common

import kotlinx.serialization.Serializable

/**
 * Describes a runtime permission the app may request, along with its
 * current grant status and the feature it unlocks.
 *
 * Used by the permission manager to display rationale UI and gate
 * feature access without scattering permission logic across modules.
 */
@Serializable
data class PermissionCapability(
    val permission: String,
    val isRequired: Boolean,
    val rationale: String,
    val featureGate: String,
    val isGranted: Boolean,
)
