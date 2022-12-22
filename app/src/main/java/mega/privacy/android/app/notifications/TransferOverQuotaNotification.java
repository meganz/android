package mega.privacy.android.app.notifications;

import static android.content.Context.NOTIFICATION_SERVICE;
import static mega.privacy.android.app.utils.Constants.ACTION_LOG_IN;
import static mega.privacy.android.app.utils.Constants.ACTION_SHOW_TRANSFERS;
import static mega.privacy.android.app.utils.Constants.ACTION_SHOW_UPGRADE_ACCOUNT;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_DOWNLOAD_FINAL;
import static mega.privacy.android.app.utils.TimeUtils.getHumanizedTime;
import static mega.privacy.android.app.utils.Util.isAndroidOreoOrUpper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.globalmanagement.TransfersManagement;
import mega.privacy.android.data.database.DatabaseHandler;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaApiAndroid;

public class TransferOverQuotaNotification {

    private MegaApplication app;
    private NotificationManager notificationManager;

    public TransferOverQuotaNotification(TransfersManagement transfersManagement) {
        app = MegaApplication.getInstance();
        this.notificationManager = (NotificationManager) app.getSystemService(NOTIFICATION_SERVICE);

        transfersManagement.setTransferOverQuotaBannerShown(true);
    }

    public void show() {
        MegaApiAndroid megaApi = app.getMegaApi();
        DatabaseHandler dbH = app.getDbH();

        boolean isLoggedIn = megaApi.isLoggedIn() != 0 && dbH.getCredentials() != null;
        boolean isFreeAccount = false;
        Intent intent = new Intent(app.getApplicationContext(), DownloadNotificationIntentService.class);

        if (isLoggedIn) {
            isFreeAccount = app.getMyAccountInfo().getAccountType() == MegaAccountDetails.ACCOUNT_TYPE_FREE;
            intent.setAction(ACTION_SHOW_UPGRADE_ACCOUNT);
        } else {
            dbH.clearEphemeral();
            intent.setAction(ACTION_LOG_IN);
        }

        PendingIntent pendingIntent = PendingIntent.getService(app.getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent dismissIntent = PendingIntent.getService(app.getApplicationContext(), 0, new Intent(app.getApplicationContext(), DownloadNotificationIntentService.class), PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent clickIntent = new Intent(app.getApplicationContext(), DownloadNotificationIntentService.class);
        clickIntent.setAction(ACTION_SHOW_TRANSFERS);
        PendingIntent clickPendingIntent = PendingIntent.getService(app.getApplicationContext(), 0, clickIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        RemoteViews customView = new RemoteViews(app.getPackageName(), R.layout.notification_transfer_over_quota);
        customView.setTextViewText(R.id.content_text, app.getString(R.string.current_text_depleted_transfer_overquota, getHumanizedTime(megaApi.getBandwidthOverquotaDelay())));
        String dismissButtonText = app.getString(isLoggedIn ? R.string.general_dismiss : R.string.login_text);
        customView.setTextViewText(R.id.dismiss_button, dismissButtonText);
        customView.setOnClickPendingIntent(R.id.dismiss_button, dismissIntent);
        String upgradeButtonText = app.getString(!isLoggedIn ? R.string.continue_without_account_transfer_overquota : (isFreeAccount ? R.string.my_account_upgrade_pro : R.string.plans_depleted_transfer_overquota));
        customView.setTextViewText(R.id.upgrade_button, upgradeButtonText);
        customView.setOnClickPendingIntent(R.id.upgrade_button, pendingIntent);

        if (isAndroidOreoOrUpper()) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_DOWNLOAD_ID, NOTIFICATION_CHANNEL_DOWNLOAD_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setShowBadge(true);
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);

            NotificationCompat.Builder builderCompat = new NotificationCompat.Builder(app.getApplicationContext(), NOTIFICATION_CHANNEL_DOWNLOAD_ID);

            builderCompat.setSmallIcon(R.drawable.ic_stat_notify)
                    .setColor(ContextCompat.getColor(app, R.color.red_600_red_300))
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                    .setContent(customView)
                    .setContentIntent(clickPendingIntent)
                    .setOngoing(false)
                    .setAutoCancel(true);

            notificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, builderCompat.build());
        } else {
            Notification.Builder builder = new Notification.Builder(app.getApplicationContext());

            builder.setSmallIcon(R.drawable.ic_stat_notify)
                    .setColor(ContextCompat.getColor(app, R.color.red_600_red_300))
                    .setContent(customView)
                    .setContentIntent(clickPendingIntent)
                    .setOngoing(false)
                    .setAutoCancel(true);

            builder.setStyle(new Notification.DecoratedCustomViewStyle());

            notificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, builder.build());
        }
    }
}
