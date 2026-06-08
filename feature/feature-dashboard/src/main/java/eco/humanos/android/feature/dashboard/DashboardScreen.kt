package eco.humanos.android.feature.dashboard

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eco.humanos.android.core.model.auth.AuthState
import eco.humanos.android.core.model.auth.HumanosLinkState
import eco.humanos.android.core.model.task.TaskItem
import eco.humanos.android.core.model.task.TaskStatus

/**
 * Dashboard -- the daily landing view: connection state, greeting, task counts,
 * today's wellbeing check-in, and the open task list (real data from HumanOS).
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showCheckIn by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<TaskItem?>(null) }

    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(uiState.greeting, style = MaterialTheme.typography.headlineMedium)
            Text(
                "humanOS",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // Connection / bridge state.
        item {
            ConnectionBanner(
                authState = uiState.authState,
                linkState = uiState.linkState,
                isSigningIn = uiState.isSigningIn,
                error = uiState.error,
                onSignIn = { viewModel.signIn(context.findActivity()) },
                onRetry = { viewModel.retryConnection() },
            )
        }

        if (uiState.isSignedIn && uiState.linkState !is HumanosLinkState.Failed) {
            // Counts.
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip("Abiertas", uiState.tasksOpen, Modifier.weight(1f))
                    StatChip("Hoy", uiState.tasksDueToday, Modifier.weight(1f))
                    StatChip("Atrasadas", uiState.tasksOverdue, Modifier.weight(1f))
                }
            }

            // Today's state.
            item {
                StateCard(
                    checkIn = uiState.todayCheckIn,
                    isSubmitting = uiState.isSubmittingCheckIn,
                    onRegister = { showCheckIn = true },
                )
            }

            // Tasks header.
            item {
                Text(
                    "Tareas (${uiState.tasks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (uiState.tasks.isEmpty()) {
                item {
                    Text(
                        "Sin tareas pendientes. Crea una desde la pestaña Tareas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(uiState.tasks, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    onToggle = { viewModel.toggleTaskDone(task) },
                    onClick = { selectedTask = task },
                )
            }
        }
    }

    if (showCheckIn) {
        CheckInDialog(
            onDismiss = { showCheckIn = false },
            onSubmit = { energy, mood, stress, note ->
                viewModel.submitCheckIn(energy, mood, stress, note)
                showCheckIn = false
            },
        )
    }

    selectedTask?.let { task ->
        TaskDetailDialog(
            task = task,
            onToggle = { viewModel.toggleTaskDone(task) },
            onDismiss = { selectedTask = null },
        )
    }
}

@Composable
private fun ConnectionBanner(
    authState: AuthState,
    linkState: HumanosLinkState,
    isSigningIn: Boolean,
    error: String?,
    onSignIn: () -> Unit,
    onRetry: () -> Unit,
) {
    when {
        authState !is AuthState.Authenticated -> {
            BannerCard(
                container = MaterialTheme.colorScheme.secondaryContainer,
                title = "Conecta tu cuenta",
                body = "Inicia sesión con Google para sincronizar tus tareas y estado con HumanOS.",
            ) {
                Button(onClick = onSignIn, enabled = !isSigningIn) {
                    if (isSigningIn) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Iniciar sesión con Google")
                    }
                }
            }
        }

        linkState is HumanosLinkState.Failed -> {
            BannerCard(
                container = MaterialTheme.colorScheme.errorContainer,
                title = "No se pudo conectar a HumanOS",
                body = linkState.reason,
            ) {
                OutlinedButton(onClick = onRetry) { Text("Reintentar conexión") }
            }
        }

        error != null -> {
            BannerCard(
                container = MaterialTheme.colorScheme.errorContainer,
                title = "Error al cargar",
                body = error,
            ) {
                OutlinedButton(onClick = onRetry) { Text("Reintentar") }
            }
        }
    }
}

@Composable
private fun BannerCard(
    container: androidx.compose.ui.graphics.Color,
    title: String,
    body: String,
    action: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            action()
        }
    }
}

@Composable
private fun StatChip(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("$value", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun StateCard(
    checkIn: eco.humanos.android.integrations.humanos.dto.CheckInDto?,
    isSubmitting: Boolean,
    onRegister: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Estado de hoy", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (checkIn != null) {
                Text(
                    "Energía ${checkIn.energy}/5  ·  Ánimo ${checkIn.mood}/5  ·  Estrés ${checkIn.stress}/5",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = onRegister, enabled = !isSubmitting) {
                    Text("Actualizar estado")
                }
            } else {
                Text(
                    "Aún no registras cómo te sientes hoy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onRegister, enabled = !isSubmitting) {
                    if (isSubmitting) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Registrar estado")
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: TaskItem, onToggle: () -> Unit, onClick: () -> Unit) {
    val done = task.status == TaskStatus.DONE
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = done, onCheckedChange = { onToggle() })
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                )
                Text(
                    text = "Prioridad ${task.priority.name.lowercase()} · ${task.status.name.lowercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Unwrap the host Activity from a (possibly wrapped) Compose context. */
private fun Context.findActivity(): Activity {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    error("DashboardScreen must be hosted in an Activity for Google Sign-In.")
}
