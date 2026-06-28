package eco.humanos.android.core.maps

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GeometryTest {

    @Test fun haversine_zero_for_same_point() {
        assertThat(haversine(MapFixtures.INSIDE, MapFixtures.INSIDE)).isEqualTo(0.0)
    }

    @Test fun haversine_is_symmetric() {
        val a = GeoPoint(-41.10, -72.30)
        val b = GeoPoint(-41.12, -72.30)
        assertThat(haversine(a, b)).isWithin(1e-6).of(haversine(b, a))
    }

    @Test fun haversine_one_degree_lat_is_about_111km() {
        val d = haversine(GeoPoint(0.0, 0.0), GeoPoint(1.0, 0.0))
        assertThat(d).isWithin(500.0).of(111_195.0) // ~111.2 km por grado de latitud
    }

    @Test fun route_length_sums_segments() {
        val r = MapFixtures.ROUTE
        val manual = haversine(r.points[0], r.points[1]) + haversine(r.points[1], r.points[2])
        assertThat(r.lengthMeters()).isWithin(1e-6).of(manual)
    }

    @Test fun empty_route_has_zero_length_and_null_bounds() {
        val r = RouteGeometry("empty", emptyList())
        assertThat(r.lengthMeters()).isEqualTo(0.0)
        assertThat(r.bounds()).isNull()
    }

    @Test fun bounds_envelopes_all_points() {
        val b = MapFixtures.ROUTE.bounds()!!
        assertThat(b.minLat).isEqualTo(-41.12)
        assertThat(b.maxLat).isEqualTo(-41.10)
        assertThat(b.minLon).isEqualTo(-72.31)
        assertThat(b.maxLon).isEqualTo(-72.30)
    }

    @Test fun bounding_box_contains() {
        assertThat(MapFixtures.SYNTHETIC_REGION.bounds.contains(MapFixtures.INSIDE)).isTrue()
        assertThat(MapFixtures.SYNTHETIC_REGION.bounds.contains(MapFixtures.OUTSIDE)).isFalse()
    }
}
