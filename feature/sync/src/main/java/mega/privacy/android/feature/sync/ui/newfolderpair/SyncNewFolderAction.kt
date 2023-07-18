package mega.privacy.android.feature.sync.ui.newfolderpair

import android.net.Uri
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder

internal sealed interface SyncNewFolderAction {

    /**
     * @param path - selected local folder path
     */
    data class LocalFolderSelected(val path: Uri) : SyncNewFolderAction

    /**
     * @param name - new name of the folder pair
     */
    data class FolderNameChanged(val name: String) : SyncNewFolderAction

    object NextClicked : SyncNewFolderAction
}