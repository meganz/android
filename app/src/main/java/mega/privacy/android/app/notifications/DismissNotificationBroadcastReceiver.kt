package mega.privacy.android.app.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import mega.privacy.android.app.utils.Constants

/**
 * Creates an BroadcastReceiver for dismissing a notification.
 */
class DismissNotificationBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.getSystemService(NOTIFICATION_SERVICE)?.let {
            (it as NotificationManager).cancel(Constants.NOTIFICATION_DOWNLOAD_FINAL)
        }
    }
}
