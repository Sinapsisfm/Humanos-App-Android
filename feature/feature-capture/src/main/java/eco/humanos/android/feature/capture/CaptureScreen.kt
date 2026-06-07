package eco.humanos.android.feature.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Capture feature screen -- quick-entry form for notes, ideas, and
 * pending items with shortcuts for photo, voice, and file attachment.
 */
@Composable
fun CaptureScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Captura rapida",
            style = MaterialTheme.typography.headlineMedium,
        )

        var textInput by remember { mutableStateOf("") }
        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            label = { Text("Escribe una nota, idea o pendiente...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { /* TODO: camera */ }) {
                Icon(Icons.Outlined.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Foto")
            }
            FilledTonalButton(onClick = { /* TODO: voice */ }) {
                Icon(Icons.Outlined.Mic, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Voz")
            }
            FilledTonalButton(onClick = { /* TODO: file */ }) {
                Icon(Icons.Outlined.AttachFile, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Archivo")
            }
        }

        Button(
            onClick = { /* TODO: save capture */ },
            modifier = Modifier.fillMaxWidth(),
            enabled = textInput.isNotBlank(),
        ) {
            Text("Guardar captura")
        }
    }
}
