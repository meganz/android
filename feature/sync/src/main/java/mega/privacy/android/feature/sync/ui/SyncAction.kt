package mega.privacy.android.feature.sync.ui

import android.net.Uri
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder

/**
 * Action from UI
 */
sealed interface SyncAction {

    /**
     * @param enabled - is auto sync enabled
     */
    data class AutoSyncChecked(val enabled: Boolean) : SyncAction

    /**
     * @param remoteFolder - selected remote folder
     */
    data class RemoteFolderSelected(val remoteFolder: RemoteFolder) : SyncAction

    /**
     * @param path - selected local folder path
     */
    data class LocalFolderSelected(val path: Uri) : SyncAction

    /**
     * Sync button clicked
     */
    object SyncClicked : SyncAction
}
