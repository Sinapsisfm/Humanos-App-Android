/**
 * core-maps / TrackRecorder.kt
 *
 * Grabador de track determinístico (sin GPS real, sin reloj). Recibe TrackPoint ya
 * resueltos (lat/lon/ele/at inyectados) y acumula. Convierte a RouteGeometry y mide
 * distancia con la misma geometría haversine del dominio. El adapter de ubicación
 * (FusedLocationProvider u otro) alimentaría esto sin que el dominio dependa de Android.
 */
package eco.humanos.android.core.maps

class TrackRecorder(val id: String) {
    private val points = ArrayList<TrackPoint>()

    fun record(point: TrackPoint) { points.add(point) }
    fun recorded(): List<TrackPoint> = points.toList()
    fun size(): Int = points.size
    fun clear() { points.clear() }

    /** Track grabado como geometría de ruta (para dibujar / comparar con la planificada). */
    fun toRoute(): RouteGeometry =
        RouteGeometry(id = id, points = points.map { GeoPoint(it.lat, it.lon, it.ele) })

    /** Distancia acumulada del track en metros. */
    fun distanceMeters(): Double = toRoute().lengthMeters()
}
