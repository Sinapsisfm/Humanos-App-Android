package eco.humanos.android.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Quick wellbeing check-in: energy / mood / stress on a 1..5 scale plus an
 * optional note. Submits via the dashboard's [DashboardViewModel.submitCheckIn].
 */
@Composable
fun CheckInDialog(
    onDismiss: () -> Unit,
    onSubmit: (energy: Int, mood: Int, stress: Int, note: String?) -> Unit,
) {
    var energy by remember { mutableFloatStateOf(3f) }
    var mood by remember { mutableFloatStateOf(3f) }
    var stress by remember { mutableFloatStateOf(3f) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Cómo te sientes hoy?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ScoreSlider("Energía", energy) { energy = it }
                ScoreSlider("Ánimo", mood) { mood = it }
                ScoreSlider("Estrés", stress) { stress = it }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSubmit(
                    energy.roundToInt(),
                    mood.roundToInt(),
                    stress.roundToInt(),
                    note.ifBlank { null },
                )
            }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun ScoreSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("${value.roundToInt()}/5", style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 1f..5f,
            steps = 3, // 1,2,3,4,5
        )
    }
}
