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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eco.humanos.android.core.model.capture.CaptureItem
import java.util.concurrent.TimeUnit

/**
 * Capture feature screen -- quick-entry form for notes, ideas, and
 * pending items with shortcuts for photo, voice, and file attachment.
 * Persisted captures are listed below the entry form.
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

        if (uiState.captures.isNotEmpty()) {
            Text(
                text = "Capturas recientes",
                style = MaterialTheme.typography.titleMedium,
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.captures, key = { it.id }) { capture ->
                    CaptureCard(capture = capture)
                }
            }
        }
    }
}

@Composable
private fun CaptureCard(
    capture: CaptureItem,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = capture.textContent ?: capture.title ?: "(sin contenido)",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = relativeTime(capture.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Best-effort relative-time label for a capture timestamp.
 * Pure function over [System.currentTimeMillis] so it needs no locale data.
 */
private fun relativeTime(epochMillis: Long): String {
    val deltaMillis = (System.currentTimeMillis() - epochMillis).coerceAtLeast(0L)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(deltaMillis)
    val hours = TimeUnit.MILLISECONDS.toHours(deltaMillis)
    val days = TimeUnit.MILLISECONDS.toDays(deltaMillis)
    return when {
        minutes < 1L -> "hace un momento"
        minutes < 60L -> "hace ${minutes} min"
        hours < 24L -> "hace ${hours} h"
        days < 7L -> "hace ${days} d"
        else -> "hace ${days / 7L} sem"
    }
}
