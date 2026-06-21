package eco.humanos.android.core.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Entry point for the local agent-reply notifier. Call [start] once at app
 * startup (from the `Application`): it creates the notification channel and
 * schedules the periodic [AgentReplyPollWorker].
 *
 * The poll runs every [POLL_INTERVAL_MINUTES] minutes (WorkManager's periodic
 * floor is 15 min) and only when the network is connected. The work is
 * **unique** + KEEP, so repeated [start] calls don't pile up duplicate jobs.
 */
@Singleton
class AgentReplyNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val notificationHelper: NotificationHelper,
) {
    fun start() {
        // Channel must exist before the first notification; creating it at
        // startup also makes it visible in system settings immediately.
        notificationHelper.ensureChannel()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<AgentReplyPollWorker>(
            POLL_INTERVAL_MINUTES,
            TimeUnit.MINUTES,
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** Cancel the periodic poll (e.g. on sign-out). */
    fun stop() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private companion object {
        const val WORK_NAME = "agent_reply_poll"
        const val POLL_INTERVAL_MINUTES = 15L
    }
}
