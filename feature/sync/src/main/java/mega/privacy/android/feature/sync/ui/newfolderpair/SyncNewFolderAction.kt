package mega.privacy.android.feature.sync.ui.newfolderpair

import android.net.Uri
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder

sealed interface SyncNewFolderAction {

    /**
     * @param remoteFolder - selected remote folder
     */
    data class RemoteFolderSelected(val remoteFolder: RemoteFolder) : SyncNewFolderAction

    /**
     * @param path - selected local folder path
     */
    data class LocalFolderSelected(val path: Uri) : SyncNewFolderAction

    /**
     * @param name - new name of the folder pair
     */
    data class FolderNameChanged(val name: String) : SyncNewFolderAction

    /**
     * Sync button clicked
     */
    object SyncClicked : SyncNewFolderAction
}