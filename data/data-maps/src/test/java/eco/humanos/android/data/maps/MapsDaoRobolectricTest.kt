/**
 * data-maps / test / MapsDaoRobolectricTest.kt
 *
 * Evidencia Room REAL vía Robolectric (sin emulador). Valida DAO, idempotencia, cobertura,
 * paridad cross-adapter (Room → in-memory) y "restore tras cierre" (cerrar/reabrir la DB).
 */
package eco.humanos.android.data.maps

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import eco.humanos.android.core.maps.BoundingBox
import eco.humanos.android.core.maps.GeoPoint
import eco.humanos.android.core.maps.InMemoryMapRepository
import eco.humanos.android.core.maps.MapPackVerifier
import eco.humanos.android.core.maps.MapRegion
import eco.humanos.android.core.maps.PackVerification
import eco.humanos.android.core.maps.RouteGeometry
import eco.humanos.android.core.maps.Waypoint
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MapsDaoRobolectricTest {

    private lateinit var db: MapsDatabase
    private lateinit var repo: RoomMapRepository

    private val region = MapRegion("r", "Región Sintética", BoundingBox(-41.2, -72.4, -41.0, -72.2))
    private val inside = GeoPoint(-41.1, -72.3)
    private val outside = GeoPoint(-33.45, -70.66)
    private val route = RouteGeometry("route1", listOf(GeoPoint(-41.1, -72.3, 100.0), GeoPoint(-41.12, -72.31, 120.0)))
    private fun pack() = MapPackVerifier.buildPack(
        "pack1", "v1", region, listOf(route),
        listOf(Waypoint("w1", "Camp", -41.1, -72.3, "campsite")), "2026-01-01T00:00:00Z",
    )

    @Before fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, MapsDatabase::class.java).allowMainThreadQueries().build()
        repo = RoomMapRepository(db.mapsDao())
    }

    @After fun tearDown() { db.close() }

    @Test fun save_and_get_pack_idempotent() {
        repo.savePack(pack())
        repo.savePack(pack()) // mismo packId → REPLACE, no duplica
        assertThat(repo.packs()).hasSize(1)
        assertThat(MapPackVerifier.verify(repo.pack("pack1")!!)).isEqualTo(PackVerification.Valid)
    }

    @Test fun coverage_from_stored_packs() {
        repo.savePack(pack())
        assertThat(repo.coverage().covers(inside)).isTrue()
        assertThat(repo.coverage().covers(outside)).isFalse()
    }

    @Test fun save_route_and_read() {
        repo.saveRoute(route)
        assertThat(repo.route("route1")!!.points).hasSize(2)
    }

    @Test fun delete_pack_removes_coverage() {
        repo.savePack(pack())
        repo.deletePack("pack1")
        assertThat(repo.packs()).isEmpty()
        assertThat(repo.coverage().covers(inside)).isFalse()
    }

    @Test fun serialize_parity_room_to_inmemory() {
        repo.savePack(pack())
        repo.saveRoute(route)
        val blob = repo.serialize()
        // el blob de Room se restaura idéntico en el adaptador in-memory (paridad)
        val restored = InMemoryMapRepository.restore(blob)
        assertThat(restored.packs()).hasSize(1)
        assertThat(restored.routes()).hasSize(1)
        assertThat(MapPackVerifier.verify(restored.pack("pack1")!!)).isEqualTo(PackVerification.Valid)
    }

    @Test fun restore_after_reopen_file_db() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val name = "maps-reopen-test.db"
        ctx.getDatabasePath(name).delete()
        var fdb = Room.databaseBuilder(ctx, MapsDatabase::class.java, name).allowMainThreadQueries().build()
        RoomMapRepository(fdb.mapsDao()).savePack(pack())
        fdb.close()
        // reabrir con el mismo archivo → el pack persiste (restore tras cierre)
        fdb = Room.databaseBuilder(ctx, MapsDatabase::class.java, name).allowMainThreadQueries().build()
        val reopened = RoomMapRepository(fdb.mapsDao())
        assertThat(reopened.packs()).hasSize(1)
        assertThat(MapPackVerifier.verify(reopened.pack("pack1")!!)).isEqualTo(PackVerification.Valid)
        fdb.close()
        ctx.getDatabasePath(name).delete()
    }
}
