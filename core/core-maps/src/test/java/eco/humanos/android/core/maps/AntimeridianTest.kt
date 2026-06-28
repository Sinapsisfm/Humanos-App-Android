package eco.humanos.android.core.maps

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Cajas envolventes robustas: span más corto + cruce del antimeridiano. */
class AntimeridianTest {

    @Test fun normal_cluster_yields_normal_box() {
        val box = BoundingBox.of(listOf(GeoPoint(-41.0, -72.0), GeoPoint(-41.0, -70.0)))!!
        assertThat(box.crossesAntimeridian).isFalse()
        assertThat(box.minLon).isEqualTo(-72.0)
        assertThat(box.maxLon).isEqualTo(-70.0)
    }

    @Test fun points_straddling_180_choose_short_crossing_span() {
        // 170E y -170E (=190E): el span corto (20°) cruza el antimeridiano, no envuelve 340°
        val box = BoundingBox.of(listOf(GeoPoint(0.0, 170.0), GeoPoint(0.0, -170.0)))!!
        assertThat(box.crossesAntimeridian).isTrue()
        assertThat(box.minLon).isEqualTo(170.0)
        assertThat(box.maxLon).isEqualTo(-170.0)
    }

    @Test fun crossing_box_contains_uses_wraparound() {
        val box = BoundingBox.of(listOf(GeoPoint(0.0, 170.0), GeoPoint(0.0, -170.0)))!!
        assertThat(box.contains(GeoPoint(0.0, 175.0))).isTrue()   // dentro del lado este
        assertThat(box.contains(GeoPoint(0.0, -175.0))).isTrue()  // dentro del lado oeste
        assertThat(box.contains(GeoPoint(0.0, 0.0))).isFalse()    // antimeridiano opuesto, fuera
    }

    @Test fun bounds_of_invalid_points_is_null_fail_closed() {
        assertThat(BoundingBox.of(listOf(GeoPoint(95.0, 0.0)))).isNull()
        assertThat(BoundingBox.of(emptyList())).isNull()
    }

    @Test fun single_point_is_degenerate_normal_box() {
        val box = BoundingBox.of(listOf(GeoPoint(10.0, 20.0)))!!
        assertThat(box.crossesAntimeridian).isFalse()
        assertThat(box.minLon).isEqualTo(20.0)
        assertThat(box.maxLon).isEqualTo(20.0)
    }
}
