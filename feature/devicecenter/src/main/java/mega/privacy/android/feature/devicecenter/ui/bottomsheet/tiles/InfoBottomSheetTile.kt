package mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.legacy.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.feature.devicecenter.R

/**
 * Test Tag for the Info Bottom Sheet Tile
 */
internal const val BOTTOM_SHEET_TILE_INFO =
    "info_bottom_sheet_tile:menu_action_list_tile_info"

/**
 * A [Composable] Bottom Sheet Tile that displays "Info"
 *
 * @param onActionClicked Lambda that is executed when the Tile is selected
 * @param addSeparator Adds a Divider at the bottom if true, and none if otherwise
 */
@Composable
internal fun InfoBottomSheetTile(
    onActionClicked: () -> Unit,
    addSeparator: Boolean = true,
) {
    MenuActionListTile(
        modifier = Modifier.testTag(BOTTOM_SHEET_TILE_INFO),
        text = stringResource(R.string.device_center_bottom_sheet_item_info),
        icon = painterResource(id = R.drawable.ic_bottom_sheet_info),
        addSeparator = addSeparator,
        onActionClicked = onActionClicked,
    )
}

/**
 * A Preview Composable that displays the Info Tile
 */
@CombinedThemePreviews
@Composable
private fun PreviewInfoBottomSheetTile() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        InfoBottomSheetTile(onActionClicked = {})
    }
}