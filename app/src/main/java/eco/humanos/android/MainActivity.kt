package eco.humanos.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import eco.humanos.android.core.ui.theme.HumanosTheme
import eco.humanos.android.navigation.HumanosApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Result is handled by the system; the AgentReplyNotifier checks the
    // permission at post time, so we only need to ask — no callback work.
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeRequestNotificationPermission()
        setContent {
            HumanosTheme {
                HumanosApp()
            }
        }
    }

    /**
     * On API 33+ POST_NOTIFICATIONS is a runtime permission; ask for it once so
     * the background agent-reply poller can actually surface notifications. On
     * older APIs it is granted at install time, so this is a no-op there.
     */
    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
