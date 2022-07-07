package mega.privacy.android.app.service.push

import android.content.Context
import androidx.work.WorkManager
import com.huawei.agconnect.AGConnectOptionsBuilder
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.common.ApiException
import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.data.extensions.enqueuePushMessage
import mega.privacy.android.app.data.extensions.enqueueUniqueWorkNewToken
import mega.privacy.android.app.data.mapper.DataMapper
import mega.privacy.android.app.utils.Constants.DEVICE_HUAWEI
import mega.privacy.android.domain.entity.pushes.MegaRemoteMessage
import timber.log.Timber
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class MegaMessageService : HmsMessageService() {

    @Inject
    lateinit var dataMapper: DataMapper

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val megaRemoteMessage = MegaRemoteMessage(
            from = remoteMessage.from,
            originalPriority = remoteMessage.originalUrgency,
            priority = remoteMessage.urgency,
            data = remoteMessage.dataOfMap
        )

        Timber.d("$megaRemoteMessage")

        WorkManager.getInstance(this)
            .enqueuePushMessage(dataMapper(megaRemoteMessage.pushMessage))
    }

    override fun onNewToken(s: String) {
        Timber.d("New token is: $s")

        WorkManager.getInstance(this)
            .enqueueUniqueWorkNewToken(s, DEVICE_HUAWEI)
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
                        .enqueueUniqueWorkNewToken(token, DEVICE_HUAWEI)
                } catch (e: ApiException) {
                    Timber.e(e.message, e)
                    e.printStackTrace()
                }
            }
        }
    }
}