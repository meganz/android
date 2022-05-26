package mega.privacy.android.app.service.push

import android.content.Context
import androidx.work.*
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.FirebaseMessaging
import mega.privacy.android.app.fcm.MegaRemoteMessage
import mega.privacy.android.app.fcm.NewTokenWorker
import mega.privacy.android.app.fcm.NewTokenWorker.Companion.DEVICE_TYPE
import mega.privacy.android.app.fcm.NewTokenWorker.Companion.NEW_TOKEN
import mega.privacy.android.app.fcm.NewTokenWorker.Companion.WORK_NAME
import mega.privacy.android.app.fcm.PushMessageWorker
import mega.privacy.android.app.utils.Constants.DEVICE_ANDROID
import timber.log.Timber
import java.util.concurrent.Executors

class MegaMessageService : FirebaseMessagingService() {

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
            .enqueue(
                OneTimeWorkRequestBuilder<PushMessageWorker>()
                    .setInputData(megaRemoteMessage.pushMessage.toData())
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
            )
    }

    override fun onNewToken(s: String) {
        Timber.d("New token is: $s")

        WorkManager.getInstance(this)
            .enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<NewTokenWorker>()
                    .setInputData(
                        Data.Builder()
                            .putString(NEW_TOKEN, s)
                            .putInt(DEVICE_TYPE, DEVICE_ANDROID)
                            .build()
                    )
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
            )
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
                        .enqueueUniqueWork(
                            WORK_NAME,
                            ExistingWorkPolicy.REPLACE,
                            OneTimeWorkRequestBuilder<NewTokenWorker>()
                                .setInputData(
                                    Data.Builder()
                                        .putString(NEW_TOKEN, token)
                                        .putInt(DEVICE_TYPE, DEVICE_ANDROID)
                                        .build()
                                ).setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                                .build()
                        )
                }
            }
        }
    }
}