package mega.privacy.android.feature.devicecenter.ui.bottomsheet.body

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles.CameraUploadsBottomSheetTile
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles.InfoBottomSheetTile
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles.RenameDeviceBottomSheetTile

/**
 * Test Tag for the Own Device Bottom Sheet Body
 */
internal const val BOTTOM_SHEET_BODY_OWN_DEVICE =
    "own_device_bottom_sheet_body:column_options_list"

/**
 * A [Composable] Bottom Sheet Body that displays options specific to a Device under the "This
 * device" category
 *
 * @param isCameraUploadsEnabled true if Camera Uploads is Enabled, and false if otherwise
 * @param onCameraUploadsClicked Lambda that is executed when the "Camera uploads" Tile is selected
 * @param onRenameDeviceClicked Lambda that is executed when the "Rename" Tile is selected
 * @param onInfoClicked Lambda that is executed when the "Info" Tile is selected
 */
@Composable
internal fun OwnDeviceBottomSheetBody(
    isCameraUploadsEnabled: Boolean,
    onCameraUploadsClicked: () -> Unit,
    onRenameDeviceClicked: () -> Unit,
    onInfoClicked: () -> Unit,
) {
    Column(modifier = Modifier.testTag(BOTTOM_SHEET_BODY_OWN_DEVICE)) {
        if (isCameraUploadsEnabled) {
            InfoBottomSheetTile(onActionClicked = onInfoClicked)
        }
        CameraUploadsBottomSheetTile(
            isCameraUploadsEnabled = isCameraUploadsEnabled,
            onActionClicked = onCameraUploadsClicked,
        )
        RenameDeviceBottomSheetTile(onActionClicked = onRenameDeviceClicked)
    }
}

/**
 * A Preview Composable that displays the Bottom Sheet and its options for a Device under the
 * "This device" category
 *
 * @param isCameraUploadsEnabled The Camera Uploads state
 */
@CombinedThemePreviews
@Composable
private fun PreviewOwnDeviceBottomSheet(
    @PreviewParameter(BooleanProvider::class) isCameraUploadsEnabled: Boolean,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        OwnDeviceBottomSheetBody(
            isCameraUploadsEnabled = isCameraUploadsEnabled,
            onCameraUploadsClicked = {},
            onRenameDeviceClicked = {},
            onInfoClicked = {},
        )
    }
}