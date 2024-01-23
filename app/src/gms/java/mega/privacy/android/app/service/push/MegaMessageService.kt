package mega.privacy.android.app.service.push

import androidx.work.Data
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import mega.privacy.android.app.data.extensions.enqueuePushMessage
import mega.privacy.android.app.data.extensions.enqueueUniqueWorkNewToken
import mega.privacy.android.app.utils.Constants.DEVICE_ANDROID
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.domain.qualifier.ApplicationScope
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MegaMessageService : FirebaseMessagingService() {

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var workManager: WorkManager

    @Inject
    lateinit var crashReporter: CrashReporter

    override fun onDestroy() {
        Timber.d("onDestroy")
        super.onDestroy()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.d("Remote Message: $remoteMessage")

        val workerData = remoteMessage.data.toWorkerData()

        applicationScope.launch {
            workManager.enqueuePushMessage(workerData, crashReporter)
        }
    }

    override fun onNewToken(token: String) {
        Timber.d("New token: $token")

        applicationScope.launch {
            workManager.enqueueUniqueWorkNewToken(token, DEVICE_ANDROID, crashReporter)
        }
    }

    /**
     * Convert RemoteMessage Data Map to Worker Data
     *
     * @return  Worker Data
     */
    private fun Map<String, String>.toWorkerData(): Data =
        Data.Builder()
            .apply { forEach { (key, value) -> putString(key, value) } }
            .build()

    companion object {
        /**
         * Mutex to avoid multiple calls to getToken.
         */
        private val mutex = Mutex()

        /**
         * Request push service token, then register it in API as an identifier of the device.
         */
        @JvmStatic
        suspend fun getToken(workManager: WorkManager, crashReporter: CrashReporter) {
            //project number from google-service.json
            mutex.withLock {
                runCatching {
                    val token = FirebaseMessaging.getInstance().token.await()

                    token?.let {
                        Timber.d("Get token succeeded")
                        workManager.enqueueUniqueWorkNewToken(token, DEVICE_ANDROID, crashReporter)
                    } ?: Timber.w("Get token failed.")
                }.onFailure {
                    Timber.e(it)
                }
            }
        }
    }
}
