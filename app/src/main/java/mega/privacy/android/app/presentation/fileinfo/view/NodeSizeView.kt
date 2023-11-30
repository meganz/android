package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.preview.CombinedTextAndThemePreviews

/**
 * View to show the file or folder total size
 */
@Composable
internal fun NodeSizeView(
    forFolder: Boolean,
    sizeString: String,
    modifier: Modifier = Modifier,
) = FileInfoTitledText(
    title = stringResource(id = if (forFolder) R.string.file_properties_info_size else R.string.file_properties_info_size_file),
    text = sizeString,
    modifier = modifier
)

/**
 * Preview for [NodeSizeView]
 */
@CombinedTextAndThemePreviews
@Composable
private fun NodeSizePreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        NodeSizeView(forFolder = true, sizeString = "1024 Bytes")
    }
}