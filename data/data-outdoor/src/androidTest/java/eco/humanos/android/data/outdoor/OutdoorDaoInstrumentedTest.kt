/**
 * data-outdoor / androidTest / OutdoorDaoInstrumentedTest.kt
 *
 * PREPARADO, NO EJECUTADO en este entorno (requiere emulador/dispositivo o Robolectric).
 * Valida DAO + idempotencia + persistencia/lectura sobre la base Room real.
 *
 * Ejecutar con (con emulador/dispositivo conectado):
 *   ./gradlew :data:data-outdoor:connectedDebugAndroidTest
 *
 * GATE: hasta ejecutarlo y conservar evidencia, RoomOutdoorRepository NO se conecta como
 * repositorio por defecto y la tarea de Room NO se marca validada.
 */
package eco.humanos.android.data.outdoor

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
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

@RunWith(AndroidJUnit4::class)
class OutdoorDaoInstrumentedTest {

    private lateinit var db: OutdoorDatabase
    private lateinit var repo: RoomOutdoorRepository

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

    @Before fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, OutdoorDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = RoomOutdoorRepository(db.outdoorDao())
    }

    @After fun teardown() = db.close()

    @Test fun appendEvent_isIdempotent() {
        assertThat(repo.appendEvent(event("x"))).isTrue()
        assertThat(repo.appendEvent(event("x"))).isFalse() // mismo eventId → ignorado
        assertThat(repo.getEvents("o1")).hasSize(1)
    }

    @Test fun outing_and_plan_persist_and_read() {
        val o = outing()
        repo.putOuting(o)
        repo.putPlan(o.id, PackingEngine.buildCampingPlan(input()))
        assertThat(repo.getOuting("o1")).isEqualTo(o)
        assertThat(repo.getPlan("o1")).isEqualTo(PackingEngine.buildCampingPlan(input()))
    }
}
