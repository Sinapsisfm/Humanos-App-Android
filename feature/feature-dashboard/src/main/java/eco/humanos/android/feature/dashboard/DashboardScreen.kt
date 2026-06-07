package eco.humanos.android.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Dashboard feature screen -- landing view showing a daily summary,
 * pending tasks, recent captures, and current state indicators.
 */
@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "Buenos dias",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "humanOS Native Android",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tareas pendientes",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "3 tareas para hoy",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Capturas recientes",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "Sin capturas aun",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Estado",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "Energia: -  |  Humor: -  |  Estres: -",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
