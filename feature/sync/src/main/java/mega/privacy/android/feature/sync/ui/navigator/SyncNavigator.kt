package mega.privacy.android.feature.sync.ui.navigator

import android.app.Activity

/**
 * Asks all the necessary permissions and starts the sync service.
 * This is a temporary method for Sync POC
 * It will be removed before this branch gets merged into develop
 */
interface SyncNavigator {

    fun startSyncService(activity: Activity)
}
