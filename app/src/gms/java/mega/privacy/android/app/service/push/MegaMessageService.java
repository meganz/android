package mega.privacy.android.app.service.push;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import mega.privacy.android.app.middlelayer.push.PushMessageHanlder;

import static mega.privacy.android.app.utils.LogUtil.logDebug;

public class MegaMessageService extends FirebaseMessagingService {

    private PushMessageHanlder messageHanlder;

    @Override
    public void onCreate() {
        super.onCreate();
        logDebug("onCreateFCM");
        messageHanlder = new PushMessageHanlder();
    }

    @Override
    public void onDestroy() {
        logDebug("onDestroyFCM");
        super.onDestroy();
    }

    private PushMessageHanlder.Message convert(RemoteMessage remoteMessage) {
        return new PushMessageHanlder.Message(
                remoteMessage.getFrom(),
                remoteMessage.getOriginalPriority(),
                remoteMessage.getPriority(),
                remoteMessage.getData());
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        messageHanlder.handleMessage(convert(remoteMessage));
    }

    @Override
    public void onNewToken(@NonNull String s) {
        messageHanlder.sendRegistrationToServer(s);
    }
}