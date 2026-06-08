package eco.humanos.android.feature.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eco.humanos.android.core.model.auth.AuthState
import eco.humanos.android.core.model.auth.HumanosLinkState
import eco.humanos.android.core.model.task.TaskItem
import eco.humanos.android.core.model.task.TaskStatus
import eco.humanos.android.data.auth.AuthRepository
import eco.humanos.android.data.auth.GoogleCredentialManager
import eco.humanos.android.data.tasks.repository.TaskRepository
import eco.humanos.android.integrations.humanos.HumanosGateway
import eco.humanos.android.integrations.humanos.dto.CheckInDto
import eco.humanos.android.integrations.humanos.toHumanosErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

/**
 * Drives the dashboard. Tasks are offline-first (observed from Room, refreshed
 * from HumanOS); the daily snapshot (greeting, counts, today's check-in) comes
 * straight from `GET /api/mobile/snapshot`. The HumanOS link state is surfaced
 * so a failed bridge exchange shows its real cause instead of a silent error.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val humanosGateway: HumanosGateway,
    private val authRepository: AuthRepository,
    private val googleCredentialManager: GoogleCredentialManager,
) : ViewModel() {

    data class DashboardUiState(
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val greeting: String = "Hola",
        val userName: String? = null,
        val tasksOpen: Int = 0,
        val tasksOverdue: Int = 0,
        val tasksDueToday: Int = 0,
        val tasks: List<TaskItem> = emptyList(),
        val todayCheckIn: CheckInDto? = null,
        val authState: AuthState = AuthState.Loading,
        val linkState: HumanosLinkState = HumanosLinkState.Unknown,
        val error: String? = null,
        val isSigningIn: Boolean = false,
        val isSubmittingCheckIn: Boolean = false,
    ) {
        /** True when signed into Firebase (regardless of bridge status). */
        val isSignedIn: Boolean get() = authState is AuthState.Authenticated
    }

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeTasks()
        observeAuth()
        observeLinkState()
        refresh()
    }

    private fun observeTasks() {
        viewModelScope.launch {
            taskRepository.observeTasks().collect { tasks ->
                _uiState.update { it.copy(tasks = tasks) }
            }
        }
    }

    private fun observeAuth() {
        viewModelScope.launch {
            authRepository.observeAuthState().collect { state ->
                val wasSignedIn = _uiState.value.isSignedIn
                _uiState.update { it.copy(authState = state) }
                // Pull fresh data the moment we become authenticated.
                if (!wasSignedIn && state is AuthState.Authenticated) refresh()
            }
        }
    }

    private fun observeLinkState() {
        viewModelScope.launch {
            authRepository.humanosLinkState.collect { link ->
                _uiState.update { it.copy(linkState = link) }
            }
        }
    }

    /** Pull the daily snapshot + sync tasks from HumanOS. */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            // Tasks: refresh Room from the server (observer updates the list).
            taskRepository.syncFromRemote()

            // Snapshot: greeting, counts, today's check-in.
            humanosGateway.fetchSnapshot()
                .onSuccess { snap ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = null,
                            greeting = greetingFor(snap.user.firstName),
                            userName = snap.user.firstName,
                            tasksOpen = snap.counts.tasksOpen,
                            tasksOverdue = snap.counts.tasksOverdue,
                            tasksDueToday = snap.counts.tasksDueToday,
                            todayCheckIn = snap.checkInToday,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = e.toHumanosErrorMessage(),
                        )
                    }
                }
        }
    }

    /** Google Sign-In, then exchange + refresh. [activityContext] must be an Activity. */
    fun signIn(activityContext: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSigningIn = true, error = null) }
            val result = googleCredentialManager.getGoogleIdToken(activityContext)
                .mapCatching { idToken -> authRepository.signInWithGoogle(idToken).getOrThrow() }
            _uiState.update {
                it.copy(
                    isSigningIn = false,
                    error = result.exceptionOrNull()?.let { e -> e.toHumanosErrorMessage() },
                )
            }
            if (result.isSuccess) refresh()
        }
    }

    /** Retry the bridge exchange (e.g. after a transient failure) then refresh. */
    fun retryConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            authRepository.refreshHumanosToken()
            refresh()
        }
    }

    /** Record today's wellbeing check-in (scores 1..5). */
    fun submitCheckIn(energy: Int, mood: Int, stress: Int, note: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingCheckIn = true) }
            humanosGateway.submitCheckIn(energy, mood, stress, note = note)
                .onSuccess { checkIn ->
                    _uiState.update {
                        it.copy(isSubmittingCheckIn = false, todayCheckIn = checkIn, error = null)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isSubmittingCheckIn = false, error = e.toHumanosErrorMessage())
                    }
                }
        }
    }

    /** Toggle a task done/pending (server-synced via the repository), then refresh. */
    fun toggleTaskDone(task: TaskItem) {
        viewModelScope.launch {
            val done = task.status != TaskStatus.DONE
            taskRepository.setDone(task, done)
                .onSuccess { refresh() }
                .onFailure { e -> _uiState.update { it.copy(error = e.toHumanosErrorMessage()) } }
        }
    }

    private fun greetingFor(name: String?): String {
        val part = when (LocalTime.now().hour) {
            in 5..11 -> "Buenos días"
            in 12..19 -> "Buenas tardes"
            else -> "Buenas noches"
        }
        return if (!name.isNullOrBlank()) "$part, $name" else part
    }
}
