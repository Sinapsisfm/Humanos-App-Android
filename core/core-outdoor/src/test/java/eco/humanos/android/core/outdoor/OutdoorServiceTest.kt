package eco.humanos.android.core.outdoor

import com.google.common.truth.Truth.assertThat
import eco.humanos.android.core.outdoor.packing.PackingEngine
import eco.humanos.android.core.outdoor.repository.InMemoryOutdoorRepository
import eco.humanos.android.core.outdoor.service.Clock
import eco.humanos.android.core.outdoor.service.OutdoorService
import org.junit.Test

class OutdoorServiceTest {

    private fun service(): Pair<OutdoorService, InMemoryOutdoorRepository> {
        val repo = InMemoryOutdoorRepository()
        val clock = Clock { "2026-06-27T12:00:00.000Z" }
        return OutdoorService(repo, clock) to repo
    }

    @Test fun `crear salida persiste outing, plan y eventos`() {
        val (svc, repo) = service()
        val view = svc.createCampingOuting("o1", "Camping Pucón", Fixtures.packingInput())
        assertThat(view.outing.id).isEqualTo("o1")
        assertThat(view.plan.items).isNotEmpty()
        assertThat(repo.getOuting("o1")).isNotNull()
        assertThat(repo.getPlan("o1")).isNotNull()
        // dos eventos: creación + plan generado
        assertThat(repo.getEvents("o1").map { it.type.name })
            .containsExactly("OUTING_CREATED", "PACKING_GENERATED")
    }

    @Test fun `applyEdits actualiza el plan y registra evento`() {
        val (svc, repo) = service()
        svc.createCampingOuting("o1", "C", Fixtures.packingInput())
        val before = repo.getPlan("o1")!!.items.first { it.key == "food.rations" }.bought
        val view = svc.applyEdits("o1", listOf(PackingEngine.UserEdit.SetBought("food.rations", true)))
        assertThat(before).isFalse()
        assertThat(view!!.plan.items.first { it.key == "food.rations" }.bought).isTrue()
        assertThat(repo.getEvents("o1").any { it.type.name == "PACKING_ITEM_EDITED" }).isTrue()
    }

    @Test fun `signals refleja contradicciones del plan editado`() {
        val (svc, _) = service()
        svc.createCampingOuting("o1", "C", Fixtures.packingInput())
        // plan completo: sin faltante de iluminación
        assertThat(svc.signals("o1").map { it.code }).doesNotContain("camping.no_lighting")
        // quitamos iluminación → debe aparecer la señal
        svc.applyEdits("o1", listOf(
            PackingEngine.UserEdit.Remove("lighting.lantern"),
            PackingEngine.UserEdit.Remove("lighting.headlamp"),
        ))
        assertThat(svc.signals("o1").map { it.code }).contains("camping.no_lighting")
    }

    @Test fun `restore via repo serialize conserva la salida creada por el servicio`() {
        val (svc, repo) = service()
        svc.createCampingOuting("o1", "C", Fixtures.packingInput())
        val restored = InMemoryOutdoorRepository.restore(repo.serialize())
        val svc2 = OutdoorService(restored, Clock { "2026-06-27T13:00:00.000Z" })
        val view = svc2.outingView("o1")
        assertThat(view).isNotNull()
        assertThat(view!!.outing.title).isEqualTo("C")
        // reaplicar el evento de creación sería no-op (idempotencia preservada)
        assertThat(restored.getEvents("o1")).hasSize(2)
    }

    @Test fun `dos ediciones distintas en el mismo instante no pierden eventos (MED-1)`() {
        val (svc, repo) = service()
        svc.createCampingOuting("o1", "C", Fixtures.packingInput()) // reloj fijo
        svc.applyEdits("o1", listOf(PackingEngine.UserEdit.SetPacked("safety.first_aid", true)))
        svc.applyEdits("o1", listOf(PackingEngine.UserEdit.SetBought("food.rations", true)))
        // ambas ediciones (mismo `now`, mismo tipo de edit distinto contenido) quedan registradas
        val edits = repo.getEvents("o1").filter { it.type.name == "PACKING_ITEM_EDITED" }
        assertThat(edits).hasSize(2)
        assertThat(edits.map { it.eventId }.toSet()).hasSize(2) // eventIds únicos
    }

    @Test fun `operaciones sobre salida inexistente devuelven null`() {
        val (svc, _) = service()
        assertThat(svc.applyEdits("nope", emptyList())).isNull()
        assertThat(svc.outingView("nope")).isNull()
        assertThat(svc.signals("nope")).isEmpty()
    }
}
