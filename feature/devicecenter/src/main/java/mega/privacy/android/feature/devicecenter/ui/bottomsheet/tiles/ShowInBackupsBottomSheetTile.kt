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
 * Test Tag for the Show in Backups Bottom Sheet Tile
 */
internal const val BOTTOM_SHEET_TILE_SHOW_IN_BACKUPS =
    "show_in_backups_bottom_sheet_tile:menu_action_list_tile_show_in_backups"

/**
 * A [Composable] Bottom Sheet Tile that displays "Show in Backups"
 *
 * @param onActionClicked Lambda that is executed when the Tile is selected
 */
@Composable
internal fun ShowInBackupsBottomSheetTile(
    onActionClicked: () -> Unit,
) {
    MenuActionListTile(
        modifier = Modifier.testTag(BOTTOM_SHEET_TILE_SHOW_IN_BACKUPS),
        text = stringResource(R.string.device_center_bottom_sheet_item_show_in_backups),
        icon = R.drawable.ic_bottom_sheet_backups,
        onActionClicked = onActionClicked,
    )
}

/**
 * A Preview Composable that displays the Show in Backups Tile
 */
@CombinedThemePreviews
@Composable
private fun PreviewShowInBackupsBottomSheetTime() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ShowInBackupsBottomSheetTile(onActionClicked = {})
    }
}