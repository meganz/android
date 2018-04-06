package mega.privacy.android.app.lollipop.megachat.calls;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;

public class CallNotificationIntentService extends IntentService implements MegaChatRequestListenerInterface {

    public static final String ANSWER = "ANSWER";
    public static final String IGNORE = "IGNORE";

    MegaChatApiAndroid megaChatApi;
    MegaApplication app;

    long chatHandleToAnswer;
    long chatHandleInProgress;

    public CallNotificationIntentService() {
        super("CallNotificationIntentService");
    }

    public void onCreate() {
        super.onCreate();

        app = (MegaApplication) getApplication();
        megaChatApi = app.getMegaChatApi();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        log("onHandleIntent");

        clearNotification();

        final String action = intent.getAction();
        if (ANSWER.equals(action)) {
            chatHandleToAnswer = intent.getExtras().getLong("chatHandleToAnswer", -1);
            chatHandleInProgress = intent.getExtras().getLong("chatHandleInProgress", -1);
            log("Hang in progress call: "+chatHandleInProgress);
            megaChatApi.hangChatCall(chatHandleInProgress, this);
        } else if (IGNORE.equals(action)) {
            log("onHandleIntent:IGNORE");
            stopSelf();
        } else {
            throw new IllegalArgumentException("Unsupported action: " + action);
        }
    }

    public static void log(String log) {
        Util.log("CallNotificationIntentService", log);
    }

    public void clearNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(Constants.NOTIFICATION_INCOMING_CALL);
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {
    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

        if(request.getType() == MegaChatRequest.TYPE_HANG_CHAT_CALL){
            log("onRequestFinish:TYPE_HANG_CHAT_CALL");
            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                megaChatApi.answerChatCall(chatHandleToAnswer, false, this);
            }
            else{
                log("onRequestFinish: ERROR:HANG_CALL: "+e.getErrorCode());
            }

        }
        else if(request.getType() == MegaChatRequest.TYPE_ANSWER_CHAT_CALL){
            log("onRequestFinish:TYPE_ANSWER_CHAT_CALL");
            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                MegaApplication.setShowPinScreen(false);

                Intent i = new Intent(this, ChatCallActivity.class);
                i.putExtra("chatHandle", chatHandleToAnswer);
                i.setAction("SECOND_CALL");
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                this.startActivity(i);
                stopSelf();
            }
            else{
                log("onRequestFinish: ERROR:ANSWER_CALL: "+e.getErrorCode());
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
    }

}