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
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import eco.humanos.android.core.ui.theme.HumanosTheme
import eco.humanos.android.integrations.humanos.HumanosGateway
import eco.humanos.android.navigation.HumanosApp
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // The HumanOS gateway, used to register this device's FCM token on startup so
    // existing installs (where onNewToken won't fire) still get push targeting.
    @Inject
    lateinit var humanosGateway: HumanosGateway

    // Result is handled by the system; the AgentReplyNotifier checks the
    // permission at post time, so we only need to ask — no callback work.
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeRequestNotificationPermission()
        registerFcmTokenBestEffort()
        setContent {
            HumanosTheme {
                HumanosApp()
            }
        }
    }

    /**
     * Best-effort: fetch the current FCM token and register it with the backend.
     * Covers existing installs where [HumanosMessagingService.onNewToken] won't
     * fire again. If there is no HumanOS session yet the gateway call fails
     * silently and [HumanosMessagingService] retries on the next refresh / start.
     * Wrapped so a missing google-services.json or Firebase error never crashes.
     */
    private fun registerFcmTokenBestEffort() {
        try {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                if (token.isNullOrBlank()) return@addOnSuccessListener
                lifecycleScope.launch {
                    runCatching { humanosGateway.registerFcmToken(token) }
                }
            }
        } catch (_: Exception) {
            // Firebase unavailable (e.g. no google-services.json) — ignore.
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
