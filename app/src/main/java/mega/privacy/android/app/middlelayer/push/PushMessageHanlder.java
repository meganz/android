package mega.privacy.android.app.middlelayer.push;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PowerManager;

import androidx.annotation.Nullable;

import java.util.Map;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.fcm.IncomingCallService;
import mega.privacy.android.app.fcm.KeepAliveService;
import mega.privacy.android.app.utils.TextUtil;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.LogUtil.*;

public class PushMessageHanlder implements MegaRequestListenerInterface, MegaChatRequestListenerInterface {

    private static final int AWAKE_CPU_FOR = 60 * 1000;
    private static final String TYPE_SHARE_FOLDER = "1";
    private static final String TYPE_CONTACT_REQUEST = "3";
    private static final String TYPE_ACCEPTANCE = "5";
    private static final String TYPE_CALL = "4";
    private static final String TYPE_CHAT = "2";
    public static final String PUSH_TOKEN = "PUSH_TOKEN";

    private MegaApplication app;

    private MegaApiAndroid megaApi;

    private MegaChatApiAndroid megaChatApi;

    private DatabaseHandler dbH;

    private boolean isLoggingIn;

    private boolean showMessageNotificationAfterPush;

    private boolean beep;

    private static String token;

    public PushMessageHanlder() {
        app = MegaApplication.getInstance();
        megaApi = app.getMegaApi();
        megaChatApi = app.getMegaChatApi();
        dbH = DatabaseHandler.getDbHandler(app);
    }

    /**
     * Awake CPU to make sure the following operations can finish.
     *
     * @param launchService Whether launch a foreground service to keep the app alive.
     */
    private void awakeCpu(boolean launchService) {
        PowerManager pm = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            logDebug("wake lock acquire");
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wake:push_message");
            wl.setReferenceCounted(false);
            wl.acquire(AWAKE_CPU_FOR);
        }
        if (launchService) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                app.startForegroundService(new Intent(app, KeepAliveService.class));
            } else {
                app.startService(new Intent(app, KeepAliveService.class));
            }
        }
    }

    public void handleMessage(Message message) {
        String messageType = message.getType();
        logDebug("Handle message from: " + message.getFrom() + " , which type is: " + messageType);
        logDebug("original priority is " + message.getOriginalPriority() + " ,priority is " + message.getPriority());

        // Check if message contains a data payload.
        if (message.hasData()) {
            logDebug("Message data payload: " + message.getData());
            UserCredentials credentials = dbH.getCredentials();
            if (credentials == null) {
                logError("No user credentials, process terminates!");
            } else {
                if (TYPE_SHARE_FOLDER.equals(messageType) || TYPE_CONTACT_REQUEST.equals(messageType) || TYPE_ACCEPTANCE.equals(messageType)) {
                    //Leave the flag showMessageNotificationAfterPush as it is
                    //If true - wait until connection finish
                    //If false, no need to change it
                    logDebug("Flag showMessageNotificationAfterPush: " + showMessageNotificationAfterPush);
                    String gSession = credentials.getSession();
                    if (megaApi.getRootNode() == null) {
                        logWarning("RootNode = null");
                        performLoginProccess(gSession);
                    } else {
                        logDebug("Awaiting info on listener");
                        retryPendingConnections();
                    }
                } else if (TYPE_CALL.equals(messageType)) {
                    //Leave the flag showMessageNotificationAfterPush as it is
                    //If true - wait until connection finish
                    //If false, no need to change it
                    logDebug("Flag showMessageNotificationAfterPush: " + showMessageNotificationAfterPush);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PowerManager pm = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
                        boolean isIdle = pm.isDeviceIdleMode();
                        if ((!app.isActivityVisible() && megaApi.getRootNode() == null) || isIdle) {
                            logDebug("Launch foreground service!");
                            awakeCpu(false);
                            app.startService(new Intent(app, IncomingCallService.class));
                            return;
                        }
                    }
                    String gSession = credentials.getSession();
                    if (megaApi.getRootNode() == null) {
                        logWarning("RootNode = null");
                        performLoginProccess(gSession);
                    } else {
                        logDebug("RootNode is NOT null - wait CALLDATA:onChatCallUpdate");
                        int ret = megaChatApi.getInitState();
                        logDebug("result of init ---> " + ret);
                        int status = megaChatApi.getOnlineStatus();
                        logDebug("online status ---> " + status);
                        int connectionState = megaChatApi.getConnectionState();
                        logDebug("connection state ---> " + connectionState);
                        retryPendingConnections();
                    }
                } else if (TYPE_CHAT.equals(messageType)) {
                    logDebug("CHAT notification");
                    if(app.isActivityVisible()){
                        logDebug("App on foreground --> return");
                        retryPendingConnections();
                        return;
                    }

                    beep = !Message.NO_BEEP.equals(message.getSilent());
                    awakeCpu(beep);

                    logDebug("Notification should beep: " + beep);
                    showMessageNotificationAfterPush = true;

                    String gSession = credentials.getSession();
                    if (megaApi.getRootNode() == null) {
                        logWarning("RootNode = null");
                        performLoginProccess(gSession);
                    } else {
                        //Leave the flag showMessageNotificationAfterPush as it is
                        //If true - wait until connection finish
                        //If false, no need to change it
                        logDebug("Flag showMessageNotificationAfterPush: " + showMessageNotificationAfterPush);
                        logDebug("Call to pushReceived");
                        megaChatApi.pushReceived(beep);
                        beep = false;
                    }
                }
            }
        }
    }

    /**
     * Send the push token to API to register the device.
     * Token will be cached, only when the new token is different,it will be sent.
     *
     * @param newToken Push token gotten from the platform.
     * @param deviceType DEVICE_ANDROID or DEVICE_HUAWEI
     */
    public void sendRegistrationToServer(String newToken, int deviceType) {
        if(TextUtil.isTextEmpty(newToken)) return;

        SharedPreferences sp = app.getSharedPreferences(PUSH_TOKEN, Context.MODE_PRIVATE);
        String token = sp.getString("token", "");
        if (!token.equals(newToken)) {
            if (megaApi == null) {
                megaApi = app.getMegaApi();
            }
            logDebug("Push service's new token: " + newToken);
            PushMessageHanlder.token = token;
            megaApi.registerPushNotifications(deviceType, newToken, this);
        } else {
            logDebug("No need to register new token.");
        }
    }

    public static String getToken() {
        return token;
    }

    /**
     * If the account hasn't logined in, login first.
     *
     * @param gSession Cached session, used to do a fast login.
     */
    private void performLoginProccess(String gSession) {
        isLoggingIn = MegaApplication.isLoggingIn();
        if (!isLoggingIn) {
            isLoggingIn = true;
            MegaApplication.setLoggingIn(isLoggingIn);

            if (megaChatApi == null) {
                megaChatApi = app.getMegaChatApi();
            }

            int ret = megaChatApi.getInitState();

            if (ret == MegaChatApi.INIT_NOT_DONE || ret == MegaChatApi.INIT_ERROR) {
                ret = megaChatApi.init(gSession);
                logDebug("result of init ---> " + ret);
                if (ret == MegaChatApi.INIT_NO_CACHE) {
                    logDebug("condition ret == MegaChatApi.INIT_NO_CACHE");

                } else if (ret == MegaChatApi.INIT_ERROR) {
                    logDebug("condition ret == MegaChatApi.INIT_ERROR");
                    megaChatApi.logout(this);

                } else {
                    logDebug("Chat correctly initialized");
                }
            }
            megaApi.fastLogin(gSession, this);
        }
    }

    private void retryPendingConnections() {
        logDebug("retryPendingConnections");
        try {
            if (megaApi != null) {
                megaApi.retryPendingConnections();
            }
            if (megaChatApi != null) {
                megaChatApi.retryPendingConnections(false, null);
            }
        } catch (Exception e) {
            logError("Exception", e);
        }
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        logDebug("onRequestStart: " + request.getRequestString());
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        logDebug("onRequestUpdate: " + request.getRequestString());
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        logDebug("onRequestFinish: " + request.getRequestString());
        if (request.getType() == MegaRequest.TYPE_LOGIN) {
            if (e.getErrorCode() == MegaError.API_OK) {
                logDebug("Fast login OK");
                logDebug("Calling fetchNodes from MegaFireBaseMessagingService");
                megaApi.fetchNodes(this);
            } else {
                logError("ERROR: " + e.getErrorString());
                isLoggingIn = false;
                MegaApplication.setLoggingIn(isLoggingIn);
            }
        } else if (request.getType() == MegaRequest.TYPE_FETCH_NODES) {
            isLoggingIn = false;
            MegaApplication.setLoggingIn(isLoggingIn);
            if (e.getErrorCode() == MegaError.API_OK) {
                logDebug("OK fetch nodes");
                logDebug("Chat --> connectInBackground");
                megaChatApi.connectInBackground(this);
            } else {
                logError("ERROR: " + e.getErrorString());
            }
        } else if (request.getType() == MegaRequest.TYPE_REGISTER_PUSH_NOTIFICATION) {
            if (e.getErrorCode() == MegaError.API_OK) {
                String token = request.getText();
                logDebug("Register push token successfully. Token is: " + token);
                // record the token locally when register successful.
                SharedPreferences sp = app.getSharedPreferences(PUSH_TOKEN, Context.MODE_PRIVATE);
                sp.edit().putString("token", token).apply();
            } else {
                logError("Register push token failed, retry. error code is: " + e.getErrorCode());
                // may need retry when error code isn't -15
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
        logWarning("onRequestTemporary: " + request.getRequestString());
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        logDebug("onRequestFinish: " + request.getRequestString() + " result: " + e.getErrorString());
        if (request.getType() == MegaChatRequest.TYPE_CONNECT) {
            logDebug("TYPE CONNECT");
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                logDebug("Connected to chat!");
                if (showMessageNotificationAfterPush) {
                    showMessageNotificationAfterPush = false;
                    logDebug("Call to pushReceived");
                    megaChatApi.pushReceived(beep);
                    beep = false;
                } else {
                    logDebug("Login do not started by CHAT message");
                }
            } else {
                logError("ERROR WHEN CONNECTING" + e.getErrorString());
            }
        } else if (request.getType() == MegaChatRequest.TYPE_SET_BACKGROUND_STATUS) {
            logDebug("TYPE SETBACKGROUNDSTATUS");
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }

    /**
     * Generic push message object, used to unify corresponding platform dependent purchase object.
     */
    public static class Message {

        private static final String KEY_SILENT = "silent";
        private static final String KEY_TYPE = "type";
        private static final String KEY_EMAIL = "email";

        private static final String NO_BEEP = "1";

        /**
         * Where is the message from. May be null. Just for log purpose.
         */
        private String from;

        /**
         * Store couples of info sent from server,
         * but the client only use: silent, type, email.
         */
        private Map<String, String> data;

        /**
         * Just for log and debug purpose.
         */
        private int originalPriority;

        /**
         * Just for log and debug purpose.
         */
        private int priority;

        public Message(String from, int originalPriority, int priority, Map<String, String> data) {
            this.from = from;
            this.originalPriority = originalPriority;
            this.priority = priority;
            this.data = data;
        }

        /**
         * Check if the data map has info.
         *
         * @return true, has; false, doesn't have.
         */
        public boolean hasData() {
            return data != null && data.size() > 0;
        }

        /**
         * Get the message type.
         *
         * @return Message type, or null if data map is empty.
         */
        @Nullable
        public String getType() {
            if (hasData()) {
                return data.get(KEY_TYPE);
            }
            logWarning("Message type is null!");
            return null;
        }

        /**
         * Get the email.
         *
         * @return Email, or null if data map is empty.
         */
        @Nullable
        public String getEmail() {
            if (hasData()) {
                return data.get(KEY_EMAIL);
            }
            logWarning("Message email is null!");
            return null;
        }

        /**
         * Get if the push message should be silent.
         *
         * @return Message type, or null if data map is empty.
         */
        @Nullable
        public String getSilent() {
            if (hasData()) {
                return data.get(KEY_SILENT);
            }
            logWarning("Message silent is null!");
            return null;
        }

        public String getFrom() {
            return from;
        }

        public int getOriginalPriority() {
            return originalPriority;
        }

        public int getPriority() {
            return priority;
        }

        public Map<String, String> getData() {
            return data;
        }

        @Override
        public String toString() {
            return "Message{" +
                    "from='" + from + '\'' +
                    ", data=" + data +
                    ", originalPriority=" + originalPriority +
                    ", priority=" + priority +
                    '}';
        }
    }
}
