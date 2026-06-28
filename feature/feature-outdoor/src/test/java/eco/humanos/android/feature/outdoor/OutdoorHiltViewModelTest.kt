/**
 * feature-outdoor / test / OutdoorHiltViewModelTest.kt
 *
 * Verifica los fixes del review independiente:
 *  - BLOCKER-1: el VM CONSUME el `OutdoorRepository` inyectado (no construye uno propio).
 *  - HIGH-1: el reconocimiento de señales se persiste en `SavedStateHandle` y se restaura
 *    en una nueva instancia (recreación de Activity / proceso muerto).
 */
package eco.humanos.android.feature.outdoor

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import eco.humanos.android.core.outdoor.repository.InMemoryOutdoorRepository
import org.junit.Test

class OutdoorHiltViewModelTest {

    @Test fun `consume el repositorio inyectado (lo puebla con la salida demo)`() {
        val repo = InMemoryOutdoorRepository()
        val vm = OutdoorHiltViewModel(repo, SavedStateHandle())
        assertThat(vm.uiState.value.title).isNotEmpty()
        // Si el VM construyera su propio repo, ESTE quedaría vacío. Prueba el wiring Hilt.
        assertThat(repo.listOutings()).isNotEmpty()
    }

    @Test fun `dismiss persiste en SavedStateHandle y se restaura en nueva instancia`() {
        val repo = InMemoryOutdoorRepository()
        val saved = SavedStateHandle()
        val vm1 = OutdoorHiltViewModel(repo, saved)
        assertThat(vm1.uiState.value.signals.map { it.code }).contains("aware.prep_incomplete")

        vm1.dismissSignal("aware.prep_incomplete")
        assertThat(saved.get<ArrayList<String>>("outdoor_dismissed")).contains("aware.prep_incomplete")

        // Recreación / proceso muerto: nueva instancia con el MISMO savedState + repo.
        val vm2 = OutdoorHiltViewModel(repo, saved)
        assertThat(vm2.uiState.value.signals.map { it.code }).doesNotContain("aware.prep_incomplete")
    }
}
