package mega.privacy.android.feature.sync.ui.mapper.sync

import mega.privacy.android.domain.entity.uri.UriPath

/**
 * Interface for validating the URI of a sync folder.
 */
sealed interface SyncValidityResult {
    data class ShowSnackbar(val messageResId: Int) : SyncValidityResult
    data class ValidFolderSelected(val localFolderUri: UriPath, val folderName: String) :
        SyncValidityResult

    object Invalid : SyncValidityResult
}
