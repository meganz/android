package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.preview.CombinedTextAndThemePreviews

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
        numberOfFolders <= 0 && numberOfFiles <= 0 -> stringResource(R.string.file_browser_empty_folder)
        numberOfFolders <= 0 && numberOfFiles > 0 ->
            pluralStringResource(R.plurals.num_files_with_parameter, numberOfFiles, numberOfFiles)

        numberOfFiles <= 0 ->
            pluralStringResource(
                R.plurals.num_folders_with_parameter,
                numberOfFolders,
                numberOfFolders
            )

        else -> {
            pluralStringResource(
                R.plurals.num_folders_num_files,
                numberOfFolders,
                numberOfFolders
            ) + pluralStringResource(
                R.plurals.num_folders_num_files_2,
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
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        FolderContentView(numberOfFolders = 3, numberOfFiles = 24)
    }
}