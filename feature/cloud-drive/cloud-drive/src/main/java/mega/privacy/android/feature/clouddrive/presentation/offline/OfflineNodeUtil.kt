package mega.privacy.android.feature.clouddrive.presentation.offline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.feature.clouddrive.R

@Composable
internal fun getFileTypeIcon(fileName: String): Int? {
    val fileTypeIconMapper = remember { FileTypeIconMapper() }
    val extension = fileName
        .substringAfterLast('.', "")
        .takeIf { it.isNotEmpty() }
        ?: return null

    return fileTypeIconMapper(extension)
}

@Composable
internal fun getOfflineNodeDescription(
    offlineFileInformation: OfflineFileInformation,
): String {
    return if (offlineFileInformation.isFolder) {
        getFolderDescription(offlineFileInformation)
    } else {
        getFileDescription(offlineFileInformation)
    }
}

@Composable
private fun getFolderDescription(offlineFileInformation: OfflineFileInformation): String {
    val folderInfo = offlineFileInformation.folderInfo ?: return ""
    val numFiles = folderInfo.numFiles
    val numFolders = folderInfo.numFolders

    return when {
        numFiles == 0 && numFolders == 0 -> {
            stringResource(R.string.file_browser_empty_folder)
        }

        numFolders == 0 && numFiles > 0 -> {
            pluralStringResource(
                R.plurals.num_files_with_parameter,
                numFiles,
                numFiles
            )
        }

        numFiles == 0 && numFolders > 0 -> {
            pluralStringResource(
                R.plurals.num_folders_with_parameter,
                numFolders,
                numFolders
            )
        }

        else -> {
            val foldersText = pluralStringResource(
                R.plurals.num_folders_num_files,
                numFolders,
                numFolders
            )
            val filesText = pluralStringResource(
                R.plurals.num_folders_num_files_2,
                numFiles,
                numFiles
            )
            foldersText + filesText
        }
    }
}

@Composable
private fun getFileDescription(offlineFileInformation: OfflineFileInformation): String {
    val context = LocalContext.current
    val fileSize = formatFileSize(offlineFileInformation.totalSize, context)
    val modifiedDate = formatModifiedDate(offlineFileInformation)

    return if (modifiedDate.isNotEmpty()) {
        "$fileSize · $modifiedDate"
    } else {
        fileSize
    }
}

@Composable
private fun formatModifiedDate(offlineFileInformation: OfflineFileInformation): String {
    val addedTime = offlineFileInformation.addedTime ?: return ""
    return formatModifiedDate(java.util.Locale.getDefault(), addedTime)
}