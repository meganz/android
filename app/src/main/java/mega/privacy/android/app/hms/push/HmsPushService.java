package mega.privacy.android.app.hms.push;

import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;

import middlelayer.PushMessageHanlder;

import static mega.privacy.android.app.utils.LogUtil.logDebug;


public class HmsPushService extends HmsMessageService {

    private PushMessageHanlder messageHanlder;

    @Override
    public void onCreate() {
        super.onCreate();
        logDebug("HMS message created");
        messageHanlder = new PushMessageHanlder();
    }

    private PushMessageHanlder.Message convert(RemoteMessage remoteMessage) {
        return new PushMessageHanlder.Message(
                remoteMessage.getFrom(),
                remoteMessage.getOriginalUrgency(),
                remoteMessage.getUrgency(),
                remoteMessage.getDataOfMap());
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        logDebug("onMessageReceived");
        messageHanlder.handleMessage(convert(remoteMessage));
    }

    @Override
    public void onNewToken(String s) {
        logDebug("New token is: " + s);
        messageHanlder.sendRegistrationToServer(s);
    }
}
