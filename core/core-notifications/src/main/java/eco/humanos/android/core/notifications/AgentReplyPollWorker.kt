package eco.humanos.android.core.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import eco.humanos.android.core.datastore.NotificationPreferences
import eco.humanos.android.integrations.humanos.HumanosGateway

/**
 * Background poller (WorkManager) that detects new Claude-agent replies in the
 * mobile bridge thread and fires a **local** notification — the safe, FCM-free
 * half of "avisar cuando el agente responde".
 *
 * ## Dependency wiring
 * The app does not register a `HiltWorkerFactory`, so this is a plain
 * [CoroutineWorker] that resolves its collaborators through a Hilt
 * [EntryPoint] on the application's `SingletonComponent`. This keeps the change
 * self-contained (no `androidx.hilt:hilt-work`, no custom WorkManager
 * `Configuration.Provider` in `:app`).
 *
 * ## Algorithm
 * 1. Read the stored `since` cursor + last-seen agent message id.
 * 2. `GET /api/mobile/message?since=…` via the existing bridge-JWT gateway.
 * 3. Find the newest "assistant" message. If it differs from the last-seen id,
 *    post the notification and persist the new id.
 * 4. Advance the `since` cursor to the newest message's timestamp so the next
 *    poll only fetches what is new.
 *
 * Failures (no session yet, offline, HTTP error) return [Result.success] with no
 * side effects: this is a best-effort poller, not a sync that must retry. The
 * periodic schedule will simply try again next interval.
 */
class AgentReplyPollWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun gateway(): HumanosGateway
        fun preferences(): NotificationPreferences
        fun notificationHelper(): NotificationHelper
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(
            applicationContext,
            Deps::class.java,
        )
        val gateway = deps.gateway()
        val prefs = deps.preferences()
        val helper = deps.notificationHelper()

        val since = prefs.lastPollIso()
        val messages = gateway.fetchMessages(since).getOrElse {
            // Not signed in / offline / HTTP error → quietly try again next cycle.
            return Result.success()
        }
        if (messages.isEmpty()) return Result.success()

        // Advance the poll cursor to the newest message we saw, regardless of
        // role, so we never re-fetch the same window.
        messages.mapNotNull { it.createdAtIso }.maxOrNull()?.let { newest ->
            prefs.setLastPollIso(newest)
        }

        val latestAgent = messages.lastOrNull { it.isFromAgent } ?: return Result.success()
        val lastSeen = prefs.lastSeenAgentMessageId()
        if (latestAgent.id == lastSeen) return Result.success()

        // A genuinely new agent reply: surface it, then remember it so we don't
        // notify twice for the same message.
        val preview = latestAgent.content.trim().take(PREVIEW_MAX_CHARS).ifBlank {
            "Tienes una nueva respuesta."
        }
        helper.showAgentReply(preview)
        prefs.setLastSeenAgentMessageId(latestAgent.id)

        return Result.success()
    }

    private companion object {
        const val PREVIEW_MAX_CHARS = 140
    }
}
