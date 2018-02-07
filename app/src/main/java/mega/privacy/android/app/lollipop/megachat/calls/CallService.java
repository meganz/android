package mega.privacy.android.app.lollipop.megachat.calls;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatCallListenerInterface;

public class CallService extends Service implements MegaChatCallListenerInterface {

    MegaApplication app;
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    long chatId;

    private Notification.Builder mBuilder;
    private NotificationCompat.Builder mBuilderCompat;
    private NotificationManager mNotificationManager;

    public void onCreate() {
        super.onCreate();
        log("onCreate");

        app = (MegaApplication) getApplication();
        megaApi = app.getMegaApi();
        megaChatApi = app.getMegaChatApi();

        megaChatApi.addChatCallListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            mBuilder = new Notification.Builder(this);
        mBuilderCompat = new NotificationCompat.Builder(this);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log("onStartCommand");


        if(intent == null){
            stopSelf();
        }

        Bundle extras = intent.getExtras();
        if (extras != null) {
            chatId = extras.getLong("chatHandle", -1);
            log("Chat handle to call: " + chatId);
        }

        showCallInProgressNotification();
        return START_NOT_STICKY;
    }

    @Override
    public void onChatCallUpdate(MegaChatApiJava api, MegaChatCall call) {

        if(call.getStatus()==MegaChatCall.CALL_STATUS_DESTROYED){
            stopForeground(true);
            mNotificationManager.cancel(Constants.NOTIFICATION_CALL_IN_PROGRESS);
            stopSelf();
        }
    }

    public void showCallInProgressNotification(){
        log("showCallInProgressNotification");

        mBuilderCompat = new NotificationCompat.Builder(this);

        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(this, ChatCallActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(Long.toString(System.currentTimeMillis()));
        intent.putExtra("chatHandle", chatId);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 , intent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilderCompat
                .setSmallIcon(R.drawable.ic_stat_notify_download)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .setColor(ContextCompat.getColor(this,R.color.mega))
                .setContentTitle("Call in progress").setContentText("Click to back to call")
                .setOngoing(false);

        Notification notif = mBuilderCompat.build();
//        mNotificationManager.notify(Constants.NOTIFICATION_CALL_IN_PROGRESS, notif);

        startForeground(Constants.NOTIFICATION_CALL_IN_PROGRESS, notif);
    }

    @Override
    public void onDestroy() {
        log("onDestroy");

        if (megaChatApi != null) {
            megaChatApi.removeChatCallListener(this);
        }

        mNotificationManager.cancel(Constants.NOTIFICATION_CALL_IN_PROGRESS);

        super.onDestroy();
    }

    public static void log(String log) {
        Util.log("CallService", log);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
