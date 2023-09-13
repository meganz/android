package mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.core.ui.controls.lists.MenuActionListTileWithBody
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.feature.devicecenter.R

/**
 * Test Tag for the Camera Uploads Bottom Sheet Tile
 */
internal const val BOTTOM_SHEET_TILE_CAMERA_UPLOADS =
    "camera_uploads_bottom_sheet_tile:menu_action_list_tile_with_body_camera_uploads"

/**
 * A [Composable] Bottom Sheet Tile that displays "Camera uploads" and its state
 *
 * @param isCameraUploadsEnabled true if Camera Uploads is Enabled, and false if otherwise
 * @param onActionClicked Lambda that is executed when the Tile is selected
 */
@Composable
internal fun CameraUploadsBottomSheetTile(
    isCameraUploadsEnabled: Boolean,
    onActionClicked: () -> Unit,
) {
    MenuActionListTileWithBody(
        modifier = Modifier.testTag(BOTTOM_SHEET_TILE_CAMERA_UPLOADS),
        title = stringResource(R.string.device_center_bottom_sheet_item_camera_uploads),
        body = stringResource(
            if (isCameraUploadsEnabled) {
                R.string.device_center_bottom_sheet_item_camera_uploads_enabled_status
            } else {
                R.string.device_center_bottom_sheet_item_camera_uploads_disabled_status
            }
        ),
        icon = R.drawable.ic_bottom_sheet_camera_uploads,
        onActionClicked = onActionClicked,
    )
}

/**
 * A Preview Composable that displays the Camera Uploads Tile with an Enabled Status
 */
@CombinedThemePreviews
@Composable
private fun PreviewEnabledCameraUploadsTile() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        CameraUploadsBottomSheetTile(
            isCameraUploadsEnabled = true,
            onActionClicked = {},
        )
    }
}

/**
 * A Preview Composable that displays the Camera Uploads Tile with a Disabled Status
 */
@CombinedThemePreviews
@Composable
private fun PreviewDisabledCameraUploadsTile() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        CameraUploadsBottomSheetTile(
            isCameraUploadsEnabled = false,
            onActionClicked = {},
        )
    }
}