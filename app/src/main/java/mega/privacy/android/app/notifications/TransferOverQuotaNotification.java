package mega.privacy.android.app.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.CountDownTimer;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.components.transferWidget.TransfersManagement.*;
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

        boolean isLoggedIn = megaApi.isLoggedIn() != 0 && dbH.getCredentials() != null;
        boolean isFreeAccount = false;
        Intent intent;

        if (isLoggedIn) {
            isFreeAccount = MegaApplication.getInstance().getMyAccountInfo().getAccountType() == MegaAccountDetails.ACCOUNT_TYPE_FREE;
            intent = new Intent(app, ManagerActivityLollipop.class);
            intent.setAction(ACTION_SHOW_UPGRADE_ACCOUNT);
        } else {
            dbH.clearEphemeral();
            intent = new Intent(app, LoginActivityLollipop.class);
            intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(app, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        RemoteViews customView = new RemoteViews(app.getPackageName(), R.layout.notification_transfer_over_quota);
        customView.setTextViewText(R.id.content_text, app.getString(R.string.current_text_depleted_transfer_overquota, formatTimeDDHHMMSS(megaApi.getBandwidthOverquotaDelay())));
        String dismissButtonText = app.getString(isLoggedIn ? R.string.general_dismiss : R.string.login_text);
        customView.setTextViewText(R.id.dismiss_button, dismissButtonText);
        customView.setOnClickPendingIntent(R.id.dismiss_button, null);
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
                    .setColor(ContextCompat.getColor(app, R.color.mega))
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                    .setContent(customView)
                    .setAutoCancel(true);

            notificationManager.notify(NOTIFICATION_DOWNLOAD, builderCompat.build());
        } else {
            Notification.Builder builder = new Notification.Builder(app.getApplicationContext());

            builder.setSmallIcon(R.drawable.ic_stat_notify)
                    .setColor(ContextCompat.getColor(app, R.color.mega))
                    .setContent(customView)
                    .setAutoCancel(true);

            if (isAndroidNougatOrUpper()) {
                builder.setStyle(new Notification.DecoratedCustomViewStyle());
            }

            new CountDownTimer(megaApi.getBandwidthOverquotaDelay(), 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    if (isOnTransferOverQuota()) {
                        show();
                    } else {
                        notificationManager.cancel(NOTIFICATION_DOWNLOAD);
                    }
                }

                @Override
                public void onFinish() {

                }
            }.start();
            notificationManager.notify(NOTIFICATION_DOWNLOAD, builder.build());
        }
    }
}
