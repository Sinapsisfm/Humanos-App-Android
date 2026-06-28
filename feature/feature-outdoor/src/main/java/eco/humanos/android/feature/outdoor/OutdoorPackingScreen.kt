/**
 * feature-outdoor / OutdoorPackingScreen.kt
 *
 * Pantalla R1 (Compose) de la lista de empaque de camping. STATELESS: recibe el estado
 * y callbacks; NO está cableada a la navegación de la app (módulo aislado). Vocabulario
 * "preparación/avisos", nunca "seguro" (SCN-007).
 */
package eco.humanos.android.feature.outdoor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OutdoorPackingScreen(
    state: OutdoorPackingUiState,
    onTogglePacked: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column {
                Text(state.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    "Preparación: ${state.progress.packed}/${state.progress.total} empacado · " +
                        "${state.summary.mandatoryPending} obligatorios pendientes",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (state.signals.isNotEmpty()) {
            item { Text("Avisos", style = MaterialTheme.typography.titleMedium) }
            items(state.signals, key = { it.code }) { signal ->
                Text("• ${signal.title}", style = MaterialTheme.typography.bodySmall)
            }
        }

        state.groups.forEach { group ->
            item { Text(group.category.name, style = MaterialTheme.typography.titleMedium) }
            items(group.items, key = { it.key }) { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = item.packed,
                        onCheckedChange = { checked -> onTogglePacked(item.key, checked) },
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("${item.name} ×${item.quantity} (${item.unit})")
                }
            }
        }
    }
}
