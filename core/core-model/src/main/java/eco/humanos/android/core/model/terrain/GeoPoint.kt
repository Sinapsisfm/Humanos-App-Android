package eco.humanos.android.core.model.terrain

import kotlinx.serialization.Serializable

/**
 * A geographic coordinate with optional altitude and accuracy metadata.
 *
 * Used by field inspections and evidence items to record where
 * observations were made. Kept as a simple value object to avoid
 * Android Location framework dependencies in the model layer.
 *
 * @property latitude Decimal degrees, WGS84.
 * @property longitude Decimal degrees, WGS84.
 * @property altitude Meters above WGS84 ellipsoid, if available.
 * @property accuracy Estimated horizontal accuracy in meters.
 * @property timestamp Epoch millis when the fix was obtained.
 */
@Serializable
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Float? = null,
    val timestamp: Long,
)
