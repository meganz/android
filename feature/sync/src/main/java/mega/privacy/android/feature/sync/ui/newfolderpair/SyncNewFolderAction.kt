package mega.privacy.android.feature.sync.ui.newfolderpair

import androidx.documentfile.provider.DocumentFile
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore


internal sealed interface SyncNewFolderAction {

    /**
     * @param documentFile - the document file of selected folder
     */
    data class LocalFolderSelected(val documentFile: DocumentFile) : SyncNewFolderAction

    /**
     * @param exclusions - the exclusions configured for a folder pair that does not exist yet. They
     * become the whole `.megaignore` of the local root, replacing any file another MEGA client left
     * there, and are discarded if the user then chooses a different local folder.
     */
    data class ExclusionsStaged(val exclusions: ParsedMegaIgnore) : SyncNewFolderAction

    data object NextClicked : SyncNewFolderAction

    data object StorageOverquotaShown : SyncNewFolderAction

    data object SyncListScreenOpened : SyncNewFolderAction
}
