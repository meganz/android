package mega.privacy.android.app.fcm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import mega.privacy.android.app.utils.ChatUtil;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logError;
import static mega.privacy.android.app.utils.LogUtil.logWarning;

public class IncomingCallService extends Service implements MegaRequestListenerInterface {

    public static final String NOTIFICATION_CHANNEL_ID = "10099";
    public static final int notificationId = 1086;
    private static final int STOP_SELF_AFTER = 60 * 1000;
    MegaApplication app;
    MegaApiAndroid megaApi;
    MegaApiAndroid megaApiFolder;
    DatabaseHandler dbH;
    MegaChatApiAndroid megaChatApi;
    ChatSettings chatSettings;
    boolean showMessageNotificationAfterPush = false;
    boolean beep = false;
    WifiManager.WifiLock lock;
    PowerManager.WakeLock wl;

    /**
     * Flag for controlling if allows the app to do login in background upon receiving a push message.
     */
    public static volatile boolean allowBackgroundLogin = true;

    @Override
    public void onCreate() {
        super.onCreate();
        logDebug("onCreateFCM");

        app = (MegaApplication) getApplication();
        megaApi = app.getMegaApi();
        megaApiFolder = app.getMegaApiFolder();
        megaChatApi = app.getMegaChatApi();

        dbH = DatabaseHandler.getDbHandler(getApplicationContext());

        showMessageNotificationAfterPush = false;
        beep = false;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                stop();
            }
        }, STOP_SELF_AFTER);
    }

    @Override
    public void onDestroy() {
        logDebug("Incoming call foreground service");
        super.onDestroy();
        if (wl != null) {
            logDebug("Wifi lock release");
            wl.release();
        }
        if (lock != null) {
            logDebug("Wake lock release");
            lock.release();
        }
        stop();
    }

    protected void stop() {
        stopForeground(true);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(notificationId);
        }
        stopSelf();
    }

    public void createNotification() {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        mBuilder.setSmallIcon(R.drawable.ic_call_started).setAutoCancel(true);
        NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "NOTIFICATION_CHANNEL_NAME", importance);
            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(notificationChannel);
            }
        }
        if (mNotificationManager != null) {
            Notification notification = mBuilder.build();
            startForeground(notificationId, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        logDebug("Network available: " + (cm.getActiveNetworkInfo() != null));
        if (cm.getActiveNetworkInfo() != null) {
            logDebug(cm.getActiveNetworkInfo().getState() + "");
            logDebug(cm.getActiveNetworkInfo().getDetailedState() + "");
        }
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        lock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "MegaIncomingCallWifiLock");
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, ":MegaIncomingCallPowerLock");
        if (!wl.isHeld()) {
            wl.acquire();
        }
        if (!lock.isHeld()) {
            lock.acquire();
        }

        createNotification();
        logDebug("CALL notification");
        //Leave the flag showMessageNotificationAfterPush as it is
        //If true - wait until connection finish
        //If false, no need to change it
        logDebug("Flag showMessageNotificationAfterPush: " + showMessageNotificationAfterPush);
        UserCredentials credentials = dbH.getCredentials();
        if (credentials == null) {
            logWarning("There are not user credentials");
        } else {
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
            }
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void performLoginProccess(String gSession) {
        if (!MegaApplication.isLoggingIn()) {
            /*
                Two locks and synchronized block prevent background login executes after login process is launched in `LoginFragment`.
                Otherwise the login process in foreground will failed with `-11` and cause logout.
             */
            if (allowBackgroundLogin) {
                synchronized (MegaApplication.getInstance()) {
                    if (allowBackgroundLogin) {
                        megaApi.fastLogin(gSession, this);
                    }
                }
            }

            ChatUtil.initMegaChatApi(gSession);
            chatSettings = dbH.getChatSettings();
        }
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        logDebug("onRequestStart: " + request.getRequestString());
        allowBackgroundLogin = false;
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        logDebug("onRequestUpdate: " + request.getRequestString());
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        logDebug("onRequestFinish: " + request.getRequestString());

        if (request.getType() == MegaRequest.TYPE_LOGIN) {
            allowBackgroundLogin = true;

            if (e.getErrorCode() == MegaError.API_OK) {
                logDebug("Fast login OK");
                logDebug("Logged in. Setting account auth token for folder links.");
                megaApiFolder.setAccountAuth(megaApi.getAccountAuth());
                logDebug("Calling fetchNodes from MegaFireBaseMessagingService");
                megaApi.fetchNodes(this);

                // Get cookies settings after login.
                MegaApplication.getInstance().checkEnabledCookies();
            } else {
                logError("ERROR: " + e.getErrorString());
            }
        } else if (request.getType() == MegaRequest.TYPE_FETCH_NODES) {
            if(e.getErrorCode() == MegaError.API_OK) {
                if (showMessageNotificationAfterPush) {
                    showMessageNotificationAfterPush = false;
                    megaChatApi.pushReceived(beep);
                    beep = false;
                } else {
                    logDebug("Login do not started by CHAT message");
                }
            } else {
                logDebug(request.getRequestString() + " failed. Error code: " + e.getErrorCode() + ", error string: " + e.getErrorString());
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
        logWarning("onRequestTemporary: " + request.getRequestString());
    }
}
