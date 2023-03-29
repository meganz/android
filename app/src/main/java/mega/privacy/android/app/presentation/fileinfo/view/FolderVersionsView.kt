package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Util
import mega.privacy.android.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme

/**
 * Shows several Titled texts with information of versions contained in a folder
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun FolderVersionsView(
    numberOfVersions: Int,
    currentVersionsSizeInBytes: Long,
    previousVersionsSizeInBytes: Long,
    modifier: Modifier = Modifier,
) =
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        FileInfoTitledText(
            title = stringResource(R.string.title_section_versions),
            text = pluralStringResource(
                id = R.plurals.number_of_versions_inside_folder,
                count = numberOfVersions,
                numberOfVersions
            )
        )
        FileInfoTitledText(
            title = stringResource(R.string.file_properties_folder_current_versions),
            text = Util.getSizeString(currentVersionsSizeInBytes),
        )
        FileInfoTitledText(
            title = stringResource(R.string.file_properties_folder_previous_versions),
            text = Util.getSizeString(previousVersionsSizeInBytes),
        )
    }


/**
 * Preview for [FolderVersionsView]
 */
@CombinedTextAndThemePreviews
@Composable
private fun FolderSizePreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        FolderVersionsView(
            numberOfVersions = 5,
            currentVersionsSizeInBytes = 1024,
            previousVersionsSizeInBytes = 2560,
        )
    }
}