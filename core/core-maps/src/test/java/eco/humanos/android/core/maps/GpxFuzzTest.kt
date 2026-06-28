package eco.humanos.android.core.maps

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

/** GPX adversarial + geometría extrema + fuzzing determinístico (seed fijo). */
class GpxFuzzTest {

    // ── versiones / namespaces ──
    @Test fun gpx_1_0_default_namespace_parses() {
        val gpx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.0" xmlns="http://www.topografix.com/GPX/1/0">
              <trk><trkseg>
                <trkpt lat="-41.10" lon="-72.30"><ele>100</ele></trkpt>
                <trkpt lat="-41.11" lon="-72.31"><ele>110</ele></trkpt>
              </trkseg></trk>
            </gpx>
        """.trimIndent()
        val doc = GpxParser.parse(gpx)
        assertThat(doc.tracks).hasSize(1)
        assertThat(doc.tracks.first().points).hasSize(2)
    }

    @Test fun gpx_prefixed_namespace_yields_no_tracks_but_does_not_crash() {
        // limitación conocida (parser namespace-unaware): prefijo → 0 tracks, sin datos basura
        val gpx = """
            <?xml version="1.0"?>
            <g:gpx xmlns:g="http://www.topografix.com/GPX/1/1">
              <g:trk><g:trkseg><g:trkpt lat="-41.1" lon="-72.3"/></g:trkseg></g:trk>
            </g:gpx>
        """.trimIndent()
        assertThat(GpxParser.parse(gpx).tracks).isEmpty()
    }

    // ── ataques XML ──
    @Test fun billion_laughs_entity_expansion_fails_closed() {
        val gpx = """
            <?xml version="1.0"?>
            <!DOCTYPE gpx [ <!ENTITY a "aaaaaaaaaa"> <!ENTITY b "&a;&a;&a;&a;&a;"> ]>
            <gpx><trk><trkseg><trkpt lat="0" lon="0"><name>&b;</name></trkpt></trkseg></trk></gpx>
        """.trimIndent()
        try { GpxParser.parse(gpx); throw AssertionError("DOCTYPE/entidad debió ser rechazado") }
        catch (e: GpxParseException) { assertThat(e.message).contains("GPX inválido") }
    }

    // ── numéricos adversariales ──
    @Test fun nan_latitude_fails_closed() {
        val gpx = """<?xml version="1.0"?><gpx><trk><trkseg><trkpt lat="NaN" lon="0"/></trkseg></trk></gpx>"""
        try { GpxParser.parse(gpx); throw AssertionError("NaN debió fallar") }
        catch (e: GpxParseException) { assertThat(e.message).contains("coordenada inválida") }
    }

    @Test fun infinity_longitude_fails_closed() {
        val gpx = """<?xml version="1.0"?><gpx><trk><trkseg><trkpt lat="0" lon="Infinity"/></trkseg></trk></gpx>"""
        try { GpxParser.parse(gpx); throw AssertionError("Infinity debió fallar") }
        catch (e: GpxParseException) { assertThat(e.message).contains("coordenada inválida") }
    }

    @Test fun out_of_range_lat_91_fails_closed() {
        val gpx = """<?xml version="1.0"?><gpx><wpt lat="91" lon="0"/></gpx>"""
        try { GpxParser.parse(gpx); throw AssertionError("lat 91 debió fallar") }
        catch (e: GpxParseException) { assertThat(e.message).contains("coordenada inválida") }
    }

    // ── geometría extrema ──
    @Test fun antimeridian_route_length_is_short_not_global() {
        // 179.9E → -179.9E: ~22 km cerca del ecuador, NO ~40000 km
        val r = RouteGeometry("am", listOf(GeoPoint(0.0, 179.9), GeoPoint(0.0, -179.9)))
        assertThat(r.lengthMeters()).isLessThan(50_000.0)
        val b = r.bounds()!!
        assertThat(b.crossesAntimeridian).isTrue()
    }

    @Test fun near_pole_bounds_valid() {
        val r = RouteGeometry("pole", listOf(GeoPoint(89.9, 0.0), GeoPoint(89.95, 120.0), GeoPoint(89.99, -120.0)))
        val b = r.bounds()!!
        assertThat(b.maxLat).isAtMost(90.0)
        assertThat(r.points.all { b.contains(it) }).isTrue()
    }

    @Test fun global_bounding_box_contains_extremes() {
        val r = RouteGeometry("g", listOf(GeoPoint(-89.0, -179.0), GeoPoint(89.0, 179.0)))
        val b = r.bounds()!!
        assertThat(b.contains(GeoPoint(-89.0, -179.0))).isTrue()
        assertThat(b.contains(GeoPoint(89.0, 179.0))).isTrue()
    }

    // ── fuzzing determinístico (seed fijo, sin rutas reales) ──
    @Test fun fuzz_bounds_always_contains_all_points() {
        val rnd = Random(42)
        repeat(300) {
            val n = 1 + rnd.nextInt(20)
            val pts = List(n) { GeoPoint(rnd.nextDouble(-89.0, 89.0), rnd.nextDouble(-179.0, 179.0)) }
            val b = BoundingBox.of(pts)!!
            // invariante: la caja (normal o cruzando antimeridiano) contiene todos los puntos
            assertThat(pts.all { b.contains(it) }).isTrue()
        }
    }

    @Test fun fuzz_gpx_roundtrip_parses_valid_points() {
        val rnd = Random(7)
        repeat(50) {
            val n = 1 + rnd.nextInt(10)
            val pts = List(n) { GeoPoint(rnd.nextDouble(-89.0, 89.0), rnd.nextDouble(-179.0, 179.0)) }
            val trkpts = pts.joinToString("") { "<trkpt lat=\"${it.lat}\" lon=\"${it.lon}\"/>" }
            val gpx = """<?xml version="1.0"?><gpx><trk><trkseg>$trkpts</trkseg></trk></gpx>"""
            val parsed = GpxParser.parse(gpx)
            assertThat(parsed.tracks.first().points).hasSize(n)
            assertThat(parsed.tracks.first().points.all { it.isValid() }).isTrue()
        }
    }

    @Test fun fuzz_pack_integrity_roundtrip_deterministic() {
        val rnd = Random(99)
        repeat(50) {
            val pts = List(1 + rnd.nextInt(8)) { GeoPoint(rnd.nextDouble(-89.0, 89.0), rnd.nextDouble(-179.0, 179.0)) }
            val route = RouteGeometry("r", pts)
            val region = MapRegion("reg", "syn", BoundingBox.of(pts)!!)
            val pack = MapPackVerifier.buildPack("p", "v", region, listOf(route), emptyList(), "2026-01-01T00:00:00Z")
            assertThat(MapPackVerifier.verify(pack)).isEqualTo(PackVerification.Valid)
            // re-serialización idéntica (determinismo)
            val repo = InMemoryMapRepository().apply { savePack(pack) }
            val restored = InMemoryMapRepository.restore(repo.serialize())
            assertThat(MapPackVerifier.verify(restored.pack("p")!!)).isEqualTo(PackVerification.Valid)
        }
    }
}
