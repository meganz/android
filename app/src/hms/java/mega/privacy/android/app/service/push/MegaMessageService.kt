package mega.privacy.android.app.service.push

import android.content.Context
import androidx.work.*
import com.huawei.agconnect.AGConnectOptionsBuilder
import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.common.ApiException
import mega.privacy.android.app.fcm.MegaRemoteMessage
import mega.privacy.android.app.fcm.NewTokenWorker
import mega.privacy.android.app.fcm.NewTokenWorker.Companion.DEVICE_TYPE
import mega.privacy.android.app.fcm.NewTokenWorker.Companion.NEW_TOKEN
import mega.privacy.android.app.fcm.NewTokenWorker.Companion.WORK_NAME
import mega.privacy.android.app.fcm.PushMessageWorker
import mega.privacy.android.app.utils.Constants.DEVICE_HUAWEI
import timber.log.Timber
import java.util.concurrent.Executors

class MegaMessageService : HmsMessageService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val megaRemoteMessage = MegaRemoteMessage(
            from = remoteMessage.from,
            originalPriority = remoteMessage.originalUrgency,
            priority = remoteMessage.urgency,
            data = remoteMessage.dataOfMap
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
                            .putInt(DEVICE_TYPE, DEVICE_HUAWEI)
                            .build()
                    )
                    .build()
            )
    }

    companion object {
        /**
         * Request push service token by sending appId to HMS, then register it in API as an identifier of the device.
         *
         * @param context   Context.
         */
        @JvmStatic
        fun getToken(context: Context) {
            Executors.newFixedThreadPool(1).submit {
                val appId = AGConnectOptionsBuilder().build(context).getString("client/app_id")
                try {
                    // Wait for the callback
                    val token = HmsInstanceId.getInstance(context).getToken(appId, "HCM")
                    Timber.d("Get token: $token")

                    WorkManager.getInstance(context)
                        .enqueueUniqueWork(
                            WORK_NAME,
                            ExistingWorkPolicy.REPLACE,
                            OneTimeWorkRequestBuilder<NewTokenWorker>()
                                .setInputData(
                                    Data.Builder()
                                        .putString(NEW_TOKEN, token)
                                        .putInt(DEVICE_TYPE, DEVICE_HUAWEI)
                                        .build()
                                )
                                .build()
                        )
                } catch (e: ApiException) {
                    Timber.e(e.message, e)
                    e.printStackTrace()
                }
            }
        }
    }
}