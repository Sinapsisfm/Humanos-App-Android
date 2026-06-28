/**
 * core-maps / MapModels.kt
 *
 * Contratos de mapas (R3 POC, endurecido en Fase AC). Kotlin/JVM PURO: sin Android, sin
 * MapLibre/SDK, sin red. El dominio NO se acopla a ningún proveedor; un adapter lo
 * implementaría después. Solo datos sintéticos / fixtures propios.
 *
 * Hardening AC: datum explícito (WGS84), provenance + precisión en puntos grabados, cajas
 * envolventes robustas con manejo de antimeridiano, validación fail-closed de coordenadas,
 * límites de tamaño. Ubicación canónica: lat/lon WGS84 (+ elevación opcional).
 */
package eco.humanos.android.core.maps

import kotlinx.serialization.Serializable
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Datum geodésico. El POC solo admite WGS84 (canónico). Enum para forzar explicitud. */
@Serializable
enum class Datum { WGS84 }

/** Origen/proveniencia de un punto grabado. Sin GPS real en el POC (SYNTHETIC/IMPORT_GPX). */
@Serializable
enum class PositionSource { GPS, MANUAL, IMPORT_GPX, SYNTHETIC, UNKNOWN }

// ── límites de tamaño (fail-closed) ──
const val MAX_GPX_BYTES = 5_000_000          // 5 MB de GPX como máximo
const val MAX_GPX_ELEMENTS = 100_000         // nº máx de trkpt/rtept/wpt combinados
const val MAX_ROUTE_POINTS = 200_000         // nº máx de puntos por ruta
const val MAX_PACK_ROUTES = 10_000           // nº máx de rutas por pack

/** Coordenada geométrica pura (WGS84). Validación fail-closed con [isValid]. */
@Serializable
data class GeoPoint(val lat: Double, val lon: Double, val ele: Double? = null) {
    /** true si lat∈[-90,90], lon∈[-180,180], todos finitos (sin NaN/Inf). */
    fun isValid(): Boolean =
        lat.isFinite() && lon.isFinite() && (ele == null || ele.isFinite()) &&
            lat in -90.0..90.0 && lon in -180.0..180.0
}

@Serializable
data class Waypoint(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val category: String? = null,
)

/**
 * Punto grabado con provenance: datum, precisión horizontal/vertical (m), origen y timestamp.
 * Todo inyectado (sin reloj/GPS real en el POC) → determinístico.
 */
@Serializable
data class TrackPoint(
    val lat: Double,
    val lon: Double,
    val ele: Double? = null,
    val at: String? = null,
    val datum: Datum = Datum.WGS84,
    val hAccuracyM: Double? = null,
    val vAccuracyM: Double? = null,
    val source: PositionSource = PositionSource.UNKNOWN,
)

/**
 * Caja envolvente. Soporta cruce del **antimeridiano**: si [minLon] > [maxLon] la caja cruza
 * ±180° (convención GeoJSON bbox). Construir SIEMPRE con [BoundingBox.of] para robustez.
 */
@Serializable
data class BoundingBox(val minLat: Double, val minLon: Double, val maxLat: Double, val maxLon: Double) {
    val crossesAntimeridian: Boolean get() = minLon > maxLon

    fun contains(p: GeoPoint): Boolean {
        if (!p.isValid()) return false
        val latOk = p.lat in minLat..maxLat
        val lonOk = if (crossesAntimeridian) (p.lon >= minLon || p.lon <= maxLon)
        else (p.lon in minLon..maxLon)
        return latOk && lonOk
    }

    companion object {
        /**
         * Caja envolvente robusta de un conjunto de puntos. Maneja antimeridiano eligiendo el
         * span longitudinal MÁS CORTO (excluye el mayor hueco angular). Devuelve null si vacío
         * o si algún punto es inválido (fail-closed).
         */
        fun of(points: List<GeoPoint>): BoundingBox? {
            if (points.isEmpty()) return null
            if (points.any { !it.isValid() }) return null
            val minLat = points.minOf { it.lat }
            val maxLat = points.maxOf { it.lat }
            val lons = points.map { it.lon }.sorted()
            // mayor hueco interior entre longitudes consecutivas
            var maxGap = -1.0; var gapStart = lons.first(); var gapEnd = lons.first()
            for (i in 1 until lons.size) {
                val gap = lons[i] - lons[i - 1]
                if (gap > maxGap) { maxGap = gap; gapStart = lons[i - 1]; gapEnd = lons[i] }
            }
            val wrapGap = (lons.first() + 360.0) - lons.last() // hueco a través del antimeridiano
            return if (wrapGap >= maxGap) {
                BoundingBox(minLat, lons.first(), maxLat, lons.last())          // caja normal
            } else {
                BoundingBox(minLat, gapEnd, maxLat, gapStart)                   // caja que cruza ±180
            }
        }
    }
}

@Serializable
data class RouteGeometry(
    val id: String,
    val points: List<GeoPoint>,
    val datum: Datum = Datum.WGS84,
    val source: PositionSource = PositionSource.UNKNOWN,
) {
    /** Caja envolvente robusta (antimeridiano-aware). Null si vacía o con punto inválido. */
    fun bounds(): BoundingBox? = BoundingBox.of(points)

    /** Longitud total en metros (haversine, determinística). */
    fun lengthMeters(): Double {
        var total = 0.0
        for (i in 1 until points.size) total += haversine(points[i - 1], points[i])
        return total
    }

    /** true si todos los puntos son válidos y no se exceden los límites. */
    fun isValid(): Boolean = points.size <= MAX_ROUTE_POINTS && points.all { it.isValid() }
}

@Serializable
data class MapRegion(
    val id: String,
    val name: String,
    val bounds: BoundingBox,
    val datum: Datum = Datum.WGS84,
)

@Serializable
data class MapCoverage(val regions: List<MapRegion>) {
    fun covers(p: GeoPoint): Boolean = regions.any { it.bounds.contains(p) }
    fun coveringRegion(p: GeoPoint): MapRegion? = regions.firstOrNull { it.bounds.contains(p) }
}

/** Manifiesto del pack offline (espeja Knowledge Packs: versión + datum + hash + tamaño). */
@Serializable
data class MapPackManifest(
    val packId: String,
    val version: String,
    val schemaVersion: Int,
    val createdAt: String,
    val region: MapRegion,
    val datum: Datum = Datum.WGS84,
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

/** Schema v2: agrega datum/provenance/antimeridiano respecto del POC v1. */
const val MAPS_SCHEMA_VERSION = 2

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
