package mega.privacy.android.app.presentation.view.extension

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.node.FolderNode

@Composable
internal fun FolderNode.folderInfo(): String {
    return if (childFolderCount == 0 && childFileCount == 0) {
        stringResource(R.string.file_browser_empty_folder)
    } else if (childFolderCount == 0 && childFileCount > 0) {
        pluralStringResource(R.plurals.num_files_with_parameter, childFileCount, childFileCount)
    } else if (childFileCount == 0 && childFolderCount > 0) {
        pluralStringResource(
            R.plurals.num_folders_with_parameter,
            childFolderCount,
            childFolderCount
        )
    } else {
        pluralStringResource(
            R.plurals.num_folders_num_files,
            childFolderCount,
            childFolderCount
        ) + pluralStringResource(
            R.plurals.num_folders_num_files_2,
            childFileCount,
            childFileCount
        )
    }
}
