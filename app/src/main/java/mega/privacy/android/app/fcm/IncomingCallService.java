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
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import mega.privacy.android.app.utils.Util;
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

public class IncomingCallService extends Service implements MegaRequestListenerInterface, MegaChatRequestListenerInterface {

    public static final String NOTIFICATION_CHANNEL_ID = "10099";
    public static final int notificationId = 1086;
    private static final int STOP_SELF_AFTER = 60 * 1000;
    MegaApplication app;
    MegaApiAndroid megaApi;
    DatabaseHandler dbH;
    MegaChatApiAndroid megaChatApi;
    ChatSettings chatSettings;
    boolean isLoggingIn = false;
    boolean showMessageNotificationAfterPush = false;
    boolean beep = false;
    WifiManager.WifiLock lock;
    PowerManager.WakeLock wl;

    protected static void log(String message) {
        Util.log("IncomingCallService", message);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        log("onCreateFCM");

        app = (MegaApplication) getApplication();
        megaApi = app.getMegaApi();
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
        log("onDestroy incoming call foreground service");
        super.onDestroy();
        if (wl != null) {
            log("wifi lock release");
            wl.release();
        }
        if (lock != null) {
            log("wake lock release");
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
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(R.drawable.ic_call_started).setAutoCancel(true);
        NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "NOTIFICATION_CHANNEL_NAME", importance);
            mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID);
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
        log("network available: " + (cm.getActiveNetworkInfo() != null));
        if (cm.getActiveNetworkInfo() != null) {
            log(cm.getActiveNetworkInfo().getState() + "");
            log(cm.getActiveNetworkInfo().getDetailedState() + "");
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
        log("CALL notification");
        //Leave the flag showMessageNotificationAfterPush as it is
        //If true - wait until connection finish
        //If false, no need to change it
        log("Flag showMessageNotificationAfterPush: " + showMessageNotificationAfterPush);
        UserCredentials credentials = dbH.getCredentials();
        if (credentials == null) {
            log("There are not user credentials");
        } else {
            String gSession = credentials.getSession();
            if (megaApi.getRootNode() == null) {
                log("RootNode = null");
                performLoginProccess(gSession);
            } else {
                log("RootNode is NOT null - wait CALLDATA:onChatCallUpdate");
                int ret = megaChatApi.getInitState();
                log("result of init ---> " + ret);
                int status = megaChatApi.getOnlineStatus();
                log("online status ---> " + status);
                int connectionState = megaChatApi.getConnectionState();
                log("connection state ---> " + connectionState);
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
        isLoggingIn = MegaApplication.isLoggingIn();
        if (!isLoggingIn) {
            isLoggingIn = true;
            MegaApplication.setLoggingIn(isLoggingIn);

            if (Util.isChatEnabled()) {
                if (megaChatApi == null) {
                    megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
                }

                int ret = megaChatApi.getInitState();

                if (ret == MegaChatApi.INIT_NOT_DONE || ret == MegaChatApi.INIT_ERROR) {
                    ret = megaChatApi.init(gSession);
                    log("result of init ---> " + ret);
                    chatSettings = dbH.getChatSettings();
                    if (ret == MegaChatApi.INIT_NO_CACHE) {
                        log("condition ret == MegaChatApi.INIT_NO_CACHE");

                    } else if (ret == MegaChatApi.INIT_ERROR) {
                        log("condition ret == MegaChatApi.INIT_ERROR");
                        if (chatSettings == null) {
                            log("ERROR----> Switch OFF chat");
                            chatSettings = new ChatSettings();
                            chatSettings.setEnabled(false + "");
                            dbH.setChatSettings(chatSettings);
                        } else {
                            log("ERROR----> Switch OFF chat");
                            dbH.setEnabledChat(false + "");
                        }
                        megaChatApi.logout(this);
                    } else {
                        log("Chat correctly initialized");
                    }
                }
            }

            megaApi.fastLogin(gSession, this);
        }
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        log("onRequestStart: " + request.getRequestString());
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        log("onRequestUpdate: " + request.getRequestString());
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        log("onRequestFinish: " + request.getRequestString());

        if (request.getType() == MegaRequest.TYPE_LOGIN) {
            if (e.getErrorCode() == MegaError.API_OK) {
                log("Fast login OK");
                log("Calling fetchNodes from MegaFireBaseMessagingService");
                megaApi.fetchNodes(this);
            } else {
                log("ERROR: " + e.getErrorString());
                isLoggingIn = false;
                MegaApplication.setLoggingIn(isLoggingIn);
                return;
            }
        } else if (request.getType() == MegaRequest.TYPE_FETCH_NODES) {
            isLoggingIn = false;
            MegaApplication.setLoggingIn(isLoggingIn);
            if (e.getErrorCode() == MegaError.API_OK) {
                log("OK fetch nodes");
                if (Util.isChatEnabled()) {
                    log("Chat enabled-->connectInBackground");
//                    MegaApplication.isFireBaseConnection=true;
                    megaChatApi.connectInBackground(this);
                } else {
                    log("Chat NOT enabled - sendNotification");
                }
            } else {
                log("ERROR: " + e.getErrorString());
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
        log("onRequestTemporary: " + request.getRequestString());
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        log("onRequestFinish: " + request.getRequestString() + " result: " + e.getErrorString());

        if (request.getType() == MegaChatRequest.TYPE_CONNECT) {
//            MegaApplication.isFireBaseConnection=false;
            log("TYPE CONNECT");
            //megaChatApi.setBackgroundStatus(true, this);
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                log("Connected to chat!");
                if (showMessageNotificationAfterPush) {
                    showMessageNotificationAfterPush = false;
                    megaChatApi.pushReceived(beep);
                    beep = false;
                } else {
                    log("Login do not started by CHAT message");
                }
            } else {
                log("EEEERRRRROR WHEN CONNECTING" + e.getErrorString());
            }
        } else if (request.getType() == MegaChatRequest.TYPE_SET_BACKGROUND_STATUS) {
            log("TYPE SETBACKGROUNDSTATUS");
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }
}
