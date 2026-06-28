/**
 * feature-outdoor / OutdoorMapsPocRoute.kt
 *
 * Wrapper de la POC de mapas. Provee datos SINTÉTICOS (definidos en repo) al Canvas. No hay
 * proveedor, red ni almacenamiento real: es una demostración de arquitectura (dominio↔UI).
 * Solo es alcanzable cuando OUTDOOR_R3_MAPS_ENABLED está encendido (OFF por defecto).
 */
package eco.humanos.android.feature.outdoor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eco.humanos.android.core.maps.BoundingBox
import eco.humanos.android.core.maps.GeoPoint
import eco.humanos.android.core.maps.MapRegion
import eco.humanos.android.core.maps.RouteGeometry
import eco.humanos.android.core.maps.TrackPoint
import eco.humanos.android.core.maps.Waypoint

/** Datos sintéticos para la POC (no representan cobertura real). */
object OutdoorMapsDemo {
    val state: OutdoorMapsPocUiState = OutdoorMapsPocUiState(
        region = MapRegion(
            id = "syn-valle",
            name = "Valle Sintético (demo, no es cobertura real)",
            bounds = BoundingBox(minLat = -41.20, minLon = -72.40, maxLat = -41.00, maxLon = -72.20),
        ),
        route = RouteGeometry(
            id = "syn-route",
            points = listOf(
                GeoPoint(-41.10, -72.30, 120.0),
                GeoPoint(-41.12, -72.31, 140.0),
                GeoPoint(-41.14, -72.29, 160.0),
                GeoPoint(-41.16, -72.30, 180.0),
            ),
        ),
        waypoints = listOf(
            Waypoint("wp-camp", "Camp Sintético", -41.10, -72.30, "campsite"),
            Waypoint("wp-water", "Agua Sintética", -41.14, -72.29, "water"),
        ),
        track = listOf(
            TrackPoint(-41.10, -72.30, 120.0, "2026-01-01T08:00:00Z"),
            TrackPoint(-41.11, -72.305, 130.0, "2026-01-01T08:30:00Z"),
            TrackPoint(-41.13, -72.308, 150.0, "2026-01-01T09:00:00Z"),
        ),
    )
}

@Composable
fun OutdoorMapsPocRoute(modifier: Modifier = Modifier) {
    OutdoorMapsPocScreen(state = OutdoorMapsDemo.state, modifier = modifier)
}
