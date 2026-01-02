package mega.privacy.android.core.nodecomponents.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.core.nodecomponents.R
import java.text.DecimalFormat

/**
 * Represents different types of node subtitles that can be resolved to localized strings
 * in Composable functions using Compose's context.
 */
sealed class NodeSubtitleText {
    /**
     * For file nodes: displays file size and modification time
     * @param fileSizeResId String resource ID for file size formatting
     * @param fileSizeValue Pre-calculated file size value (raw number)
     * @param modificationTime File modification time in milliseconds
     * @param showPublicLinkCreationTime Whether to show public link creation time instead of modification time
     * @param publicLinkCreationTime Public link creation time in milliseconds (optional)
     */
    data class FileSubtitle(
        @StringRes val fileSizeResId: Int,
        val fileSizeValue: Double,
        val modificationTime: Long,
        val showPublicLinkCreationTime: Boolean,
        val publicLinkCreationTime: Long? = null,
    ) : NodeSubtitleText()

    /**
     * For folder nodes with proper pluralization
     * @param childFolderCount Number of child folders
     * @param childFileCount Number of child files
     */
    data class FolderSubtitle(
        val childFolderCount: Int,
        val childFileCount: Int,
    ) : NodeSubtitleText()

    /**
     * For shared items
     * @param shareCount Number of shares
     * @param user User email/name (optional)
     * @param userFullName User full name (optional)
     * @param isVerified Whether the user is verified
     */
    data class SharedSubtitle(
        val shareCount: Int,
        val user: String? = null,
        val userFullName: String? = null,
        val isVerified: Boolean = false,
    ) : NodeSubtitleText()

    /**
     * Empty string
     */
    object Empty : NodeSubtitleText()
}

/**
 * Composable function that resolves NodeSubtitleText to a localized string
 * @return Localized string representation of the node subtitle
 */
@Composable
fun NodeSubtitleText.text(): String {
    return when (this) {
        is NodeSubtitleText.FileSubtitle -> {
            val locale = Locale.current.platformLocale
            val time = publicLinkCreationTime.takeIf {
                showPublicLinkCreationTime
            } ?: modificationTime
            val format = DecimalFormat("#.##")
            val formattedFileSize = format.format(fileSizeValue)
            val fileSizeText = stringResource(fileSizeResId, formattedFileSize)
            if (time != 0L) {
                val formattedDate = formatModifiedDate(locale, time)
                stringResource(
                    R.string.file_subtitle_format,
                    fileSizeText,
                    formattedDate
                )
            } else {
                fileSizeText
            }
        }

        is NodeSubtitleText.FolderSubtitle -> {
            when {
                childFolderCount == 0 && childFileCount == 0 ->
                    stringResource(R.string.file_browser_empty_folder)

                childFolderCount == 0 && childFileCount > 0 ->
                    pluralStringResource(
                        R.plurals.num_files_with_parameter,
                        childFileCount,
                        childFileCount
                    )

                childFileCount == 0 && childFolderCount > 0 ->
                    pluralStringResource(
                        R.plurals.num_folders_with_parameter,
                        childFolderCount,
                        childFolderCount
                    )

                else -> {
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
        }

        is NodeSubtitleText.SharedSubtitle -> {
            when (shareCount) {
                0 -> if (!isVerified) user else ""
                1 -> if (isVerified) userFullName else ""
                else -> pluralStringResource(
                    R.plurals.general_num_shared_with,
                    shareCount,
                    shareCount
                )
            } ?: ""
        }

        is NodeSubtitleText.Empty -> ""
    }
}