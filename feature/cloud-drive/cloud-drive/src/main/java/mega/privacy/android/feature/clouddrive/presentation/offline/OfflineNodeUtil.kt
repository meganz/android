package mega.privacy.android.feature.clouddrive.presentation.offline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.shared.nodes.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailUriRequest
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.clouddrive.R
import mega.privacy.android.shared.resources.R as SharedR

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
            stringResource(SharedR.string.empty_file_browser_folder)
        }

        numFolders == 0 && numFiles > 0 -> {
            pluralStringResource(
                SharedR.plurals.num_of_files_with_parameter,
                numFiles,
                numFiles
            )
        }

        numFiles == 0 && numFolders > 0 -> {
            pluralStringResource(
                SharedR.plurals.num_of_folders_with_parameter,
                numFolders,
                numFolders
            )
        }

        else -> {
            val foldersText = pluralStringResource(
                SharedR.plurals.num_of_folders_and_num_of_files,
                numFolders,
                numFolders
            )
            val filesText = pluralStringResource(
                SharedR.plurals.num_of_files_with_parameter,
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
    return formatModifiedDate(LocalLocale.current.platformLocale, addedTime)
}

/**
 * Resolves the [ThumbnailData] to hand to Coil for an offline node.
 *
 * Prefers a locally-known URI (image-as-thumbnail or cached thumbnail file) and falls back
 * to a [ThumbnailRequest] keyed by node handle so [mega.privacy.android.app.fetcher.MegaThumbnailFetcher]
 * can lazily resolve the thumbnail (local cache first, server on miss) per visible row.
 */
val OfflineFileInformation.thumbnailData: ThumbnailData?
    get() {
        if (isFolder) return null
        return thumbnail?.let { ThumbnailUriRequest(UriPath(it)) }
            ?: handle.toLongOrNull()
                ?.takeIf { it != -1L }
                ?.let { ThumbnailRequest(NodeId(it)) }
    }
