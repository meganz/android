package mega.privacy.android.app.presentation.settings.camerauploads.tiles

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadOptionUiItem
import mega.privacy.android.core.ui.controls.dividers.DividerType
import mega.privacy.android.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.core.ui.controls.lists.GenericTwoLineListItem
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * A [Composable] that shows the type of content that can be uploaded by Camera Uploads
 *
 * @param uploadOptionUiItem Determines the type of content that Camera Uploads can upload
 * @param onItemClicked Lambda to execute when the Tile is clicked
 * @param modifier The [Modifier]
 */
@Composable
internal fun FileUploadTile(
    uploadOptionUiItem: UploadOptionUiItem,
    onItemClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        GenericTwoLineListItem(
            modifier = Modifier.testTag(FILE_UPLOAD_TILE),
            title = stringResource(R.string.settings_camera_upload_what_to_upload),
            subtitle = stringResource(uploadOptionUiItem.textRes),
            onItemClicked = onItemClicked,
        )
        MegaDivider(
            modifier = Modifier.testTag(FILE_UPLOAD_TILE_DIVIDER),
            dividerType = DividerType.FullSize,
        )
    }
}

/**
 * A Preview [Composable] for [FileUploadTile]
 *
 * @param uploadOptionUiItem Determines the type of content that Camera Uploads can upload
 */
@CombinedThemePreviews
@Composable
private fun FileUploadTilePreview(
    @PreviewParameter(UploadOptionUiItemParameterProvider::class) uploadOptionUiItem: UploadOptionUiItem,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        FileUploadTile(
            uploadOptionUiItem = uploadOptionUiItem,
            onItemClicked = {},
        )
    }
}

private class UploadOptionUiItemParameterProvider : PreviewParameterProvider<UploadOptionUiItem> {
    override val values: Sequence<UploadOptionUiItem>
        get() = UploadOptionUiItem.entries.asSequence()

}

/**
 * Test Tags for the File Upload Tile
 */
internal const val FILE_UPLOAD_TILE = "file_upload_tile:generic_two_line_list_item"
internal const val FILE_UPLOAD_TILE_DIVIDER = "file_upload_tile:mega_divider"