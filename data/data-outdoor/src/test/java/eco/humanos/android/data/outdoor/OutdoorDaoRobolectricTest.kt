/**
 * data-outdoor / test / OutdoorDaoRobolectricTest.kt
 *
 * Evidencia Android REAL ejecutada vía Robolectric (sin emulador; los únicos devices
 * conectados eran instancias BlueStacks de otro proyecto y NO se usaron). Valida el DAO
 * Room, idempotencia de eventos, persistencia/lectura, y "restore tras cierre" (cerrar y
 * reabrir la base con el mismo nombre de archivo).
 */
package eco.humanos.android.data.outdoor

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import eco.humanos.android.core.outdoor.repository.InMemoryOutdoorRepository
import eco.humanos.android.core.outdoor.domain.AccessMode
import eco.humanos.android.core.outdoor.domain.Accommodation
import eco.humanos.android.core.outdoor.domain.Availability
import eco.humanos.android.core.outdoor.domain.FacilityProfile
import eco.humanos.android.core.outdoor.domain.Intent
import eco.humanos.android.core.outdoor.domain.OutdoorOuting
import eco.humanos.android.core.outdoor.domain.OutingEvent
import eco.humanos.android.core.outdoor.domain.OutingEventType
import eco.humanos.android.core.outdoor.domain.OutingStatus
import eco.humanos.android.core.outdoor.domain.PackingInput
import eco.humanos.android.core.outdoor.domain.ScenarioKind
import eco.humanos.android.core.outdoor.domain.Season
import eco.humanos.android.core.outdoor.domain.SourceType
import eco.humanos.android.core.outdoor.packing.PackingEngine
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OutdoorDaoRobolectricTest {

    private val ctx get() = ApplicationProvider.getApplicationContext<Context>()

    private fun input() = PackingInput(
        scenario = ScenarioKind.CAMPING, season = Season.VERANO, territory = "CL-AR", nights = 2,
        participants = emptyList(),
        facility = FacilityProfile(
            Accommodation.TENT, AccessMode.VEHICLE, Availability.AVAILABLE, true,
            Availability.NONE, true, true, false, Availability.LIMITED,
        ),
    )

    private fun outing() = OutdoorOuting(
        "o1", "Camping", Intent.CAMPING_VACACIONES, ScenarioKind.CAMPING, OutingStatus.DRAFT,
        "camping-cl.v0.2.0", input(), "2026-06-27T10:00:00.000Z", "2026-06-27T10:00:00.000Z",
    )

    private fun event(id: String) =
        OutingEvent(id, "o1", OutingEventType.OUTING_CREATED, SourceType.USER_DECLARATION, "2026-06-27T10:00:00.000Z")

    private lateinit var db: OutdoorDatabase

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ctx, OutdoorDatabase::class.java).allowMainThreadQueries().build()
    }

    @After fun teardown() = db.close()

    @Test fun `dao appendEvent es idempotente`() {
        val repo = RoomOutdoorRepository(db.outdoorDao())
        assertThat(repo.appendEvent(event("x"))).isTrue()
        assertThat(repo.appendEvent(event("x"))).isFalse()
        assertThat(repo.getEvents("o1")).hasSize(1)
    }

    @Test fun `dao persiste y lee outing + plan`() {
        val repo = RoomOutdoorRepository(db.outdoorDao())
        val o = outing()
        repo.putOuting(o)
        repo.putPlan(o.id, PackingEngine.buildCampingPlan(input()))
        assertThat(repo.getOuting("o1")).isEqualTo(o)
        assertThat(repo.getPlan("o1")).isEqualTo(PackingEngine.buildCampingPlan(input()))
    }

    @Test fun `serialize de Room es restaurable en in-memory (paridad de snapshot)`() {
        val repo = RoomOutdoorRepository(db.outdoorDao())
        repo.putOuting(outing())
        repo.putPlan("o1", PackingEngine.buildCampingPlan(input()))
        repo.appendEvent(event("e1"))

        val blob = repo.serialize()
        val mem = InMemoryOutdoorRepository.restore(blob) // mismo códec → restaurable

        assertThat(mem.getOuting("o1")).isEqualTo(outing())
        assertThat(mem.getPlan("o1")).isEqualTo(repo.getPlan("o1"))
        assertThat(mem.getEvents("o1")).isEqualTo(repo.getEvents("o1"))
    }

    @Test fun `restore tras cierre - reabrir base de archivo conserva los datos`() {
        val name = "outdoor-restore-test.db"
        val db1 = Room.databaseBuilder(ctx, OutdoorDatabase::class.java, name).allowMainThreadQueries().build()
        val repo1 = RoomOutdoorRepository(db1.outdoorDao())
        repo1.putOuting(outing())
        repo1.appendEvent(event("e1"))
        db1.close() // "cierre forzado"

        val db2 = Room.databaseBuilder(ctx, OutdoorDatabase::class.java, name).allowMainThreadQueries().build()
        val repo2 = RoomOutdoorRepository(db2.outdoorDao())
        assertThat(repo2.getOuting("o1")).isEqualTo(outing())
        assertThat(repo2.getEvents("o1")).hasSize(1)
        // reaplicar el evento tras restore sigue siendo no-op (idempotencia preservada)
        assertThat(repo2.appendEvent(event("e1"))).isFalse()
        db2.close()
        ctx.deleteDatabase(name)
    }
}
