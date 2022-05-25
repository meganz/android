package mega.privacy.android.app.service.push

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.google.android.gms.tasks.Task
import dagger.hilt.android.AndroidEntryPoint
import com.google.firebase.messaging.FirebaseMessagingService
import javax.inject.Inject
import nz.mega.sdk.MegaApiAndroid
import mega.privacy.android.app.di.MegaApiFolder
import nz.mega.sdk.MegaChatApiAndroid
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.middlelayer.push.PushMessageHandler
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.FirebaseMessaging
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.fcm.MegaRemoteMessage
import mega.privacy.android.app.fcm.PushMessageWorker
import mega.privacy.android.app.utils.Constants
import timber.log.Timber
import java.util.concurrent.Executors

@AndroidEntryPoint
class MegaMessageService : FirebaseMessagingService() {

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    @MegaApiFolder
    @Inject
    lateinit var megaApiFolder: MegaApiAndroid

    @Inject
    lateinit var megaChatApi: MegaChatApiAndroid

    @Inject
    lateinit var dbH: DatabaseHandler

    private var messageHandler: PushMessageHandler? = null

    override fun onCreate() {
        super.onCreate()
        Timber.d("onCreateFCM")
        messageHandler = PushMessageHandler(this, megaApi, megaApiFolder, megaChatApi, dbH)
    }

    override fun onDestroy() {
        Timber.d("onDestroyFCM")
        super.onDestroy()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
//        val message = convert(remoteMessage)
//        Timber.d("Receive remote msg: $message")
//        messageHandler?.handleMessage(message)

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
        messageHandler?.sendRegistrationToServer(s, Constants.DEVICE_ANDROID)
    }

//    /**
//     * Convert Google RemoteMessage object into generic Message object.
//     *
//     * @param remoteMessage Google RemoteMessage.
//     * @return Generic Message object.
//     */
//    private fun convert(remoteMessage: RemoteMessage): PushMessageHandler.Message =
//        PushMessageHandler.Message(
//            remoteMessage.from ?: "",
//            remoteMessage.originalPriority,
//            remoteMessage.priority,
//            remoteMessage.data
//        )

    companion object {
        /**
         * Request push service token, then register it in API as an identifier of the device.
         *
         * @param context       Context.
         * @param megaApi       MegaApi instance.
         * @param megaApiFolder MegaApi instance for folder links.
         * @param megaChatApi   MegaChatApi instance.
         * @param dbH           DatabaseHandler.
         */
        @JvmStatic
        fun getToken(
            context: Context, megaApi: MegaApiAndroid, megaApiFolder: MegaApiAndroid,
            megaChatApi: MegaChatApiAndroid, dbH: DatabaseHandler
        ) {
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

                    PushMessageHandler(context, megaApi, megaApiFolder, megaChatApi, dbH)
                        .sendRegistrationToServer(token, Constants.DEVICE_ANDROID)
                }
            }
        }
    }
}