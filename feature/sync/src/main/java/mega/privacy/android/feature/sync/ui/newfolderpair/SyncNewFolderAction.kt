package mega.privacy.android.feature.sync.ui.newfolderpair

import androidx.documentfile.provider.DocumentFile

internal sealed interface SyncNewFolderAction {

    /**
     * @param documentFile - the document file of selected folder
     */
    data class LocalFolderSelected(val documentFile: DocumentFile) : SyncNewFolderAction

    data object NextClicked : SyncNewFolderAction

    data object StorageOverquotaShown : SyncNewFolderAction

    data object SyncListScreenOpened : SyncNewFolderAction
}
