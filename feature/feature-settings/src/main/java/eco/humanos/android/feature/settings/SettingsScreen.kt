package eco.humanos.android.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Settings feature screen -- account, privacy, integrations,
 * permissions, and app version info.
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = "Configuracion",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Cuenta") },
                supportingContent = { Text("No conectado") },
                leadingContent = {
                    Icon(Icons.Outlined.AccountCircle, contentDescription = null)
                },
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Privacidad") },
                supportingContent = { Text("Modo local-first activo") },
                leadingContent = {
                    Icon(Icons.Outlined.Shield, contentDescription = null)
                },
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Integraciones") },
                supportingContent = {
                    Text("HumanOS: desconectado | QueBot: desconectado")
                },
                leadingContent = {
                    Icon(Icons.Outlined.Cloud, contentDescription = null)
                },
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Permisos") },
                supportingContent = { Text("0 de 6 permisos otorgados") },
                leadingContent = {
                    Icon(Icons.Outlined.Security, contentDescription = null)
                },
            )
        }
        item {
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("humanOS Android") },
                supportingContent = {
                    Text("v0.1.0-dev | Phase 1 Skeleton\nGCP: humanos-app | compileSdk 36")
                },
                leadingContent = {
                    Icon(Icons.Outlined.Info, contentDescription = null)
                },
            )
        }
    }
}
