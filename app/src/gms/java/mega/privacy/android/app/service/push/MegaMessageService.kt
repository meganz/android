package mega.privacy.android.app.service.push

import android.content.Context
import androidx.work.Data
import androidx.work.WorkManager
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.data.extensions.enqueuePushMessage
import mega.privacy.android.app.data.extensions.enqueueUniqueWorkNewToken
import mega.privacy.android.app.utils.Constants.DEVICE_ANDROID
import timber.log.Timber
import java.util.concurrent.Executors

@AndroidEntryPoint
class MegaMessageService : FirebaseMessagingService() {

    override fun onDestroy() {
        Timber.d("onDestroy")
        super.onDestroy()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.d("Remote Message: $remoteMessage")

        val workerData = remoteMessage.data.toWorkerData()

        WorkManager.getInstance(this)
            .enqueuePushMessage(workerData)
    }

    override fun onNewToken(token: String) {
        Timber.d("New token: $token")

        WorkManager.getInstance(this)
            .enqueueUniqueWorkNewToken(token, DEVICE_ANDROID)
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
         * Request push service token, then register it in API as an identifier of the device.
         *
         * @param context       Context.
         */
        @JvmStatic
        fun getToken(context: Context) {
            //project number from google-service.json
            Executors.newFixedThreadPool(1).submit {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task: Task<String> ->
                    if (!task.isSuccessful) {
                        Timber.w("Get token failed.")
                        return@addOnCompleteListener
                    }

                    // Get new Instance ID token
                    val token = task.result
                    Timber.d("Get token: $token")

                    WorkManager.getInstance(context)
                        .enqueueUniqueWorkNewToken(token, DEVICE_ANDROID)
                }
            }
        }
    }
}
