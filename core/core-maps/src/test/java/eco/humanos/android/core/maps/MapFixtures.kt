/**
 * core-maps / test / MapFixtures.kt
 *
 * Datos SINTÉTICOS creados para tests. NO representan cobertura cartográfica real.
 * Coordenadas ficticias dentro de una caja inventada ("Valle Sintético"). No provienen
 * de OSM ni de ningún proveedor; no hay tiles, claves ni red.
 */
package eco.humanos.android.core.maps

object MapFixtures {

    /** Región sintética de prueba (caja chica, coordenadas ficticias). */
    val SYNTHETIC_REGION = MapRegion(
        id = "syn-valle",
        name = "Valle Sintético (test, no es cobertura real)",
        bounds = BoundingBox(minLat = -41.20, minLon = -72.40, maxLat = -41.00, maxLon = -72.20),
    )

    /** Punto dentro de la región. */
    val INSIDE = GeoPoint(lat = -41.10, lon = -72.30)

    /** Punto fuera de la región. */
    val OUTSIDE = GeoPoint(lat = -33.45, lon = -70.66)

    val ROUTE = RouteGeometry(
        id = "syn-route",
        points = listOf(
            GeoPoint(-41.10, -72.30, 120.0),
            GeoPoint(-41.11, -72.31, 140.0),
            GeoPoint(-41.12, -72.30, 160.0),
        ),
    )

    val WAYPOINTS = listOf(
        Waypoint(id = "wp-camp", name = "Camp Sintético", lat = -41.10, lon = -72.30, category = "campsite"),
        Waypoint(id = "wp-water", name = "Agua Sintética", lat = -41.115, lon = -72.305, category = "water"),
    )

    /**
     * GPX 1.1 ficticio: 1 track de 3 puntos + 1 waypoint. Tiempos ISO fijos (determinista).
     */
    val GPX_FICTIONAL = """
        <?xml version="1.0" encoding="UTF-8"?>
        <gpx version="1.1" creator="humanos-outdoor-test">
          <wpt lat="-41.10" lon="-72.30">
            <ele>120.0</ele>
            <name>Camp Sintético</name>
            <type>campsite</type>
          </wpt>
          <trk>
            <name>Track Sintético</name>
            <trkseg>
              <trkpt lat="-41.10" lon="-72.30"><ele>120.0</ele><time>2026-01-01T08:00:00Z</time></trkpt>
              <trkpt lat="-41.11" lon="-72.31"><ele>140.0</ele><time>2026-01-01T08:30:00Z</time></trkpt>
              <trkpt lat="-41.12" lon="-72.30"><ele>160.0</ele><time>2026-01-01T09:00:00Z</time></trkpt>
            </trkseg>
          </trk>
        </gpx>
    """.trimIndent()

    /** GeoJSON mínimo generado en repo (LineString sintético) — para futuros adapters. */
    val GEOJSON_MINIMAL = """
        {"type":"Feature","properties":{"id":"syn-route","synthetic":true},
        "geometry":{"type":"LineString","coordinates":[[-72.30,-41.10],[-72.31,-41.11],[-72.30,-41.12]]}}
    """.trimIndent()

    /** GPX malformado (XML roto) para probar fail-closed del parser. */
    val GPX_CORRUPT = "<gpx><trk><trkseg><trkpt lat=\"oops\""

    /** GPX BIEN FORMADO pero con lat no numérica → fail-closed a nivel de atributo. */
    val GPX_BAD_ATTR = """
        <?xml version="1.0" encoding="UTF-8"?>
        <gpx version="1.1"><trk><trkseg>
          <trkpt lat="oops" lon="-72.30"><ele>1.0</ele></trkpt>
        </trkseg></trk></gpx>
    """.trimIndent()
}
