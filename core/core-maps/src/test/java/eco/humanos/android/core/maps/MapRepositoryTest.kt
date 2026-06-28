package eco.humanos.android.core.maps

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MapRepositoryTest {

    private fun pack(id: String = "syn-pack") = MapPackVerifier.buildPack(
        packId = id, version = "v0.1.0", region = MapFixtures.SYNTHETIC_REGION,
        routes = listOf(MapFixtures.ROUTE), waypoints = MapFixtures.WAYPOINTS,
        createdAt = "2026-01-01T00:00:00Z",
    )

    @Test fun absent_pack_returns_null() {
        assertThat(InMemoryMapRepository().pack("nope")).isNull()
    }

    @Test fun save_and_get_pack_idempotent() {
        val repo = InMemoryMapRepository()
        repo.savePack(pack())
        repo.savePack(pack()) // mismo id → no duplica
        assertThat(repo.packs()).hasSize(1)
        assertThat(repo.pack("syn-pack")).isNotNull()
    }

    @Test fun coverage_is_union_of_pack_regions() {
        val repo = InMemoryMapRepository()
        repo.savePack(pack())
        assertThat(repo.coverage().covers(MapFixtures.INSIDE)).isTrue()
        assertThat(repo.coverage().covers(MapFixtures.OUTSIDE)).isFalse()
    }

    @Test fun save_route_and_read() {
        val repo = InMemoryMapRepository()
        repo.saveRoute(MapFixtures.ROUTE)
        assertThat(repo.route("syn-route")!!.points).hasSize(3)
    }

    @Test fun serialize_then_restore_roundtrip_process_death() {
        val repo = InMemoryMapRepository()
        repo.savePack(pack())
        repo.saveRoute(MapFixtures.ROUTE)
        val snapshot = repo.serialize()

        val restored = InMemoryMapRepository.restore(snapshot)
        assertThat(restored.packs()).hasSize(1)
        assertThat(restored.routes()).hasSize(1)
        // el pack restaurado sigue verificando íntegro
        assertThat(MapPackVerifier.verify(restored.pack("syn-pack")!!))
            .isEqualTo(PackVerification.Valid)
    }

    @Test fun pack_imported_from_gpx_verifies() {
        val gpx = GpxParser.parse(MapFixtures.GPX_FICTIONAL)
        val p = MapPackVerifier.buildPack(
            packId = "from-gpx", version = "v0.1.0", region = MapFixtures.SYNTHETIC_REGION,
            routes = gpx.tracks, waypoints = gpx.waypoints, createdAt = "2026-01-01T00:00:00Z",
        )
        val repo = InMemoryMapRepository()
        repo.savePack(p)
        assertThat(MapPackVerifier.verify(repo.pack("from-gpx")!!)).isEqualTo(PackVerification.Valid)
    }

    @Test fun delete_pack_removes_coverage() {
        val repo = InMemoryMapRepository()
        repo.savePack(pack())
        repo.deletePack("syn-pack")
        assertThat(repo.packs()).isEmpty()
        assertThat(repo.coverage().covers(MapFixtures.INSIDE)).isFalse()
    }
}
