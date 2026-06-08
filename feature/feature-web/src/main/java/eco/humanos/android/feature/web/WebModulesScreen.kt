package eco.humanos.android.feature.web

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Hub of embedded web modules (ADR-0006). Each card opens the module inside an
 * authenticated WebView. Adding a module = one entry in [WebModule].
 */
@Composable
fun WebModulesScreen(
    onOpen: (moduleKey: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Módulos", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Abren dentro de la app, ya con tu sesión de HumanOS",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(WebModule.entries.toList(), key = { it.key }) { module ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(module.key) },
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        module.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        module.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
