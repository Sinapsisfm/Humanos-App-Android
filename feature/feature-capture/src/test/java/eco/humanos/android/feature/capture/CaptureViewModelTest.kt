package eco.humanos.android.feature.capture

import com.google.common.truth.Truth.assertThat
import eco.humanos.android.core.model.capture.CaptureItem
import eco.humanos.android.core.model.capture.CaptureType
import eco.humanos.android.core.model.capture.ProcessingStatus
import eco.humanos.android.core.model.common.IntegrationSource
import eco.humanos.android.core.model.common.PrivacyLevel
import eco.humanos.android.data.capture.repository.CaptureRepository
import eco.humanos.android.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Pure-JVM unit tests for [CaptureViewModel] using a hand-written fake
 * [CaptureRepository] backed by a [MutableStateFlow]. No Robolectric / Android
 * runtime required.
 */
class CaptureViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ── Fake ─────────────────────────────────────────────────────────────────

    /**
     * In-memory [CaptureRepository]. [observeCaptures] returns the backing flow;
     * [saveTextCapture] appends a TEXT [CaptureItem] and returns its generated id.
     */
    private class FakeCaptureRepository : CaptureRepository {

        val state = MutableStateFlow<List<CaptureItem>>(emptyList())
        val savedTexts = mutableListOf<String>()

        override fun observeCaptures(): Flow<List<CaptureItem>> = state

        override suspend fun saveTextCapture(
            text: String,
            privacyLevel: PrivacyLevel,
        ): String {
            savedTexts += text
            val id = "capture-${state.value.size + 1}"
            state.update {
                it + CaptureItem(
                    id = id,
                    type = CaptureType.TEXT,
                    textContent = text,
                    source = IntegrationSource.LOCAL,
                    privacyLevel = privacyLevel,
                    processingStatus = ProcessingStatus.DONE,
                    createdAt = 1L,
                )
            }
            return id
        }

        override suspend fun deleteCapture(id: String) {
            state.update { current -> current.filterNot { it.id == id } }
        }
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    fun `updateText changes the text input`() = runTest {
        val viewModel = CaptureViewModel(FakeCaptureRepository())

        viewModel.updateText("hola mundo")

        assertThat(viewModel.uiState.value.textInput).isEqualTo("hola mundo")
    }

    @Test
    fun `saveCapture with non-blank text saves and clears input and sets message`() = runTest {
        val repo = FakeCaptureRepository()
        val viewModel = CaptureViewModel(repo)

        viewModel.updateText("comprar pan")
        viewModel.saveCapture()

        // Repository received the capture.
        assertThat(repo.savedTexts).containsExactly("comprar pan")

        val state = viewModel.uiState.value
        assertThat(state.textInput).isEmpty()
        assertThat(state.isSaving).isFalse()
        assertThat(state.savedMessage).isEqualTo("Captura guardada")
        // The observed captures list reflects the new item.
        assertThat(state.captures.map { it.textContent }).containsExactly("comprar pan")
    }

    @Test
    fun `saveCapture with blank text is a no-op and does not touch the repository`() = runTest {
        val repo = FakeCaptureRepository()
        val viewModel = CaptureViewModel(repo)

        viewModel.updateText("   ")
        viewModel.saveCapture()

        assertThat(repo.savedTexts).isEmpty()
        assertThat(viewModel.uiState.value.savedMessage).isNull()
        assertThat(viewModel.uiState.value.captures).isEmpty()
    }
}
