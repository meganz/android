package mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.feature.devicecenter.R
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

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
        dividerType = null,
        text = stringResource(R.string.device_center_bottom_sheet_item_rename),
        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Edit),
        onActionClicked = onActionClicked,
    )
}

/**
 * A Preview Composable that displays the Rename Tile
 */
@CombinedThemePreviews
@Composable
private fun PreviewRenameDeviceBottomSheetTile() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        RenameDeviceBottomSheetTile(onActionClicked = {})
    }
}