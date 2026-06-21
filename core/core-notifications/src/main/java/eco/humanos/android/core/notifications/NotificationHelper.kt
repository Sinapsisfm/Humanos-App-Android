package eco.humanos.android.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts local notifications for new Claude-agent replies in the mobile bridge
 * thread. No FCM — this is fired locally by [AgentReplyPollWorker] after a
 * background poll finds a new "assistant" message.
 *
 * The notification channel ("Mensajes del agente") is created idempotently; on
 * API 26+ (the app's minSdk) channels are mandatory. Tapping the notification
 * relaunches the app via its launcher intent (resolved by package name so this
 * module stays decoupled from `:app`).
 */
@Singleton
class NotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    /**
     * Create (or update) the agent-messages channel. Safe to call repeatedly;
     * idempotent. Invoked once at app startup and defensively before posting.
     */
    fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Mensajes del agente",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Te avisa cuando el agente responde en el chat."
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * Show a notification for a new agent reply. No-op (returns false) if the
     * user hasn't granted POST_NOTIFICATIONS on API 33+, so we never crash on a
     * missing permission. [preview] is a short excerpt of the reply.
     */
    fun showAgentReply(preview: String): Boolean {
        if (!hasPermission()) return false
        ensureChannel()

        val intent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }

        val contentIntent = intent?.let {
            PendingIntent.getActivity(
                context,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("El agente te respondió")
            .setContentText(preview)
            .setStyle(NotificationCompat.BigTextStyle().bigText(preview))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .apply { contentIntent?.let(::setContentIntent) }
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post — fail closed.
            false
        }
    }

    /** Whether POST_NOTIFICATIONS is granted (always true below API 33). */
    private fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val CHANNEL_ID = "agent_messages"
        private const val NOTIFICATION_ID = 1001
    }
}
