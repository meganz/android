package mega.privacy.android.app.utils;

import static android.content.Context.NOTIFICATION_SERVICE;
import static mega.privacy.android.app.utils.Util.isAndroid10OrUpper;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationCompat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import mega.privacy.android.app.R;
import mega.privacy.android.app.presentation.chat.dialog.AskForDisplayOverActivity;

public class IncomingCallNotification {

    private static final int TO_SYSTEM_SETTING_ID = 13992;

    public static final String INCOMING_CALL_CHANNEL_ID = "incoming_call_channel_id";
    public static final String INCOMING_CALL_CHANNEL_NAME = "Incoming call";

    @TargetApi(Build.VERSION_CODES.Q)
    public static void toSystemSettingNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        createChannel(notificationManager);
        for(StatusBarNotification notification : notificationManager.getActiveNotifications()) {
            if(notification.getId() == TO_SYSTEM_SETTING_ID) {
                return;
            }
        }

        Intent intent = new Intent(context, AskForDisplayOverActivity.class);
        @NoMeaning int i = 0;
        PendingIntent pendingIntent = PendingIntent.getActivity(context, i, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, INCOMING_CALL_CHANNEL_ID);
        notificationBuilder
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(context.getString(R.string.notification_enable_display)))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        notificationManager.notify(TO_SYSTEM_SETTING_ID, notificationBuilder.build());
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private static void createChannel(NotificationManager notificationManager) {
        NotificationChannel channel = new NotificationChannel(INCOMING_CALL_CHANNEL_ID, INCOMING_CALL_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        channel.enableVibration(false);
        channel.setSound(null, null);
        notificationManager.createNotificationChannel(channel);
    }

    public static boolean shouldNotify(Context context) {
        return isAndroid10OrUpper() && !Settings.canDrawOverlays(context);
    }

    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.LOCAL_VARIABLE})
    private @interface NoMeaning {
    }
}
