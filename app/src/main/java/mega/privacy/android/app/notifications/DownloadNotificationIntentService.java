package mega.privacy.android.app.notifications;


import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;

import androidx.annotation.Nullable;

import mega.privacy.android.app.main.LoginActivity;
import mega.privacy.android.app.main.ManagerActivity;

import static mega.privacy.android.app.main.ManagerActivity.PENDING_TAB;
import static mega.privacy.android.app.main.ManagerActivity.TRANSFERS_TAB;
import static mega.privacy.android.app.utils.Constants.*;

public class DownloadNotificationIntentService extends IntentService {

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public DownloadNotificationIntentService() {
        super("DownloadNotificationIntentService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_DOWNLOAD_FINAL);
        }

        if (intent != null && intent.getAction() != null) {
            Intent pendingIntent = null;

            if (intent.getAction().equals(ACTION_SHOW_UPGRADE_ACCOUNT)) {
                pendingIntent = new Intent(DownloadNotificationIntentService.this, ManagerActivity.class);
                pendingIntent.setAction(ACTION_SHOW_UPGRADE_ACCOUNT);
            } else if (intent.getAction().equals(ACTION_LOG_IN)){
                pendingIntent = new Intent(DownloadNotificationIntentService.this, LoginActivity.class);
                pendingIntent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
            } else if (intent.getAction().equals(ACTION_SHOW_TRANSFERS)) {
                pendingIntent = new Intent(DownloadNotificationIntentService.this, ManagerActivity.class);
                pendingIntent.setAction(ACTION_SHOW_TRANSFERS);
                pendingIntent.putExtra(TRANSFERS_TAB, PENDING_TAB);
            }

            if (pendingIntent != null) {
                pendingIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                pendingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                this.startActivity(pendingIntent);
            }
        }
    }
}
