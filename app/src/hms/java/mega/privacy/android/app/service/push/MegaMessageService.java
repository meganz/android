package mega.privacy.android.app.service.push;

import android.content.Context;

import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;

import java.util.concurrent.Executors;

import mega.privacy.android.app.middlelayer.push.PushMessageHanlder;

import static mega.privacy.android.app.utils.Constants.DEVICE_HUAWEI;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logError;

public class MegaMessageService extends HmsMessageService {

    private PushMessageHanlder messageHanlder;

    @Override
    public void onCreate() {
        super.onCreate();
        logDebug("HMS message service created");
        messageHanlder = new PushMessageHanlder();
    }

    /**
     * Convert Huawei RemoteMessage object into generic Message object.
     *
     * @param remoteMessage Huawei RemoteMessage.
     * @return Generic Message object.
     */
    private PushMessageHanlder.Message convert(RemoteMessage remoteMessage) {
        return new PushMessageHanlder.Message(
                remoteMessage.getFrom(),
                remoteMessage.getOriginalUrgency(),
                remoteMessage.getUrgency(),
                remoteMessage.getDataOfMap());
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        PushMessageHanlder.Message message = convert(remoteMessage);
        logDebug("Receive remote msg: " + message);
        messageHanlder.handleMessage(message);
    }

    @Override
    public void onNewToken(String s) {
        logDebug("New token is: " + s);
        messageHanlder.sendRegistrationToServer(s, DEVICE_HUAWEI);
    }

    /**
     * Request push service token by sending appId to HMS, then register it in API as an identifier of the device.
     *
     * @param context Context.
     */
    public static void getToken(Context context) {
        Executors.newFixedThreadPool(1).submit(() -> {
            String appId = AGConnectServicesConfig.fromContext(context).getString("client/app_id");
            try {
                // Wait for the callback
                String token = HmsInstanceId.getInstance(context).getToken(appId, "HCM");
                logDebug("Get token: " + token);
                new PushMessageHanlder().sendRegistrationToServer(token, DEVICE_HUAWEI);
            } catch (ApiException e) {
                logError(e.getMessage(), e);
                e.printStackTrace();
            }
        });
    }
}
