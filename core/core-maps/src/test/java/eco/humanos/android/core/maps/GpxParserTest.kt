package eco.humanos.android.core.maps

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GpxParserTest {

    @Test fun parses_track_points() {
        val doc = GpxParser.parse(MapFixtures.GPX_FICTIONAL)
        assertThat(doc.tracks).hasSize(1)
        val t = doc.tracks.first()
        assertThat(t.points).hasSize(3)
        assertThat(t.points[0].lat).isEqualTo(-41.10)
        assertThat(t.points[0].lon).isEqualTo(-72.30)
        assertThat(t.points[0].ele).isEqualTo(120.0)
    }

    @Test fun parses_waypoint_with_name_and_type() {
        val doc = GpxParser.parse(MapFixtures.GPX_FICTIONAL)
        assertThat(doc.waypoints).hasSize(1)
        val w = doc.waypoints.first()
        assertThat(w.name).isEqualTo("Camp Sintético")
        assertThat(w.category).isEqualTo("campsite")
        assertThat(w.lat).isEqualTo(-41.10)
    }

    @Test fun parsed_track_geometry_has_positive_length() {
        val t = GpxParser.parse(MapFixtures.GPX_FICTIONAL).tracks.first()
        assertThat(t.lengthMeters()).isGreaterThan(0.0)
    }

    @Test fun malformed_gpx_fails_closed() {
        try {
            GpxParser.parse(MapFixtures.GPX_CORRUPT)
            throw AssertionError("debió lanzar GpxParseException")
        } catch (e: GpxParseException) {
            assertThat(e.message).contains("GPX inválido")
        }
    }

    @Test fun non_numeric_lat_fails_closed_at_attribute_level() {
        // XML bien formado, pero lat no numérica → debe fallar en toGeoPoint, no parsear basura.
        try {
            GpxParser.parse(MapFixtures.GPX_BAD_ATTR)
            throw AssertionError("debió lanzar GpxParseException por lat inválida")
        } catch (e: GpxParseException) {
            assertThat(e.message).contains("punto sin lat")
        }
    }

    @Test fun doctype_is_rejected_anti_xxe() {
        val withDoctype = """
            <?xml version="1.0"?>
            <!DOCTYPE gpx [ <!ENTITY x "y"> ]>
            <gpx></gpx>
        """.trimIndent()
        try {
            GpxParser.parse(withDoctype)
            throw AssertionError("DTD debió ser rechazado")
        } catch (e: GpxParseException) {
            assertThat(e.message).contains("GPX inválido")
        }
    }
}
