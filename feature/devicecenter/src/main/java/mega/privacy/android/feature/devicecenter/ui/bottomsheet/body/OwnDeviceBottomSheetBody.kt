package mega.privacy.android.feature.devicecenter.ui.bottomsheet.body

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
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
 * @param hasSyncedFolders True if the device has synced folders. False otherwise
 * @param onCameraUploadsClicked Lambda that is executed when the "Camera uploads" Tile is selected
 * @param onRenameDeviceClicked Lambda that is executed when the "Rename" Tile is selected
 * @param onInfoClicked Lambda that is executed when the "Info" Tile is selected
 * @param isSyncFeatureFlagEnabled True if Sync feature flag is enabled. False otherwise
 */
@Composable
internal fun OwnDeviceBottomSheetBody(
    isCameraUploadsEnabled: Boolean,
    hasSyncedFolders: Boolean,
    onCameraUploadsClicked: () -> Unit,
    onRenameDeviceClicked: () -> Unit,
    onInfoClicked: () -> Unit,
    isSyncFeatureFlagEnabled: Boolean = false,
) {
    Column(modifier = Modifier.testTag(BOTTOM_SHEET_BODY_OWN_DEVICE)) {
        if ((isSyncFeatureFlagEnabled && hasSyncedFolders) || isCameraUploadsEnabled) {
            InfoBottomSheetTile(onActionClicked = onInfoClicked)
        }
        if (!isSyncFeatureFlagEnabled) {
            CameraUploadsBottomSheetTile(
                isCameraUploadsEnabled = isCameraUploadsEnabled,
                onActionClicked = onCameraUploadsClicked,
            )
        }
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
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        OwnDeviceBottomSheetBody(
            isCameraUploadsEnabled = isCameraUploadsEnabled,
            hasSyncedFolders = true,
            onCameraUploadsClicked = {},
            onRenameDeviceClicked = {},
            onInfoClicked = {},
        )
    }
}