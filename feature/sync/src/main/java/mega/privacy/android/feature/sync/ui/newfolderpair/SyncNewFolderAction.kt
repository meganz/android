package mega.privacy.android.feature.sync.ui.newfolderpair

import android.net.Uri

internal sealed interface SyncNewFolderAction {

    /**
     * @param path - selected local folder path
     */
    data class LocalFolderSelected(val path: Uri) : SyncNewFolderAction

    object NextClicked : SyncNewFolderAction
}