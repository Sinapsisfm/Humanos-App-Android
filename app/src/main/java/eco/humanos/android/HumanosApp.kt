package eco.humanos.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import eco.humanos.android.core.notifications.AgentReplyNotifier
import javax.inject.Inject

@HiltAndroidApp
class HumanosApp : Application() {

    /**
     * Schedules the background poller that fires a local notification when the
     * Claude agent replies in the mobile bridge thread. Injected by Hilt; we
     * only kick off scheduling here (the periodic work itself runs in
     * WorkManager, not on the main thread).
     */
    @Inject
    lateinit var agentReplyNotifier: AgentReplyNotifier

    override fun onCreate() {
        super.onCreate()
        agentReplyNotifier.start()
    }
}
