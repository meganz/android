package mega.privacy.android.app.presentation.settings.camerauploads.tiles

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dividers.DividerType
import mega.privacy.android.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.core.ui.controls.lists.GenericTwoLineListItem
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * A [Composable] that displays the Camera Uploads (Primary Folder) Folder Name from Cloud Drive.
 * Clicking the Tile allows the User to select a new Primary Cloud Drive Folder
 *
 * @param primaryFolderName The Camera Uploads Cloud Drive Folder name
 * @param onItemClicked Lambda to execute when the Tile is clicked
 * @param modifier The [Modifier]
 */
@Composable
internal fun CameraUploadsFolderNodeTile(
    primaryFolderName: String,
    onItemClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        GenericTwoLineListItem(
            modifier = Modifier.testTag(CAMERA_UPLOADS_FOLDER_NODE_TILE),
            title = stringResource(R.string.settings_mega_camera_upload_folder),
            subtitle = primaryFolderName,
            showEntireSubtitle = true,
            onItemClicked = onItemClicked,
        )
        MegaDivider(
            modifier = Modifier.testTag(CAMERA_UPLOADS_FOLDER_NODE_TILE_DIVIDER),
            dividerType = DividerType.FullSize,
        )
    }
}

/**
 * A Preview [Composable] for [CameraUploadsFolderNodeTile]
 */
@CombinedThemePreviews
@Composable
private fun CameraUploadsFolderNodeTilePreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        CameraUploadsFolderNodeTile(
            primaryFolderName = "Camera Uploads",
            onItemClicked = {},
        )
    }
}

/**
 * Test Tags for the Camera Uploads Folder Node Tile
 */
internal const val CAMERA_UPLOADS_FOLDER_NODE_TILE =
    "camera_uploads_folder_node_tile:generic_two_line_list_item"
internal const val CAMERA_UPLOADS_FOLDER_NODE_TILE_DIVIDER =
    "camera_uploads_folder_node_tile:mega_divider"