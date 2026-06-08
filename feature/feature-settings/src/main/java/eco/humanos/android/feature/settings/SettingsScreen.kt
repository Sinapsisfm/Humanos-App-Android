package eco.humanos.android.feature.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eco.humanos.android.core.model.auth.AuthState
import eco.humanos.android.core.update.UpdateInfo

/**
 * Settings feature screen -- account, privacy, integrations,
 * permissions, and app version info.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val humanosLabel = if (uiState.isCheckingConnections) {
        "verificando..."
    } else if (uiState.humanosConnected) {
        "conectado"
    } else {
        "desconectado"
    }

    val quebotLabel = if (uiState.isCheckingConnections) {
        "verificando..."
    } else if (uiState.quebotConnected) {
        "conectado"
    } else {
        "desconectado"
    }

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
        uiState.availableUpdate?.let { update ->
            item {
                UpdateBanner(
                    update = update,
                    onDownload = { context.openUrl(update.downloadUrl) },
                )
            }
        }
        item {
            AccountListItem(
                authState = uiState.authState,
                isSigningIn = uiState.isSigningIn,
                authError = uiState.authError,
                onSignIn = { viewModel.signIn(context.findActivity()) },
                onSignOut = { viewModel.signOut() },
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
                    Text("HumanOS: $humanosLabel | QueBot: $quebotLabel")
                },
                leadingContent = {
                    if (uiState.isCheckingConnections) {
                        CircularProgressIndicator()
                    } else {
                        Icon(Icons.Outlined.Cloud, contentDescription = null)
                    }
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
            val versionLabel = uiState.currentVersionName.ifBlank { "0.1.0" }
            val updateStatus = if (uiState.availableUpdate == null) {
                "App actualizada"
            } else {
                "Actualizacion disponible: v${uiState.availableUpdate!!.versionName}"
            }
            ListItem(
                headlineContent = { Text("humanOS Android") },
                supportingContent = {
                    Text("v$versionLabel | Phase 1 Skeleton\n$updateStatus")
                },
                leadingContent = {
                    Icon(Icons.Outlined.Info, contentDescription = null)
                },
            )
        }
    }
}

/**
 * "Cuenta" row. Renders the Google Sign-In button when unauthenticated, or the
 * signed-in identity plus a sign-out action when authenticated.
 */
@Composable
private fun AccountListItem(
    authState: AuthState,
    isSigningIn: Boolean,
    authError: String?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
) {
    when (authState) {
        is AuthState.Authenticated -> {
            val primary = authState.displayName?.takeIf { it.isNotBlank() }
                ?: authState.email
                ?: "Sesion iniciada"
            ListItem(
                headlineContent = { Text("Cuenta") },
                supportingContent = {
                    Column {
                        Text(primary)
                        if (authState.email != null && authState.email != primary) {
                            Text(
                                text = authState.email!!,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        TextButton(onClick = onSignOut) {
                            Text("Cerrar sesion")
                        }
                    }
                },
                leadingContent = {
                    Icon(Icons.Outlined.AccountCircle, contentDescription = null)
                },
            )
        }

        else -> {
            // Unauthenticated, Loading, or TokenExpired all show the sign-in CTA.
            ListItem(
                headlineContent = { Text("Cuenta") },
                supportingContent = {
                    Column {
                        Text("No conectado")
                        OutlinedButton(
                            onClick = onSignIn,
                            enabled = !isSigningIn,
                        ) {
                            if (isSigningIn) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            } else {
                                Text("Iniciar sesion con Google")
                            }
                        }
                        if (authError != null) {
                            Text(
                                text = authError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
                leadingContent = {
                    Icon(Icons.Outlined.AccountCircle, contentDescription = null)
                },
            )
        }
    }
}

/**
 * Highlighted card shown at the top of Settings when a newer release exists. The
 * primary action opens the APK download URL in the browser; the user then taps
 * the downloaded file to install (sideload).
 */
@Composable
private fun UpdateBanner(
    update: UpdateInfo,
    onDownload: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ListItem(
                colors = androidx.compose.material3.ListItemDefaults.colors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
                headlineContent = {
                    Text("Nueva version v${update.versionName} disponible")
                },
                supportingContent = update.releaseNotes?.let { notes ->
                    {
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 4,
                        )
                    }
                },
                leadingContent = {
                    Icon(Icons.Outlined.SystemUpdate, contentDescription = null)
                },
            )
            Button(onClick = onDownload) {
                Text("Descargar actualizacion")
            }
        }
    }
}

/**
 * Opens [url] in the user's browser / download handler via an implicit
 * ACTION_VIEW intent. FLAG_ACTIVITY_NEW_TASK is required because this may be
 * launched from a non-Activity context.
 */
private fun Context.openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

/**
 * The Credential Manager needs an Activity context. Compose's [LocalContext] can
 * be a ContextWrapper (e.g. the themed context), so unwrap to the host Activity.
 */
private fun Context.findActivity(): Activity {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    error("SettingsScreen must be hosted in an Activity to start Google Sign-In.")
}
