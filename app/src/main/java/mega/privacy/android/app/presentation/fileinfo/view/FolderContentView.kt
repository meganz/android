package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as SharedR

/**
 * Shows a titled text with the total amount of files and folders for a folder
 */
@Composable
internal fun FolderContentView(
    numberOfFolders: Int,
    numberOfFiles: Int,
    modifier: Modifier = Modifier,
) = FileInfoTitledText(
    title = stringResource(R.string.file_properties_info_content),
    text = when {
        numberOfFolders <= 0 && numberOfFiles <= 0 -> stringResource(SharedR.string.empty_file_browser_folder)
        numberOfFolders <= 0 && numberOfFiles > 0 ->
            pluralStringResource(
                SharedR.plurals.num_of_files_with_parameter,
                numberOfFiles,
                numberOfFiles
            )

        numberOfFiles <= 0 ->
            pluralStringResource(
                SharedR.plurals.num_of_folders_with_parameter,
                numberOfFolders,
                numberOfFolders
            )

        else -> {
            pluralStringResource(
                SharedR.plurals.num_of_folders_and_num_of_files,
                numberOfFolders,
                numberOfFolders
            ) + pluralStringResource(
                SharedR.plurals.num_of_files_with_parameter,
                numberOfFiles,
                numberOfFiles
            )
        }
    },
    modifier = modifier
        .fillMaxWidth()
)

/**
 * Preview for [FolderContentView]
 */
@CombinedTextAndThemePreviews
@Composable
private fun FolderContentPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        FolderContentView(numberOfFolders = 3, numberOfFiles = 24)
    }
}