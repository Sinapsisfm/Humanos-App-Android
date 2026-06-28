/**
 * feature-outdoor / OutdoorMapsPocScreen.kt
 *
 * POC R3 (mapas offline) — pantalla Compose STATELESS, detrás del flag OUTDOOR_R3_MAPS_ENABLED
 * (OFF por defecto). Dibuja en un Canvas propio una región de cobertura, una ruta planificada,
 * waypoints y un track grabado, proyectando lat/lon → píxeles. NO usa MapLibre ni ningún SDK
 * cartográfico, NO descarga tiles, NO toca la red: solo geometrías SINTÉTICAS de core-maps.
 *
 * Demuestra la separación dominio↔UI: la pantalla solo conoce los contratos de core-maps
 * (RouteGeometry/Waypoint/MapRegion/TrackPoint); un adapter cartográfico real reemplazaría
 * este Canvas más adelante sin cambiar el dominio.
 */
package eco.humanos.android.feature.outdoor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import eco.humanos.android.core.maps.GeoPoint
import eco.humanos.android.core.maps.MapRegion
import eco.humanos.android.core.maps.RouteGeometry
import eco.humanos.android.core.maps.TrackPoint
import eco.humanos.android.core.maps.Waypoint

const val OUTDOOR_MAPS_POC_TAG = "outdoor-maps-poc"

/** Estado de la POC: todo opcional salvo la región (define el encuadre). */
data class OutdoorMapsPocUiState(
    val region: MapRegion,
    val route: RouteGeometry? = null,
    val waypoints: List<Waypoint> = emptyList(),
    val track: List<TrackPoint> = emptyList(),
)

@Composable
fun OutdoorMapsPocScreen(
    state: OutdoorMapsPocUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Mapa offline · POC", style = MaterialTheme.typography.titleLarge)
        Text(
            "DATOS SINTÉTICOS — no es cobertura cartográfica real. Sin tiles, sin red.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        Text(state.region.name, style = MaterialTheme.typography.bodyMedium)

        val routeColor = MaterialTheme.colorScheme.primary
        val trackColor = MaterialTheme.colorScheme.tertiary
        val regionColor = MaterialTheme.colorScheme.outline
        val wpColor = MaterialTheme.colorScheme.secondary

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .testTag(OUTDOOR_MAPS_POC_TAG)
                .semantics { contentDescription = "Mapa POC de la salida (datos sintéticos)" },
        ) {
            val proj = MapProjection(state, size.width, size.height, padding = 24f)

            // marco de la región (cobertura)
            drawRect(
                color = regionColor,
                topLeft = Offset(proj.padding, proj.padding),
                size = androidx.compose.ui.geometry.Size(
                    size.width - 2 * proj.padding,
                    size.height - 2 * proj.padding,
                ),
                style = Stroke(width = 2f),
            )

            // ruta planificada
            state.route?.let { r -> drawPolyline(r.points.map { proj.toOffset(it.lat, it.lon) }, routeColor, 5f) }

            // track grabado (punteado)
            if (state.track.isNotEmpty()) {
                drawPolyline(
                    state.track.map { proj.toOffset(it.lat, it.lon) },
                    trackColor, 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
                )
            }

            // waypoints
            state.waypoints.forEach { w ->
                drawCircle(color = wpColor, radius = 7f, center = proj.toOffset(w.lat, w.lon))
            }
        }
    }
}

private fun DrawScope.drawPolyline(
    pts: List<Offset>,
    color: Color,
    width: Float,
    pathEffect: PathEffect? = null,
) {
    for (i in 1 until pts.size) {
        drawLine(color = color, start = pts[i - 1], end = pts[i], strokeWidth = width, pathEffect = pathEffect)
    }
}

/**
 * Proyección equirectangular simple lat/lon → píxeles, encuadrada en la región (más cualquier
 * punto fuera de ella) con padding. Norte arriba. Maneja spans degenerados (línea/punto).
 */
private class MapProjection(
    state: OutdoorMapsPocUiState,
    private val w: Float,
    private val h: Float,
    val padding: Float,
) {
    private val minLat: Double
    private val maxLat: Double
    private val minLon: Double
    private val maxLon: Double

    init {
        val lats = ArrayList<Double>()
        val lons = ArrayList<Double>()
        with(state.region.bounds) {
            lats += minLat; lats += maxLat; lons += minLon; lons += maxLon
        }
        state.route?.points?.forEach { lats += it.lat; lons += it.lon }
        state.waypoints.forEach { lats += it.lat; lons += it.lon }
        state.track.forEach { lats += it.lat; lons += it.lon }
        minLat = lats.min(); maxLat = lats.max(); minLon = lons.min(); maxLon = lons.max()
    }

    private fun span(min: Double, max: Double) = (max - min).let { if (it == 0.0) 1.0 else it }

    fun toOffset(lat: Double, lon: Double): Offset {
        val innerW = w - 2 * padding
        val innerH = h - 2 * padding
        val x = padding + ((lon - minLon) / span(minLon, maxLon)) * innerW
        val y = padding + ((maxLat - lat) / span(minLat, maxLat)) * innerH // invertir: norte arriba
        return Offset(x.toFloat(), y.toFloat())
    }
}
