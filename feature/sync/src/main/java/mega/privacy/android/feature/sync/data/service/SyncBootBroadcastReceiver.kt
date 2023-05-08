package mega.privacy.android.feature.sync.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Broadcast receiver that starts the sync service when the device is booted
 * This is needed so that even if the user restarts the device - the sync continues.
 * Note that this will not work on some device vendors and some versions of Android
 */
internal class SyncBootBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent) {
        if (intent.action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            context?.let(SyncBackgroundService::start)
        }
    }
}