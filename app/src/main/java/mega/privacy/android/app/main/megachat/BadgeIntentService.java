package mega.privacy.android.app.main.megachat;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

import me.leolin.shortcutbadger.ShortcutBadger;
import mega.privacy.android.app.R;
import timber.log.Timber;

public class BadgeIntentService extends IntentService {

    private int notificationId = 0;
    private NotificationManager notificationManager;

    public BadgeIntentService() {
        super("BadgeIntentService");
        Timber.d("Constructor");
    }

    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        Timber.d("onStart");
        super.onStart(intent, startId);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Timber.d("onHandleIntent");
        if (intent != null) {
            int badgeCount = intent.getIntExtra("badgeCount", 0);
            Timber.d("Badge count: %d abs: %d", badgeCount, Math.abs(badgeCount));
            notificationManager.cancel(notificationId);
            notificationId++;

            Notification.Builder builder = new Notification.Builder(getApplicationContext())
                    .setContentTitle("")
                    .setContentText("")
                    .setSmallIcon(R.drawable.ic_launcher);
            Notification notification = builder.build();
            ShortcutBadger.applyNotification(getApplicationContext(), notification, Math.abs(badgeCount));
            notificationManager.notify(notificationId, notification);
        }
    }
}