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
 * A [Composable] that displays the Camera Uploads (Primary Folder) Local Folder Path. Clicking the
 * Tile allows the User to select a new Primary Local Folder
 *
 * @param primaryFolderPath The Camera Uploads Local Primary Folder Path
 * @param onItemClicked Lambda to execute when the Tile is clicked
 * @param modifier The [Modifier]
 */
@Composable
internal fun CameraUploadsLocalFolderTile(
    primaryFolderPath: String,
    onItemClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        GenericTwoLineListItem(
            modifier = Modifier.testTag(CAMERA_UPLOADS_LOCAL_FOLDER_TILE),
            title = stringResource(R.string.settings_local_camera_upload_folder),
            subtitle = primaryFolderPath,
            showEntireSubtitle = true,
            onItemClicked = onItemClicked,
        )
        MegaDivider(
            modifier = Modifier.testTag(CAMERA_UPLOADS_LOCAL_FOLDER_TILE_DIVIDER),
            dividerType = DividerType.FullSize,
        )
    }
}

/**
 * A Preview [Composable] for [CameraUploadsLocalFolderTile]
 */
@CombinedThemePreviews
@Composable
private fun CameraUploadsLocalFolderTilePreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        CameraUploadsLocalFolderTile(
            primaryFolderPath = "primary/folder/path",
            onItemClicked = {},
        )
    }
}

/**
 * Test Tags for the Camera Uploads Local Folder Tile
 */
internal const val CAMERA_UPLOADS_LOCAL_FOLDER_TILE =
    "camera_uploads_local_folder_tile:generic_two_line_list_item"
internal const val CAMERA_UPLOADS_LOCAL_FOLDER_TILE_DIVIDER =
    "camera_uploads_local_folder_tile:mega_divider"