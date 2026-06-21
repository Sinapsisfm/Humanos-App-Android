package eco.humanos.android.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-scoped Preferences DataStore for the agent-reply notification poller.
 *
 * Persists the **cursor** of what the user has already seen so the background
 * `AgentReplyPollWorker` only notifies once per new agent message:
 *  - [lastSeenAgentMessageId] — id of the most recent "assistant" message we
 *    already either notified for or that the user read in-app.
 *  - [lastPollIso] — ISO-8601 instant of the last successful poll, used as the
 *    `since` query so each poll fetches only what is new.
 *
 * Property-delegate DataStore (`Context.dataStore`) guarantees a single instance
 * per process for this file name, so it is safe to read it from both the worker
 * and the UI.
 */
private val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "humanos_notifications",
)

@Singleton
class NotificationPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val store get() = context.notificationDataStore

    /** Read the last agent-message id we have already surfaced, or null if none. */
    suspend fun lastSeenAgentMessageId(): String? =
        store.data.first()[KEY_LAST_SEEN_ID]

    /** Persist the last agent-message id surfaced (notified or read in-app). */
    suspend fun setLastSeenAgentMessageId(id: String) {
        store.edit { it[KEY_LAST_SEEN_ID] = id }
    }

    /** Read the ISO-8601 instant of the last successful poll, or null. */
    suspend fun lastPollIso(): String? =
        store.data.first()[KEY_LAST_POLL_ISO]

    /** Persist the ISO-8601 instant of the latest successful poll. */
    suspend fun setLastPollIso(iso: String) {
        store.edit { it[KEY_LAST_POLL_ISO] = iso }
    }

    private companion object {
        val KEY_LAST_SEEN_ID = stringPreferencesKey("last_seen_agent_message_id")
        val KEY_LAST_POLL_ISO = stringPreferencesKey("last_poll_iso")
    }
}
