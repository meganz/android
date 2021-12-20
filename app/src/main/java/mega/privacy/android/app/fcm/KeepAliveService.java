package mega.privacy.android.app.fcm;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Constants;

import static mega.privacy.android.app.utils.Constants.ACTION_CHAT_NOTIFICATION_MESSAGE;
import static mega.privacy.android.app.utils.Constants.EXTRA_MOVE_TO_CHAT_SECTION;

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
        Intent intent = new Intent(this, ManagerActivityLollipop.class);
        intent.setAction(ACTION_CHAT_NOTIFICATION_MESSAGE)
                .putExtra(EXTRA_MOVE_TO_CHAT_SECTION, true)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, RETRIEVING_MSG_CHANNEL_ID);
        mBuilder.setSmallIcon(smallIcon)
                .setContentIntent(pendingIntent)
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
                NotificationChannel channel = mNotificationManager.getNotificationChannel(RETRIEVING_MSG_CHANNEL_ID);
                if (channel == null) {
                    mNotificationManager.createNotificationChannel(notificationChannel);
                }
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
