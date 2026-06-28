package eco.humanos.android.core.outdoor

import com.google.common.truth.Truth.assertThat
import eco.humanos.android.core.outdoor.domain.DietaryRestriction
import eco.humanos.android.core.outdoor.domain.OutingEvent
import eco.humanos.android.core.outdoor.domain.OutingEventType
import eco.humanos.android.core.outdoor.domain.SourceType
import eco.humanos.android.core.outdoor.packing.PackingEngine
import eco.humanos.android.core.outdoor.repository.InMemoryOutdoorRepository
import org.junit.Test

class RepositoryRestoreTest {

    private fun evt(eventId: String, at: String = "2026-06-27T10:00:00.000Z", type: OutingEventType = OutingEventType.OUTING_CREATED) =
        OutingEvent(eventId, "outing-1", type, SourceType.USER_DECLARATION, at)

    @Test fun `serialize then restore reconstruye estado exacto`() {
        val repo = InMemoryOutdoorRepository()
        val o = Fixtures.outing()
        repo.putOuting(o)
        repo.putPlan(o.id, PackingEngine.buildCampingPlan(Fixtures.packingInput()))
        repo.appendEvent(evt("e1"))
        repo.appendEvent(evt("e2", "2026-06-27T10:01:00.000Z", OutingEventType.PACKING_GENERATED))

        val blob = repo.serialize()
        val restored = InMemoryOutdoorRepository.restore(blob) // "cierre forzado"

        assertThat(restored.getOuting("outing-1")).isEqualTo(o)
        assertThat(restored.getPlan("outing-1")).isEqualTo(repo.getPlan("outing-1"))
        assertThat(restored.getEvents("outing-1")).isEqualTo(repo.getEvents("outing-1"))
        assertThat(restored.serialize()).isEqualTo(blob)
    }

    @Test fun `idempotencia - reaplicar mismo eventId no duplica`() {
        val repo = InMemoryOutdoorRepository()
        assertThat(repo.appendEvent(evt("x"))).isTrue()
        assertThat(repo.appendEvent(evt("x"))).isFalse()
        assertThat(repo.getEvents()).hasSize(1)
    }

    @Test fun `tras restore reaplicar eventos previos sigue siendo no-op`() {
        val repo = InMemoryOutdoorRepository()
        repo.appendEvent(evt("e1"))
        repo.appendEvent(evt("e2", "2026-06-27T10:02:00.000Z"))
        val restored = InMemoryOutdoorRepository.restore(repo.serialize())
        assertThat(restored.appendEvent(evt("e1"))).isFalse()
        assertThat(restored.appendEvent(evt("e2", "2026-06-27T10:02:00.000Z"))).isFalse()
        assertThat(restored.getEvents()).hasSize(2)
    }

    @Test fun `export por defecto excluye restricciones alimentarias (D-010)`() {
        val repo = InMemoryOutdoorRepository()
        val o = Fixtures.outing(Fixtures.packingInput(dietary = listOf(DietaryRestriction("d1", "alergia maní"))))
        repo.putOuting(o)

        val exported = repo.exportOuting(o.id)
        assertThat(exported!!.includesSensitive).isFalse()
        assertThat(exported.outing.input.dietary).isEmpty()

        val full = repo.exportOuting(o.id, includeSensitive = true)
        assertThat(full!!.includesSensitive).isTrue()
        assertThat(full.outing.input.dietary).hasSize(1)
    }
}
