/**
 * core-maps / MapModels.kt
 *
 * Contratos de mapas (R3 POC). Kotlin/JVM PURO: sin Android, sin MapLibre/SDK, sin red.
 * El dominio NO se acopla a ningún proveedor cartográfico; un adapter (MapLibre u otro)
 * implementaría estas interfaces más adelante. Solo datos sintéticos / fixtures propios.
 *
 * Ubicación canónica: lat/lon WGS84 (+ elevación opcional). Sin tiles públicos ni claves.
 */
package eco.humanos.android.core.maps

import kotlinx.serialization.Serializable
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Serializable
data class GeoPoint(val lat: Double, val lon: Double, val ele: Double? = null)

@Serializable
data class Waypoint(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val category: String? = null,
)

@Serializable
data class TrackPoint(val lat: Double, val lon: Double, val ele: Double? = null, val at: String? = null)

@Serializable
data class BoundingBox(val minLat: Double, val minLon: Double, val maxLat: Double, val maxLon: Double) {
    fun contains(p: GeoPoint): Boolean =
        p.lat in minLat..maxLat && p.lon in minLon..maxLon
}

@Serializable
data class RouteGeometry(val id: String, val points: List<GeoPoint>) {
    /** Caja envolvente (null si no hay puntos). */
    fun bounds(): BoundingBox? {
        if (points.isEmpty()) return null
        return BoundingBox(
            minLat = points.minOf { it.lat }, minLon = points.minOf { it.lon },
            maxLat = points.maxOf { it.lat }, maxLon = points.maxOf { it.lon },
        )
    }

    /** Longitud total en metros (haversine, determinística). */
    fun lengthMeters(): Double {
        var total = 0.0
        for (i in 1 until points.size) total += haversine(points[i - 1], points[i])
        return total
    }
}

@Serializable
data class MapRegion(val id: String, val name: String, val bounds: BoundingBox)

@Serializable
data class MapCoverage(val regions: List<MapRegion>) {
    fun covers(p: GeoPoint): Boolean = regions.any { it.bounds.contains(p) }
    fun coveringRegion(p: GeoPoint): MapRegion? = regions.firstOrNull { it.bounds.contains(p) }
}

/** Manifiesto del pack offline (espeja la filosofía de Knowledge Packs: versión + hash). */
@Serializable
data class MapPackManifest(
    val packId: String,
    val version: String,
    val schemaVersion: Int,
    val createdAt: String,
    val region: MapRegion,
    /** sha256 del contenido serializado (para verificar integridad/anti-corrupción). */
    val contentHash: String,
    val sizeBytes: Long,
)

/** Pack offline: manifiesto + contenido propio (rutas + waypoints). Sin tiles externos. */
@Serializable
data class OfflineMapPack(
    val manifest: MapPackManifest,
    val routes: List<RouteGeometry>,
    val waypoints: List<Waypoint>,
)

const val MAPS_SCHEMA_VERSION = 1

// ── geometría pura ──
private const val EARTH_RADIUS_M = 6_371_000.0

fun haversine(a: GeoPoint, b: GeoPoint): Double {
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLon = Math.toRadians(b.lon - a.lon)
    val la1 = Math.toRadians(a.lat)
    val la2 = Math.toRadians(b.lat)
    val h = sin(dLat / 2) * sin(dLat / 2) + cos(la1) * cos(la2) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * EARTH_RADIUS_M * atan2(sqrt(h), sqrt(1 - h))
}
