package mega.privacy.android.app.fcm;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Constants;

public class KeepAliveService extends Service {

    public static final String RETRIEVING_MSG_CHANNEL_ID = "Retrieving message";

    public static final int NEW_MESSAGE_NOTIFICATION_ID = 1086;

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
    }

    protected void stop() {
        stopForeground(true);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if(manager != null) {
            manager.cancel(NEW_MESSAGE_NOTIFICATION_ID);
        }
        stopSelf();
    }

    public void createNotification(int smallIcon,String title) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, RETRIEVING_MSG_CHANNEL_ID);
        mBuilder.setSmallIcon(smallIcon)
                .setContentText(title)
                .setAutoCancel(false);
        NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel notificationChannel = new NotificationChannel(RETRIEVING_MSG_CHANNEL_ID, Constants.NOTIFICATION_CHANNEL_FCM_FETCHING_MESSAGE, importance);
            //no sound and vibration for this channel.
            notificationChannel.enableVibration(false);
            notificationChannel.setSound(null, null);
            if (mNotificationManager != null) {
                NotificationChannel oldChannel = mNotificationManager.getNotificationChannel(RETRIEVING_MSG_CHANNEL_ID);
                if (oldChannel != null) {
                    mNotificationManager.deleteNotificationChannel(RETRIEVING_MSG_CHANNEL_ID);
                }
                mNotificationManager.createNotificationChannel(notificationChannel);
            }
        }
        if (mNotificationManager != null) {
            Notification notification = mBuilder.build();
            startForeground(NEW_MESSAGE_NOTIFICATION_ID, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent,int flags,int startId) {
        createNotification(R.drawable.ic_stat_notify,getString(R.string.notification_chat_undefined_content));
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
