package mega.privacy.android.feature.sync.ui.navigator

import android.app.Activity
import mega.privacy.android.feature.sync.data.service.SyncBackgroundService
import javax.inject.Inject

internal class SyncNavigatorImpl @Inject constructor() : SyncNavigator {

    override fun startSyncService(activity: Activity) {
        if (!SyncBackgroundService.isRunning()) {
            SyncBackgroundService.start(activity)
        }
    }

    override fun stopSyncService(activity: Activity) {
        if (SyncBackgroundService.isRunning()) {
            SyncBackgroundService.stop(activity)
        }
    }
}
