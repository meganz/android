package mega.privacy.android.app.presentation.offline.optionbottomsheet.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.shared.resources.R as SharedR

/**
 * Get the description for the offline node
 */
@Composable
internal fun getOfflineNodeDescription(offlineFileInformation: OfflineFileInformation): String {
    val context = LocalContext.current
    return if (offlineFileInformation.isFolder) {
        offlineFileInformation.folderInfo?.let { folderInfo ->
            if (folderInfo.numFolders == 0 && folderInfo.numFiles == 0) {
                stringResource(SharedR.string.empty_file_browser_folder)
            } else if (folderInfo.numFolders == 0 && folderInfo.numFiles > 0) {
                pluralStringResource(
                    SharedR.plurals.num_of_files_with_parameter,
                    folderInfo.numFiles,
                    folderInfo.numFiles
                )
            } else if (folderInfo.numFiles == 0 && folderInfo.numFolders > 0) {
                pluralStringResource(
                    SharedR.plurals.num_of_folders_with_parameter,
                    folderInfo.numFolders,
                    folderInfo.numFolders
                )
            } else {
                pluralStringResource(
                    SharedR.plurals.num_of_folders_and_num_of_files,
                    folderInfo.numFolders,
                    folderInfo.numFolders
                ) + pluralStringResource(
                    SharedR.plurals.num_of_files_with_parameter,
                    folderInfo.numFiles,
                    folderInfo.numFiles
                )
            }
        } ?: run {
            ""
        }
    } else {
        formatFileSize(offlineFileInformation.totalSize, context)
            .plus(offlineFileInformation.addedTime?.let {
                " · ".plus(
                    formatModifiedDate(
                        java.util.Locale(
                            Locale.current.language, Locale.current.region
                        ),
                        it
                    )
                )
            } ?: "")
    }
}
