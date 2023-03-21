package mega.privacy.android.app.service.push

import android.content.Context
import androidx.work.WorkManager
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.data.extensions.enqueuePushMessage
import mega.privacy.android.app.data.extensions.enqueueUniqueWorkNewToken
import mega.privacy.android.data.mapper.pushmessage.DataMapper
import mega.privacy.android.app.utils.Constants.DEVICE_ANDROID
import mega.privacy.android.domain.entity.pushes.MegaRemoteMessage
import timber.log.Timber
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class MegaMessageService : FirebaseMessagingService() {

    @Inject
    lateinit var dataMapper: DataMapper

    override fun onDestroy() {
        Timber.d("onDestroyFCM")
        super.onDestroy()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val megaRemoteMessage = MegaRemoteMessage(
            from = remoteMessage.from,
            originalPriority = remoteMessage.originalPriority,
            priority = remoteMessage.priority,
            data = remoteMessage.data
        )

        Timber.d("$megaRemoteMessage")

        WorkManager.getInstance(this)
            .enqueuePushMessage(dataMapper(megaRemoteMessage.pushMessage))
    }

    override fun onNewToken(s: String) {
        Timber.d("New token is: $s")

        WorkManager.getInstance(this)
            .enqueueUniqueWorkNewToken(s, DEVICE_ANDROID)
    }

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