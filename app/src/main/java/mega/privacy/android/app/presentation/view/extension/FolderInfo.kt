package mega.privacy.android.app.presentation.view.extension

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.shared.resources.R as SharedR

@Composable
internal fun FolderNode.folderInfo(): String {
    return if (childFolderCount == 0 && childFileCount == 0) {
        stringResource(SharedR.string.empty_file_browser_folder)
    } else if (childFolderCount == 0 && childFileCount > 0) {
        pluralStringResource(
            SharedR.plurals.num_of_files_with_parameter,
            childFileCount,
            childFileCount
        )
    } else if (childFileCount == 0 && childFolderCount > 0) {
        pluralStringResource(
            SharedR.plurals.num_of_folders_with_parameter,
            childFolderCount,
            childFolderCount
        )
    } else {
        pluralStringResource(
            SharedR.plurals.num_of_folders_and_num_of_files,
            childFolderCount,
            childFolderCount
        ) + pluralStringResource(
            SharedR.plurals.num_of_files_with_parameter,
            childFileCount,
            childFileCount
        )
    }
}
