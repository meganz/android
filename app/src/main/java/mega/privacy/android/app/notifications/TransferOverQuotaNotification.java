package mega.privacy.android.app.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.*;

public class TransferOverQuotaNotification {

    private NotificationManager notificationManager;

    public TransferOverQuotaNotification(NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
    }

    public void show() {
        MegaApplication app = MegaApplication.getInstance();
        if (app == null) {
            logWarning("MegaApplication is null");
            return;
        }

        MegaApiAndroid megaApi = app.getMegaApi();
        DatabaseHandler dbH = app.getDbH();

        NotificationCompat.Builder builderCompat;

        if (isAndroidOreoOrUpper()) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_DOWNLOAD_ID, NOTIFICATION_CHANNEL_DOWNLOAD_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setShowBadge(true);
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);

            builderCompat  = new NotificationCompat.Builder(app.getApplicationContext(), NOTIFICATION_CHANNEL_DOWNLOAD_ID);
        }
        else {
            builderCompat = new NotificationCompat.Builder(app.getApplicationContext());
        }

        Intent intent;
        PendingIntent pendingIntent;

        if (megaApi.isLoggedIn() == 0 || dbH.getCredentials() == null) {
            dbH.clearEphemeral();
            intent = new Intent(app, LoginActivityLollipop.class);
            intent.setAction(ACTION_OVERQUOTA_TRANSFER);
            pendingIntent = PendingIntent.getActivity(app, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            intent = new Intent(app, ManagerActivityLollipop.class);
            intent.setAction(ACTION_OVERQUOTA_TRANSFER);
            pendingIntent = PendingIntent.getActivity(app, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        NotificationCompat.Action actionDismiss = new NotificationCompat.Action.Builder(0, app.getString(R.string.general_dismiss).toUpperCase(), pendingIntent).build();
        NotificationCompat.Action actionUpgrade = new NotificationCompat.Action.Builder(0, app.getString(R.string.my_account_upgrade_pro).toUpperCase(), pendingIntent).build();

        builderCompat
                .setSmallIcon(R.mipmap.ic_launcher_mega)
                .setLargeIcon(BitmapFactory.decodeResource(app.getResources(), R.mipmap.ic_launcher_mega))
                .setTicker(app.getString(R.string.title_transfer_over_quota))
                .setContentTitle(app.getString(R.string.title_transfer_over_quota))
                .setContentText(app.getString(R.string.current_text_depleted_transfer_overquota, formatTimeDDHHMMSS(megaApi.getBandwidthOverquotaDelay())))
                .addAction(actionDismiss)
                .addAction(actionUpgrade)
                .setAutoCancel(true);

        notificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, builderCompat.build());
    }
}
