package eco.humanos.android.core.maps

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Cobertura del hardening AC: datum/provenance, validación, límites, fail-closed. */
class HardeningTest {

    // ── datum / provenance ──
    @Test fun route_and_region_default_to_wgs84() {
        assertThat(MapFixtures.ROUTE.datum).isEqualTo(Datum.WGS84)
        assertThat(MapFixtures.SYNTHETIC_REGION.datum).isEqualTo(Datum.WGS84)
    }

    @Test fun track_point_carries_provenance() {
        val tp = MapFixtures.TRACK_WITH_PROVENANCE.first()
        assertThat(tp.datum).isEqualTo(Datum.WGS84)
        assertThat(tp.hAccuracyM).isEqualTo(5.0)
        assertThat(tp.vAccuracyM).isEqualTo(8.0)
        assertThat(tp.source).isEqualTo(PositionSource.SYNTHETIC)
        assertThat(tp.at).isEqualTo("2026-01-01T08:00:00Z")
    }

    // ── validación de coordenadas (fail-closed) ──
    @Test fun geopoint_validity_range_and_finite() {
        assertThat(GeoPoint(-41.0, -72.0).isValid()).isTrue()
        assertThat(GeoPoint(91.0, 0.0).isValid()).isFalse()      // lat fuera de rango
        assertThat(GeoPoint(0.0, 181.0).isValid()).isFalse()     // lon fuera de rango
        assertThat(GeoPoint(Double.NaN, 0.0).isValid()).isFalse()
        assertThat(GeoPoint(0.0, Double.POSITIVE_INFINITY).isValid()).isFalse()
    }

    @Test fun route_validity_respects_point_validity() {
        assertThat(MapFixtures.ROUTE.isValid()).isTrue()
        assertThat(RouteGeometry("bad", listOf(GeoPoint(91.0, 0.0))).isValid()).isFalse()
    }

    // ── pack fail-closed ──
    @Test fun buildPack_rejects_invalid_route() {
        try {
            MapPackVerifier.buildPack(
                "p", "v", MapFixtures.SYNTHETIC_REGION,
                listOf(RouteGeometry("bad", listOf(GeoPoint(0.0, 999.0)))), emptyList(),
                "2026-01-01T00:00:00Z",
            )
            throw AssertionError("debió lanzar MapPackException")
        } catch (e: MapPackException) {
            assertThat(e.message).contains("ruta inválida")
        }
    }

    @Test fun verify_flags_invalid_route_in_tampered_pack() {
        val good = MapPackVerifier.buildPack(
            "p", "v", MapFixtures.SYNTHETIC_REGION, listOf(MapFixtures.ROUTE), MapFixtures.WAYPOINTS,
            "2026-01-01T00:00:00Z",
        )
        // inyectar una ruta inválida sin recalcular manifiesto → Invalid (no Valid)
        val tampered = good.copy(routes = good.routes + RouteGeometry("x", listOf(GeoPoint(95.0, 0.0))))
        assertThat(MapPackVerifier.verify(tampered)).isInstanceOf(PackVerification.Invalid::class.java)
    }

    @Test fun buildPack_rejects_invalid_waypoint() {
        try {
            MapPackVerifier.buildPack(
                "p", "v", MapFixtures.SYNTHETIC_REGION, listOf(MapFixtures.ROUTE),
                listOf(Waypoint("bad", "x", 99.0, 0.0)), "2026-01-01T00:00:00Z",
            )
            throw AssertionError("debió lanzar MapPackException")
        } catch (e: MapPackException) {
            assertThat(e.message).contains("waypoint inválido")
        }
    }

    // ── límites de tamaño (fail-closed) ──
    @Test fun route_exceeding_max_points_is_invalid() {
        val tooMany = RouteGeometry("big", List(MAX_ROUTE_POINTS + 1) { GeoPoint(0.0, 0.0) })
        assertThat(tooMany.isValid()).isFalse()
    }

    @Test fun buildPack_exceeding_max_routes_fails_closed() {
        val many = List(MAX_PACK_ROUTES + 1) { RouteGeometry("r$it", listOf(GeoPoint(0.0, 0.0))) }
        try {
            MapPackVerifier.buildPack("p", "v", MapFixtures.SYNTHETIC_REGION, many, emptyList(), "2026-01-01T00:00:00Z")
            throw AssertionError("debió lanzar MapPackException por MAX_PACK_ROUTES")
        } catch (e: MapPackException) {
            assertThat(e.message).contains("MAX_PACK_ROUTES")
        }
    }

    @Test fun verify_flags_pack_over_max_routes() {
        val good = MapPackVerifier.buildPack(
            "p", "v", MapFixtures.SYNTHETIC_REGION, listOf(MapFixtures.ROUTE), emptyList(), "2026-01-01T00:00:00Z",
        )
        val over = good.copy(routes = List(MAX_PACK_ROUTES + 1) { RouteGeometry("r$it", listOf(GeoPoint(0.0, 0.0))) })
        assertThat(MapPackVerifier.verify(over)).isInstanceOf(PackVerification.Invalid::class.java)
    }

    // ── manifest autenticado por el digest (no solo el contenido) ──
    @Test fun tampering_manifest_metadata_breaks_authenticated_digest() {
        val p = MapPackVerifier.buildPack(
            "p", "v", MapFixtures.SYNTHETIC_REGION, listOf(MapFixtures.ROUTE), MapFixtures.WAYPOINTS,
            "2026-01-01T00:00:00Z",
        )
        // cambiar metadata del manifiesto (region.name) SIN recalcular el hash → Corrupt
        val tampered = p.copy(manifest = p.manifest.copy(region = p.manifest.region.copy(name = "OtraRegion")))
        assertThat(MapPackVerifier.verify(tampered)).isInstanceOf(PackVerification.Corrupt::class.java)
    }

    @Test fun pack_carries_datum_in_manifest() {
        val p = MapPackVerifier.buildPack(
            "p", "v", MapFixtures.SYNTHETIC_REGION, listOf(MapFixtures.ROUTE), MapFixtures.WAYPOINTS,
            "2026-01-01T00:00:00Z",
        )
        assertThat(p.manifest.datum).isEqualTo(Datum.WGS84)
        assertThat(MapPackVerifier.verify(p)).isEqualTo(PackVerification.Valid)
    }

    // ── schema v2 ──
    @Test fun schema_version_is_2() {
        assertThat(MAPS_SCHEMA_VERSION).isEqualTo(2)
    }

    @Test fun v1_pack_is_rejected_as_schema_mismatch() {
        val p = MapPackVerifier.buildPack(
            "p", "v", MapFixtures.SYNTHETIC_REGION, listOf(MapFixtures.ROUTE), MapFixtures.WAYPOINTS,
            "2026-01-01T00:00:00Z",
        )
        val v1 = p.copy(manifest = p.manifest.copy(schemaVersion = 1))
        assertThat(MapPackVerifier.verify(v1)).isEqualTo(PackVerification.SchemaMismatch(2, 1))
    }

    // ── GPX adversarial / límites ──
    @Test fun gpx_over_byte_limit_fails_closed() {
        val huge = "x".repeat(MAX_GPX_BYTES + 1)
        try {
            GpxParser.parse(huge)
            throw AssertionError("debió lanzar por límite de tamaño")
        } catch (e: GpxParseException) {
            assertThat(e.message).contains("límite de tamaño")
        }
    }

    @Test fun gpx_out_of_range_coordinate_fails_closed() {
        try {
            GpxParser.parse(MapFixtures.GPX_OUT_OF_RANGE)
            throw AssertionError("debió lanzar por coordenada inválida")
        } catch (e: GpxParseException) {
            assertThat(e.message).contains("coordenada inválida")
        }
    }

    @Test fun gpx_import_marks_provenance_source() {
        val doc = GpxParser.parse(MapFixtures.GPX_FICTIONAL)
        assertThat(doc.tracks.first().source).isEqualTo(PositionSource.IMPORT_GPX)
    }
}
