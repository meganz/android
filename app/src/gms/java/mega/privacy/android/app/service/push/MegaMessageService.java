package mega.privacy.android.app.service.push;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.util.concurrent.Executors;

import mega.privacy.android.app.R;
import mega.privacy.android.app.middlelayer.push.PushMessageHanlder;

import static mega.privacy.android.app.utils.LogUtil.*;

public class MegaMessageService extends FirebaseMessagingService {

    private static PushMessageHanlder messageHanlder;

    @Override
    public void onCreate() {
        super.onCreate();
        logDebug("onCreateFCM");
        synchronized (new Object()) {
            if (messageHanlder == null) {
                logDebug("Create new push message handler.");
                messageHanlder = new PushMessageHanlder();
            }
        }
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
        logDebug("New token is: " + s);
        messageHanlder.sendRegistrationToServer(s);
    }

    public static void getToken(Context context) {
        //project number from google-service.json
        final String id = context.getString(R.string.gcm_defaultSenderId);
        Executors.newFixedThreadPool(1).submit(() -> {
            FirebaseInstanceId instanceId = FirebaseInstanceId.getInstance();
            try {
                String token = instanceId.getToken(id, "FCM");
                messageHanlder.sendRegistrationToServer(token);
            } catch (IOException e) {
                e.printStackTrace();
                logError(e.getMessage(), e);
            }
        });
    }
}