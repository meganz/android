package mega.privacy.android.app.lollipop.megachat;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import me.leolin.shortcutbadger.ShortcutBadger;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.LogUtil;

public class BadgeIntentService extends IntentService {

    private  int notificationId = 0;
    private NotificationManager notificationManager;

    public BadgeIntentService() {
        super("BadgeIntentService");
        LogUtil.logDebug("Constructor");
    }

    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        LogUtil.logDebug("onStart");
        super.onStart(intent, startId);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        LogUtil.logDebug("onHandleIntent");
        if (intent != null) {
            int badgeCount = intent.getIntExtra("badgeCount", 0);
            LogUtil.logDebug("Badge count: " + badgeCount + " abs: " + Math.abs(badgeCount));
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