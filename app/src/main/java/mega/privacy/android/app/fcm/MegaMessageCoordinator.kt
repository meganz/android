package mega.privacy.android.app.fcm

import androidx.work.Data
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.data.extensions.enqueuePushMessage
import mega.privacy.android.app.data.extensions.enqueueUniqueWorkNewToken
import mega.privacy.android.app.utils.Constants.DEVICE_ANDROID
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.domain.qualifier.ApplicationScope
import javax.inject.Inject

/**
 * Handles the push callbacks delivered to the messaging service.
 *
 * The work lives here rather than in the service so it is reachable without the Android framework:
 * the service is a platform component the system creates on its own schedule, which makes it a
 * poor place for logic and an impossible one to unit test.
 *
 * Takes the message payload as a plain map rather than a Firebase `RemoteMessage`, which keeps
 * this class free of the messaging SDK.
 */
class MegaMessageCoordinator @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val workManager: WorkManager,
    private val crashReporter: CrashReporter,
) {

    /**
     * Enqueues the work that handles a received push message.
     *
     * @param data the message payload.
     */
    fun onMessageReceived(data: Map<String, String>) {
        applicationScope.launch {
            workManager.enqueuePushMessage(data.toWorkerData(), crashReporter)
        }
    }

    /**
     * Enqueues the work that registers a new push token with the API.
     *
     * @param token the new push token.
     */
    fun onNewToken(token: String) {
        applicationScope.launch {
            workManager.enqueueUniqueWorkNewToken(token, DEVICE_ANDROID, crashReporter)
        }
    }

    private fun Map<String, String>.toWorkerData(): Data =
        Data.Builder()
            .apply { forEach { (key, value) -> putString(key, value) } }
            .build()
}
