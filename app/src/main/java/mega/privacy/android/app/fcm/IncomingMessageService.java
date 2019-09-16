package mega.privacy.android.app.fcm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.RemoteMessage;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.LogUtil;
import mega.privacy.android.app.utils.Util;

public class IncomingMessageService extends IncomingCallService {
    
    private ChatAdvancedNotificationBuilder chatNotificationBuilder;
    private Handler h;
    private RemoteMessage remoteMessage;
    
    @Override
    public int onStartCommand(Intent intent,int flags,int startId) {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        LogUtil.logDebug("Network available: " + (cm.getActiveNetworkInfo() != null));
        if (cm.getActiveNetworkInfo() != null) {
            LogUtil.logDebug(cm.getActiveNetworkInfo().getState() + "");
            LogUtil.logDebug(cm.getActiveNetworkInfo().getDetailedState() + "");
        }
        
        PowerManager pm = (PowerManager)getApplicationContext().getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,":MegaIncomingMessagePowerLock");
        if (!wl.isHeld()) {
            wl.acquire();
        }
        
        remoteMessage = intent.getParcelableExtra("remoteMessage");
        createNotification();
        checkMessage();
        return START_NOT_STICKY;
    }
    
    public void createNotification() {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(R.drawable.ic_new_messages)
                .setContentText(getString(R.string.retrieving_message_title))
                .setAutoCancel(false);
        NotificationManager mNotificationManager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,Constants.NOTIFICATION_CHANNEL_FCM_FETCHING_MESSAGE,importance);
            mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID);
            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(notificationChannel);
            }
        }
        if (mNotificationManager != null) {
            Notification notification = mBuilder.build();
            startForeground(notificationId,notification);
        }
    }
    
    private void checkMessage() {
        if (Util.isChatEnabled()) {
            
            try {
                String silent;
                if (remoteMessage != null) {
                    silent = remoteMessage.getData().get("silent");
                } else {
                    silent = "0"; //beep
                }
                LogUtil.logDebug("Silent payload: " + silent);
                
                if (silent != null) {
                    if (silent.equals("1")) {
                        beep = false;
                    } else {
                        beep = true;
                    }
                } else {
                    LogUtil.logWarning("NO DATA on the PUSH");
                    beep = true;
                }
            } catch (Exception e) {
                LogUtil.logError("ERROR:remoteSilentParameter", e);
                beep = true;
            }

            LogUtil.logDebug("Notification should beep: " + beep);
            showMessageNotificationAfterPush = true;
            
            UserCredentials credentials = dbH.getCredentials();
            String gSession = credentials.getSession();
            if (megaApi.getRootNode() == null) {
                LogUtil.logWarning("RootNode = null");
                performLoginProccess(gSession);
            } else {
                //Leave the flag showMessageNotificationAfterPush as it is
                //If true - wait until connection finish
                //If false, no need to change it
                LogUtil.logDebug("Flag showMessageNotificationAfterPush: " + showMessageNotificationAfterPush);
                LogUtil.logDebug("Call to pushReceived");
                megaChatApi.pushReceived(beep);
                beep = false;
            }
            chatNotificationBuilder = ChatAdvancedNotificationBuilder.newInstance(this,megaApi,megaChatApi);
            
            h = new Handler(Looper.getMainLooper());
            h.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            stop();
                            boolean shown = ((MegaApplication)getApplication()).isChatNotificationReceived();
                            if (!shown) {
                                LogUtil.logDebug("Show simple notification - no connection finished");
                                chatNotificationBuilder.showSimpleNotification();
                            } else {
                                LogUtil.logDebug("Notification already shown");
                            }
                        }
                    },
                    12000
            );
        } else {
            stop();
        }
    }
}
