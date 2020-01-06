package mega.privacy.android.app.utils;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.megachat.calls.ChatCallActivity;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatRoom;

import static mega.privacy.android.app.utils.Constants.CALL_ID;
import static mega.privacy.android.app.utils.Constants.CHAT_ID;

public class IncomingCallNotification {

    private static final int TO_SYSTEM_SETTING_ID = 13992;

    public static final int INCOMING_CALL_NOTI_ID = 13993;

    public static final String INCOMING_CALL_CHANNEL_ID = "incoming_call_channel_id";
    public static final String INCOMING_CALL_CHANNEL_NAME = "Incoming call";


    public static void toSystemSettingNotification(Context context) {
        if (shouldNotify(context)) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(INCOMING_CALL_CHANNEL_ID, INCOMING_CALL_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);

            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.getPackageName()));
            @NoMeaning int i = 0;
            PendingIntent pendingIntent = PendingIntent.getActivity(context, i, intent, PendingIntent.FLAG_ONE_SHOT);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, INCOMING_CALL_CHANNEL_ID);
            notificationBuilder
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText("Click to allow system to give priority to the call interface."))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);
            notificationManager.notify(TO_SYSTEM_SETTING_ID, notificationBuilder.build());
        }
    }

    @TargetApi(29)
    public static void toIncomingCall(Context context, MegaChatCall callToLaunch, MegaChatApiAndroid megaChatApi) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(INCOMING_CALL_CHANNEL_ID, INCOMING_CALL_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        channel.enableVibration(false);
        channel.setSound(null, null);
        notificationManager.createNotificationChannel(channel);

        Intent intent = new Intent(context, ChatCallActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(CHAT_ID, callToLaunch.getChatid());
        intent.putExtra(CALL_ID, callToLaunch.getId());
        @NoMeaning int i = 0;
        PendingIntent pendingIntent = PendingIntent.getActivity(context, i, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder mBuilderCompat = new NotificationCompat.Builder(context, INCOMING_CALL_CHANNEL_ID);
        mBuilderCompat
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentText("Incoming call")
                .setAutoCancel(false)
                .addAction(R.drawable.ic_phone_white, "Go to the call", pendingIntent)
                .setFullScreenIntent(pendingIntent, true)
                .setOngoing(true)
                .setColor(ContextCompat.getColor(context, R.color.mega))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL);

        MegaChatRoom chat = megaChatApi.getChatRoom(callToLaunch.getChatid());
        if (chat != null) {
            mBuilderCompat.setContentTitle(chat.getTitle());
        }

        notificationManager.notify(INCOMING_CALL_NOTI_ID, mBuilderCompat.build());
    }

    public static boolean shouldNotify(Context context) {
        return Build.VERSION.SDK_INT >= 29 && !Settings.canDrawOverlays(context);
    }

    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.LOCAL_VARIABLE})
    private @interface NoMeaning {
    }
}
