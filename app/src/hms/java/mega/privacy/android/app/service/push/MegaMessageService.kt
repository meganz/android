package mega.privacy.android.app.service.push

import android.content.Context
import com.huawei.agconnect.AGConnectOptionsBuilder
import com.huawei.hms.push.HmsMessageService
import mega.privacy.android.app.middlelayer.push.PushMessageHandler
import com.huawei.hms.push.RemoteMessage
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.common.ApiException
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.di.MegaApiFolder
import mega.privacy.android.app.utils.Constants
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import timber.log.Timber
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class MegaMessageService : HmsMessageService() {

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
        Timber.d("HMS message service created")
        messageHandler = PushMessageHandler(this, megaApi, megaApiFolder, megaChatApi, dbH)
    }

    /**
     * Convert Huawei RemoteMessage object into generic Message object.
     *
     * @param remoteMessage Huawei RemoteMessage.
     * @return Generic Message object.
     */
    private fun convert(remoteMessage: RemoteMessage): PushMessageHandler.Message {
        return PushMessageHandler.Message(
            remoteMessage.from,
            remoteMessage.originalUrgency,
            remoteMessage.urgency,
            remoteMessage.dataOfMap
        )
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val message = convert(remoteMessage)
        Timber.d("Receive remote msg: $message")
        messageHandler?.handleMessage(message)
    }

    override fun onNewToken(s: String) {
        Timber.d("New token is: $s")
        messageHandler?.sendRegistrationToServer(s, Constants.DEVICE_HUAWEI)
    }

    companion object {
        /**
         * Request push service token by sending appId to HMS, then register it in API as an identifier of the device.
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
            Executors.newFixedThreadPool(1).submit {
                val appId = AGConnectOptionsBuilder().build(context).getString("client/app_id")
                try {
                    // Wait for the callback
                    val token = HmsInstanceId.getInstance(context).getToken(appId, "HCM")
                    Timber.d("Get token: $token")
                    PushMessageHandler(context, megaApi, megaApiFolder, megaChatApi, dbH)
                        .sendRegistrationToServer(token, Constants.DEVICE_HUAWEI)
                } catch (e: ApiException) {
                    Timber.e(e.message, e)
                    e.printStackTrace()
                }
            }
        }
    }
}