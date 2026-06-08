package eco.humanos.android.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eco.humanos.android.core.model.task.TaskItem
import eco.humanos.android.core.model.task.TaskStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Read-only detail of a task (title, status, priority, description, due date,
 * tags, source) with a one-tap done/undone action. Surfaces the fields the list
 * row can't show, so the task is more than just a checkbox.
 */
@Composable
fun TaskDetailDialog(
    task: TaskItem,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
) {
    val done = task.status == TaskStatus.DONE
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(task.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("Estado", if (done) "Hecha" else statusLabel(task.status))
                DetailRow("Prioridad", priorityLabel(task.priority.name))
                task.description?.takeIf { it.isNotBlank() }?.let { DetailRow("Descripción", it) }
                task.dueDate?.let { DetailRow("Vence", formatDate(it)) }
                task.tags.takeIf { it.isNotEmpty() }?.let { DetailRow("Etiquetas", it.joinToString(", ")) }
                DetailRow("Origen", task.source.name.lowercase())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onToggle()
                onDismiss()
            }) {
                Text(if (done) "Marcar pendiente" else "Marcar hecha")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun statusLabel(status: TaskStatus): String = when (status) {
    TaskStatus.PENDING -> "Pendiente"
    TaskStatus.IN_PROGRESS -> "En progreso"
    TaskStatus.DONE -> "Hecha"
    TaskStatus.CANCELLED -> "Cancelada"
}

private fun priorityLabel(raw: String): String = when (raw.uppercase()) {
    "LOW" -> "Baja"
    "MEDIUM" -> "Media"
    "HIGH" -> "Alta"
    "CRITICAL" -> "Urgente"
    else -> raw.lowercase()
}

private val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

private fun formatDate(epochMillis: Long): String =
    runCatching {
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
    }.getOrDefault("—")
