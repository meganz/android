package mega.privacy.android.app.service.push;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.concurrent.Executors;

import mega.privacy.android.app.middlelayer.push.PushMessageHanlder;

import static mega.privacy.android.app.utils.Constants.DEVICE_ANDROID;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logWarning;

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

    /**
     * Convert Google RemoteMessage object into generic Message object.
     *
     * @param remoteMessage Google RemoteMessage.
     * @return Generic Message object.
     */
    private PushMessageHanlder.Message convert(RemoteMessage remoteMessage) {
        return new PushMessageHanlder.Message(
                remoteMessage.getFrom(),
                remoteMessage.getOriginalPriority(),
                remoteMessage.getPriority(),
                remoteMessage.getData());
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        PushMessageHanlder.Message message = convert(remoteMessage);
        logDebug("Receive remote msg: " + message);
        messageHanlder.handleMessage(message);
    }

    @Override
    public void onNewToken(@NonNull String s) {
        logDebug("New token is: " + s);
        messageHanlder.sendRegistrationToServer(s, DEVICE_ANDROID);
    }

    /**
     * Request push service token, then register it in API as an identifier of the device.
     *
     * @param context Context.
     */
    public static void getToken(Context context) {
        //project number from google-service.json
        Executors.newFixedThreadPool(1).submit(() -> {
            FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    logWarning("Get token failed.");
                    return;
                }

                // Get new Instance ID token
                String token = task.getResult().getToken();
                logDebug("Get token: " + token);
                new PushMessageHanlder().sendRegistrationToServer(token, DEVICE_ANDROID);
            });
        });
    }
}