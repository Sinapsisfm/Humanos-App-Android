/**
 * data-maps / test / MapsMappersTest.kt — round-trip dominio↔entidad (JVM puro).
 */
package eco.humanos.android.data.maps

import com.google.common.truth.Truth.assertThat
import eco.humanos.android.core.maps.BoundingBox
import eco.humanos.android.core.maps.GeoPoint
import eco.humanos.android.core.maps.MapPackVerifier
import eco.humanos.android.core.maps.MapRegion
import eco.humanos.android.core.maps.RouteGeometry
import eco.humanos.android.core.maps.Waypoint
import org.junit.Test

class MapsMappersTest {

    private val region = MapRegion("r", "Región Sintética", BoundingBox(-41.2, -72.4, -41.0, -72.2))
    private val route = RouteGeometry("route1", listOf(GeoPoint(-41.1, -72.3, 100.0), GeoPoint(-41.12, -72.31, 120.0)))
    private val pack = MapPackVerifier.buildPack(
        "pack1", "v1", region, listOf(route),
        listOf(Waypoint("w1", "Camp", -41.1, -72.3, "campsite")), "2026-01-01T00:00:00Z",
    )

    @Test fun pack_round_trips() {
        assertThat(pack.toEntity().toDomain()).isEqualTo(pack)
    }

    @Test fun route_round_trips() {
        assertThat(route.toEntity().toDomain()).isEqualTo(route)
    }

    @Test fun pack_entity_key_is_packId() {
        assertThat(pack.toEntity().packId).isEqualTo("pack1")
    }
}
