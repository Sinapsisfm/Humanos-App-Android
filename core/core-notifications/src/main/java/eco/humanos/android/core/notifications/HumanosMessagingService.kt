package eco.humanos.android.core.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import eco.humanos.android.integrations.humanos.HumanosGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Firebase Cloud Messaging service — the real push half of "avisar cuando el
 * agente responde". Complements the FCM-free [AgentReplyPollWorker] fallback.
 *
 * ## Dependency wiring
 * `FirebaseMessagingService` is instantiated by the system, not Hilt, so (like
 * [AgentReplyPollWorker]) it resolves its collaborators through a Hilt
 * [EntryPoint] on the application's `SingletonComponent` rather than via
 * constructor injection.
 *
 * ## What it does
 *  - [onNewToken]: best-effort register the refreshed token with the backend so
 *    pushes can target this install. Failure (e.g. not signed in yet) is ignored;
 *    the next refresh / app start retries.
 *  - [onMessageReceived]: fires only while the app is in the **foreground** (the
 *    system tray handles background/killed via the message's `notification`
 *    block). Surfaces the reply as a local notification through
 *    [NotificationHelper], which already gates on POST_NOTIFICATIONS.
 */
class HumanosMessagingService : FirebaseMessagingService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun gateway(): HumanosGateway
        fun notificationHelper(): NotificationHelper
    }

    private val deps: Deps by lazy {
        EntryPointAccessors.fromApplication(applicationContext, Deps::class.java)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Best-effort registration; if there is no HumanOS session yet the call
        // fails and we simply retry on the next refresh / app start.
        CoroutineScope(Dispatchers.IO).launch {
            deps.gateway().registerFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val preview = message.notification?.body
            ?: message.data["body"]
            ?: "Tienes una nueva respuesta."
        deps.notificationHelper().showAgentReply(preview)
    }
}
