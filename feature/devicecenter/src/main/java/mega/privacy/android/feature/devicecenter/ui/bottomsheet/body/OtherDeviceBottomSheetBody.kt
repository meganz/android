package mega.privacy.android.feature.devicecenter.ui.bottomsheet.body

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles.InfoBottomSheetTile
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles.RenameDeviceBottomSheetTile

/**
 * Test Tag for the Other Device Bottom Sheet Body
 */
internal const val BOTTOM_SHEET_BODY_OTHER_DEVICE =
    "other_device_bottom_sheet_body:column_options_list"

/**
 * A [Composable] Bottom Sheet Body that displays options specific to a Device under the "Other
 * devices" category
 *
 * @param onRenameDeviceClicked Lambda that is executed when the "Rename" Tile is selected
 * @param onInfoClicked Lambda that is executed when the "Info" Tile is selected
 */
@Composable
internal fun OtherDeviceBottomSheetBody(
    onRenameDeviceClicked: () -> Unit,
    onInfoClicked: () -> Unit,
) {
    Column(modifier = Modifier.testTag(BOTTOM_SHEET_BODY_OTHER_DEVICE)) {
        InfoBottomSheetTile(onActionClicked = onInfoClicked)
        RenameDeviceBottomSheetTile(onActionClicked = onRenameDeviceClicked)
    }
}

/**
 * A Preview Composable that displays the Bottom Sheet and its Options for a Device under the "Other
 * devices" category
 */
@CombinedThemePreviews
@Composable
private fun PreviewOtherDeviceBottomSheet() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        OtherDeviceBottomSheetBody(
            onRenameDeviceClicked = {},
            onInfoClicked = {},
        )
    }
}