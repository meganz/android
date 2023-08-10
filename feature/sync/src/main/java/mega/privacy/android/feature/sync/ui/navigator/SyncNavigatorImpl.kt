package mega.privacy.android.feature.sync.ui.navigator

import android.app.Activity
import android.content.Context
import mega.privacy.android.feature.sync.data.service.SyncBackgroundService
import javax.inject.Inject

internal class SyncNavigatorImpl @Inject constructor() : SyncNavigator {

    override fun startSyncService(activity: Activity) {
        startSyncBackgroundService(activity)
    }

    private fun startSyncBackgroundService(context: Context) {
        SyncBackgroundService.start(context)
    }
}
