package eco.humanos.android.feature.settings

import android.content.Context
import com.google.common.truth.Truth.assertThat
import eco.humanos.android.core.model.auth.AuthState
import eco.humanos.android.core.model.auth.HumanOSSession
import eco.humanos.android.core.model.task.TaskItem
import eco.humanos.android.core.model.task.TaskPriority
import eco.humanos.android.data.auth.AuthRepository
import eco.humanos.android.data.auth.GoogleCredentialManager
import eco.humanos.android.integrations.humanos.DailyReviewDto
import eco.humanos.android.integrations.humanos.HumanosGateway
import eco.humanos.android.integrations.quebot.QuebotGateway
import eco.humanos.android.integrations.quebot.ServiceStatus
import eco.humanos.android.integrations.quebot.SseEvent
import eco.humanos.android.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Pure-JVM unit tests for [SettingsViewModel] using minimal hand-written fakes
 * for [HumanosGateway] and [QuebotGateway]. The fakes are controllable so both
 * connected and disconnected paths can be asserted deterministically without
 * simulated network latency. No Robolectric / Android runtime required.
 */
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ── Fakes ────────────────────────────────────────────────────────────────

    /** Minimal [HumanosGateway] whose connectivity result is controllable. */
    private class FakeHumanosGateway(
        private val connected: Boolean,
    ) : HumanosGateway {
        override suspend fun exchangeFirebaseToken(firebaseIdToken: String): Result<HumanOSSession> =
            error("not used in these tests")

        override suspend fun fetchTasks(status: String?): Result<List<TaskItem>> =
            error("not used in these tests")

        override suspend fun createTask(
            title: String,
            description: String?,
            priority: TaskPriority,
        ): Result<TaskItem> = error("not used in these tests")

        override suspend fun fetchDailyReview(): Result<DailyReviewDto> =
            error("not used in these tests")

        override suspend fun checkConnectivity(): Boolean = connected
    }

    /** Minimal [QuebotGateway] whose status result is controllable. */
    private class FakeQuebotGateway(
        private val statusResult: Result<ServiceStatus>,
    ) : QuebotGateway {
        override fun sendMessage(message: String, caseId: String?): Flow<SseEvent> = emptyFlow()

        override suspend fun checkStatus(): Result<ServiceStatus> = statusResult
    }

    /** Minimal [AuthRepository] that emits a fixed auth state and no-ops elsewhere. */
    private class FakeAuthRepository(
        private val state: AuthState = AuthState.Unauthenticated,
    ) : AuthRepository {
        override fun observeAuthState(): Flow<AuthState> = flowOf(state)

        override suspend fun signInWithGoogle(idToken: String): Result<AuthState.Authenticated> =
            error("not used in these tests")

        override suspend fun refreshHumanosToken(): Result<HumanOSSession> =
            error("not used in these tests")

        override suspend fun signOut() = Unit

        override suspend fun getFirebaseToken(): String? = null

        override suspend fun getHumanosToken(): String? = null
    }

    /** Minimal [GoogleCredentialManager]; the sign-in path is not exercised here. */
    private class FakeGoogleCredentialManager : GoogleCredentialManager {
        override suspend fun getGoogleIdToken(activityContext: Context): Result<String> =
            error("not used in these tests")
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    fun `on init checkConnections reflects healthy gateways and ends checking`() = runTest {
        val viewModel = SettingsViewModel(
            humanosGateway = FakeHumanosGateway(connected = true),
            quebotGateway = FakeQuebotGateway(
                Result.success(ServiceStatus(healthy = true, version = "2.1.0")),
            ),
            authRepository = FakeAuthRepository(),
            googleCredentialManager = FakeGoogleCredentialManager(),
        )

        val state = viewModel.uiState.value
        assertThat(state.humanosConnected).isTrue()
        assertThat(state.quebotConnected).isTrue()
        assertThat(state.isCheckingConnections).isFalse()
    }

    @Test
    fun `on init checkConnections reflects disconnected gateways`() = runTest {
        val viewModel = SettingsViewModel(
            humanosGateway = FakeHumanosGateway(connected = false),
            quebotGateway = FakeQuebotGateway(
                Result.success(ServiceStatus(healthy = false, version = null)),
            ),
            authRepository = FakeAuthRepository(),
            googleCredentialManager = FakeGoogleCredentialManager(),
        )

        val state = viewModel.uiState.value
        assertThat(state.humanosConnected).isFalse()
        assertThat(state.quebotConnected).isFalse()
        assertThat(state.isCheckingConnections).isFalse()
    }

    @Test
    fun `quebot status failure is treated as not connected`() = runTest {
        val viewModel = SettingsViewModel(
            humanosGateway = FakeHumanosGateway(connected = true),
            quebotGateway = FakeQuebotGateway(
                Result.failure(IllegalStateException("quebot down")),
            ),
            authRepository = FakeAuthRepository(),
            googleCredentialManager = FakeGoogleCredentialManager(),
        )

        val state = viewModel.uiState.value
        assertThat(state.humanosConnected).isTrue()
        assertThat(state.quebotConnected).isFalse()
        assertThat(state.isCheckingConnections).isFalse()
    }
}
