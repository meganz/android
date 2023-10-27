package mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.feature.devicecenter.R

/**
 * Test Tag for the Show in Cloud Drive Bottom Sheet Tile
 */
internal const val BOTTOM_SHEET_TILE_SHOW_IN_CLOUD_DRIVE =
    "show_in_cloud_drive_bottom_sheet_tile:menu_action_list_tile_show_in_cloud_drive"

/**
 * A [Composable] Bottom Sheet Tile that displays "Show in Cloud Drive"
 *
 * @param onActionClicked Lambda that is executed when the Tile is selected
 */
@Composable
internal fun ShowInCloudDriveBottomSheetTile(
    onActionClicked: () -> Unit,
) {
    MenuActionListTile(
        modifier = Modifier.testTag(BOTTOM_SHEET_TILE_SHOW_IN_CLOUD_DRIVE),
        addSeparator = false,
        text = stringResource(R.string.device_center_bottom_sheet_item_show_in_cloud_drive),
        icon = painterResource(id = R.drawable.ic_bottom_sheet_cloud_drive),
        onActionClicked = onActionClicked,
    )
}

/**
 * A Preview Composable that displays the Show in Cloud Drive Tile
 */
@CombinedThemePreviews
@Composable
private fun PreviewShowInCloudDriveBottomSheetTile() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ShowInCloudDriveBottomSheetTile(onActionClicked = {})
    }
}