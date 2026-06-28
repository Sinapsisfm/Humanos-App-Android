package eco.humanos.android.core.maps

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TrackRecorderTest {

    @Test fun empty_recorder_has_zero_distance() {
        val rec = TrackRecorder("t1")
        assertThat(rec.size()).isEqualTo(0)
        assertThat(rec.distanceMeters()).isEqualTo(0.0)
    }

    @Test fun records_points_in_order() {
        val rec = TrackRecorder("t1")
        rec.record(TrackPoint(-41.10, -72.30, at = "2026-01-01T08:00:00Z"))
        rec.record(TrackPoint(-41.11, -72.31, at = "2026-01-01T08:30:00Z"))
        assertThat(rec.size()).isEqualTo(2)
        assertThat(rec.recorded().first().at).isEqualTo("2026-01-01T08:00:00Z")
    }

    @Test fun to_route_matches_recorded_points() {
        val rec = TrackRecorder("t1")
        MapFixtures.ROUTE.points.forEach { rec.record(TrackPoint(it.lat, it.lon, it.ele)) }
        val route = rec.toRoute()
        assertThat(route.id).isEqualTo("t1")
        assertThat(route.points).hasSize(3)
        assertThat(rec.distanceMeters()).isWithin(1e-6).of(MapFixtures.ROUTE.lengthMeters())
    }

    @Test fun clear_resets() {
        val rec = TrackRecorder("t1")
        rec.record(TrackPoint(-41.10, -72.30))
        rec.clear()
        assertThat(rec.size()).isEqualTo(0)
    }
}
