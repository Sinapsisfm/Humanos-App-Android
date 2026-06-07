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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Capture feature screen -- quick-entry form for notes, ideas, and
 * pending items with shortcuts for photo, voice, and file attachment.
 */
@Composable
fun CaptureScreen(
    viewModel: CaptureViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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

        OutlinedTextField(
            value = uiState.textInput,
            onValueChange = viewModel::updateText,
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
            onClick = viewModel::saveCapture,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.textInput.isNotBlank() && !uiState.isSaving,
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .height(18.dp)
                        .width(18.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text("Guardar captura")
        }

        if (uiState.savedMessage != null) {
            Text(
                text = uiState.savedMessage ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
