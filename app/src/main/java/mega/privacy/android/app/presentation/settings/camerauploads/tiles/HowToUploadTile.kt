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
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadConnectionType
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.lists.GenericTwoLineListItem
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * A Composable that displays the User's selected Internet Connection Type used to upload Camera
 * Uploads content
 *
 * @param uploadConnectionType Determines the connection type for uploading content in Camera Uploads
 * Otherwise, both Wi-Fi and Mobile Data can be used
 * @param onItemClicked Lambda to execute when the Tile is clicked
 * @param modifier The [Modifier]
 */
@Composable
internal fun HowToUploadTile(
    uploadConnectionType: UploadConnectionType,
    onItemClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        GenericTwoLineListItem(
            modifier = Modifier.testTag(HOW_TO_UPLOAD_TILE),
            title = stringResource(R.string.settings_camera_upload_how_to_upload),
            subtitle = stringResource(uploadConnectionType.textRes),
            onItemClicked = onItemClicked,
        )
        MegaDivider(
            modifier = Modifier.testTag(HOW_TO_UPLOAD_TILE_DIVIDER),
            dividerType = DividerType.FullSize,
        )
    }
}

/**
 * A Preview [Composable] for [HowToUploadTile]
 *
 * @param uploadConnectionType Determines the connection type for uploading content in Camera Uploads
 */
@CombinedThemePreviews
@Composable
private fun HowToUploadTilePreview(
    @PreviewParameter(UploadConnectionTypeParameterProvider::class) uploadConnectionType: UploadConnectionType,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        HowToUploadTile(
            uploadConnectionType = uploadConnectionType,
            onItemClicked = {},
        )
    }
}

private class UploadConnectionTypeParameterProvider :
    PreviewParameterProvider<UploadConnectionType> {
    override val values: Sequence<UploadConnectionType>
        get() = UploadConnectionType.entries.asSequence()

}

/**
 * Test Tags for the How to Upload Tile
 */
internal const val HOW_TO_UPLOAD_TILE = "how_to_upload_tile:generic_two_line_list_item"
internal const val HOW_TO_UPLOAD_TILE_DIVIDER = "how_to_upload_tile:mega_divider"