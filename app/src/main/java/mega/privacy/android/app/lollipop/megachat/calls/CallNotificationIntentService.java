package mega.privacy.android.app.lollipop.megachat.calls;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;

import mega.privacy.android.app.MegaApplication;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class CallNotificationIntentService extends IntentService implements MegaChatRequestListenerInterface {

    public static final String ANSWER = "ANSWER";
    public static final String IGNORE = "IGNORE";

    MegaChatApiAndroid megaChatApi;
    MegaApiAndroid megaApi;
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
        megaApi = app.getMegaApi();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        logDebug("onHandleIntent");

        chatHandleToAnswer = intent.getExtras().getLong(CHAT_ID_TO_ANSWER, -1);
        chatHandleInProgress = intent.getExtras().getLong(CHAT_ID_IN_PROGRESS, -1);

        clearIncomingCallNotification(chatHandleToAnswer);

        final String action = intent.getAction();
        logDebug("action: " + action);
        if (ANSWER.equals(action)) {
            megaChatApi.hangChatCall(chatHandleInProgress, this);

        } else if (IGNORE.equals(action)) {
            megaChatApi.setIgnoredCall(chatHandleToAnswer);
            app.getInstance().removeChatAudioManager();
            stopSelf();

        } else {
            throw new IllegalArgumentException("Unsupported action: " + action);
        }
    }

    public void clearIncomingCallNotification(long chatHandleToAnswer) {
        logDebug("chatHandleToAnswer: " + chatHandleToAnswer);

        try {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (megaChatApi == null) return;
            MegaChatCall call = megaChatApi.getChatCall(chatHandleToAnswer);
            if (call == null) return;

            long chatCallId = call.getId();
            String notificationCallId = MegaApiJava.userHandleToBase64(chatCallId);
            int notificationId = (notificationCallId).hashCode();
            notificationManager.cancel(notificationId);
        } catch (Exception e) {
            logError("EXCEPTION", e);
        }
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {
    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        if (request.getType() == MegaChatRequest.TYPE_HANG_CHAT_CALL) {
            logDebug("TYPE_HANG_CHAT_CALL");
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                logDebug("TYPE_HANG_CHAT_CALL:OK: ");
                MegaApplication.setSpeakerStatus(chatHandleToAnswer, false);
                megaChatApi.answerChatCall(chatHandleToAnswer, false, this);
            } else {
                logError("TYPE_HANG_CHAT_CALL:ERROR: " + e.getErrorCode());
            }
        } else if (request.getType() == MegaChatRequest.TYPE_ANSWER_CHAT_CALL) {
            logDebug("TYPE_ANSWER_CHAT_CALL");
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                logDebug("TYPE_ANSWER_CHAT_CALL:OK");
                MegaApplication.setShowPinScreen(false);
                Intent i = new Intent(this, ChatCallActivity.class);
                i.putExtra(CHAT_ID, chatHandleToAnswer);
                i.setAction(SECOND_CALL);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                this.startActivity(i);
                stopSelf();
            } else {
                logError("TYPE_ANSWER_CHAT_CALL:ERROR: " + e.getErrorCode());
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
    }

}