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
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.RemoteMessage;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;

public class IncomingMessageService extends IncomingCallService {
    
    private ChatAdvancedNotificationBuilder chatNotificationBuilder;
    private Handler h;
    private RemoteMessage remoteMessage;
    
    @Override
    public int onStartCommand(Intent intent,int flags,int startId) {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        log("network available: " + (cm.getActiveNetworkInfo() != null));
        if (cm.getActiveNetworkInfo() != null) {
            log(cm.getActiveNetworkInfo().getState() + "");
            log(cm.getActiveNetworkInfo().getDetailedState() + "");
        }
        remoteMessage = intent.getParcelableExtra("remoteMessage");
        createNotification(R.drawable.ic_new_messages,getString(R.string.retrieving_message_title));
        checkMessage();
        return START_NOT_STICKY;
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
                log("Silent payload: " + silent);
                
                if (silent != null) {
                    if (silent.equals("1")) {
                        beep = false;
                    } else {
                        beep = true;
                    }
                } else {
                    log("NO DATA on the PUSH");
                    beep = true;
                }
            } catch (Exception e) {
                log("ERROR:remoteSilentParameter");
                beep = true;
            }
            
            log("notification should beep: " + beep);
            showMessageNotificationAfterPush = true;
            
            UserCredentials credentials = dbH.getCredentials();
            String gSession = credentials.getSession();
            if (megaApi.getRootNode() == null) {
                log("RootNode = null");
                performLoginProccess(gSession);
            } else {
                //Leave the flag showMessageNotificationAfterPush as it is
                //If true - wait until connection finish
                //If false, no need to change it
                log("Flag showMessageNotificationAfterPush: " + showMessageNotificationAfterPush);
                log("(2)Call to pushReceived");
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
                                log("Show simple notification - no connection finished");
                                chatNotificationBuilder.showSimpleNotification();
                            } else {
                                log("Notification already shown");
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
