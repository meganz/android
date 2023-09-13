package mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.feature.devicecenter.R

/**
 * Test Tag for the Rename Device Bottom Sheet Tile
 */
internal const val BOTTOM_SHEET_TILE_RENAME_DEVICE =
    "rename_device_bottom_sheet_tile:menu_action_list_tile_rename_device"

/**
 * A [Composable] Bottom Sheet Tile that displays "Rename"
 *
 * @param onActionClicked Lambda that is executed when the Tile is selected
 */
@Composable
internal fun RenameDeviceBottomSheetTile(
    onActionClicked: () -> Unit,
) {
    MenuActionListTile(
        modifier = Modifier.testTag(BOTTOM_SHEET_TILE_RENAME_DEVICE),
        text = stringResource(R.string.device_center_bottom_sheet_item_rename),
        icon = R.drawable.ic_bottom_sheet_rename,
        onActionClicked = onActionClicked,
    )
}

/**
 * A Preview Composable that displays the Rename Tile
 */
@CombinedThemePreviews
@Composable
private fun PreviewRenameDeviceBottomSheetTile() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        RenameDeviceBottomSheetTile(onActionClicked = {})
    }
}