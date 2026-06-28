package eco.humanos.android.core.maps

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test

/** Frontera IO asíncrona (Fase D): estados tipados, cancelación, timeout, last-wins, retry. */
class MapLoadUseCaseTest {

    private fun region(id: String = "r") = MapRegion(id, "syn", BoundingBox(-41.2, -72.4, -41.0, -72.2))
    private fun mkPack(id: String, withRoute: Boolean = true) = MapPackVerifier.buildPack(
        id, "v", region(),
        if (withRoute) listOf(RouteGeometry("rt", listOf(GeoPoint(-41.1, -72.3), GeoPoint(-41.12, -72.31)))) else emptyList(),
        emptyList(), "2026-01-01T00:00:00Z",
    )

    private class FakeReader(
        val packs: Map<String, OfflineMapPack?>,
        val delays: Map<String, Long> = emptyMap(),
        val failFirst: Boolean = false,
    ) : SuspendMapReader {
        var calls = 0
        override suspend fun pack(packId: String): OfflineMapPack? {
            calls++
            delays[packId]?.let { if (it > 0) delay(it) }
            if (failFirst && calls == 1) throw IllegalStateException("DB closed")
            return packs[packId]
        }
    }

    @Test fun valid_pack_loads_ready() = runTest {
        val uc = MapLoadUseCase(FakeReader(mapOf("p" to mkPack("p"))))
        assertThat(uc.loadPack("p")).isInstanceOf(MapLoadState.Ready::class.java)
    }

    @Test fun missing_pack() = runTest {
        val uc = MapLoadUseCase(FakeReader(mapOf("p" to null)))
        assertThat(uc.loadPack("p")).isEqualTo(MapLoadState.Missing)
    }

    @Test fun empty_pack() = runTest {
        val uc = MapLoadUseCase(FakeReader(mapOf("p" to mkPack("p", withRoute = false))))
        assertThat(uc.loadPack("p")).isEqualTo(MapLoadState.Empty)
    }

    @Test fun corrupt_pack() = runTest {
        val good = mkPack("p")
        val tampered = good.copy(routes = good.routes.map { r -> r.copy(points = r.points.map { it.copy(lat = it.lat + 0.0001) }) })
        val uc = MapLoadUseCase(FakeReader(mapOf("p" to tampered)))
        assertThat(uc.loadPack("p")).isInstanceOf(MapLoadState.Corrupt::class.java)
    }

    @Test fun reader_failure_maps_to_error_not_crash() = runTest {
        val uc = MapLoadUseCase(FakeReader(mapOf("p" to mkPack("p")), failFirst = true))
        assertThat(uc.loadPack("p")).isInstanceOf(MapLoadState.Error::class.java)
    }

    @Test fun slow_io_awaits_then_ready() = runTest {
        val uc = MapLoadUseCase(FakeReader(mapOf("p" to mkPack("p")), delays = mapOf("p" to 1000)))
        var result: MapLoadState? = null
        val job = launch { result = uc.loadPack("p") }
        assertThat(result).isNull()        // aún no resolvió (no bloqueó el hilo de test)
        advanceUntilIdle()
        job.join()
        assertThat(result).isInstanceOf(MapLoadState.Ready::class.java)
    }

    @Test fun cancellation_is_not_swallowed() = runTest {
        val uc = MapLoadUseCase(FakeReader(mapOf("p" to mkPack("p")), delays = mapOf("p" to 1000)))
        var result: MapLoadState? = null
        val job = backgroundScope.launch { result = uc.loadPack("p") }
        advanceTimeBy(500)
        job.cancelAndJoin()
        assertThat(result).isNull()        // cancelado → no produce estado terminal erróneo
    }

    @Test fun timeout_returns_null() = runTest {
        val uc = MapLoadUseCase(FakeReader(mapOf("p" to mkPack("p")), delays = mapOf("p" to 1000)))
        val out = withTimeoutOrNull(500) { uc.loadPack("p") }
        assertThat(out).isNull()
    }

    @Test fun last_request_wins() = runTest {
        val reader = FakeReader(
            mapOf("slow" to mkPack("slow"), "fast" to mkPack("fast")),
            delays = mapOf("slow" to 1000, "fast" to 10),
        )
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val coord = MapLoadCoordinator(scope, MapLoadUseCase(reader))
        coord.load("slow")
        coord.load("fast")  // cancela la carga lenta (last-request-wins)
        advanceUntilIdle()
        val s = coord.state.value
        assertThat(s).isInstanceOf(MapLoadState.Ready::class.java)
        assertThat((s as MapLoadState.Ready).pack.manifest.packId).isEqualTo("fast")
        scope.cancel()
    }

    @Test fun retry_after_error_succeeds() = runTest {
        val reader = FakeReader(mapOf("p" to mkPack("p")), failFirst = true)
        val uc = MapLoadUseCase(reader)
        assertThat(uc.loadPack("p")).isInstanceOf(MapLoadState.Error::class.java)
        assertThat(uc.loadPack("p")).isInstanceOf(MapLoadState.Ready::class.java) // segundo intento OK
    }

    @Test fun dispatching_reader_over_real_repo() = runTest {
        val repo = InMemoryMapRepository().apply { savePack(mkPack("p")) }
        val reader = DispatchingMapReader(repo, kotlinx.coroutines.Dispatchers.Unconfined)
        val uc = MapLoadUseCase(reader)
        assertThat(uc.loadPack("p")).isInstanceOf(MapLoadState.Ready::class.java)
    }

    @Test fun from_signed_mapper_bridges_states() {
        assertThat(MapLoadState.fromSigned(SignedVerification.Expired)).isEqualTo(MapLoadState.Expired)
        assertThat(MapLoadState.fromSigned(SignedVerification.ContentHashMismatch("manifest")))
            .isInstanceOf(MapLoadState.Corrupt::class.java)
        assertThat(MapLoadState.fromSigned(SignedVerification.InvalidMetadata("x")))
            .isInstanceOf(MapLoadState.Invalid::class.java)
    }
}
