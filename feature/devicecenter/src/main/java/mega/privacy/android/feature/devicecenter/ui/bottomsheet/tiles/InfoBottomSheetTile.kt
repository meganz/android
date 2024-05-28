package mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.feature.devicecenter.R
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Test Tag for the Info Bottom Sheet Tile
 */
internal const val BOTTOM_SHEET_TILE_INFO =
    "info_bottom_sheet_tile:menu_action_list_tile_info"

/**
 * A [Composable] Bottom Sheet Tile that displays "Info"
 *
 * @param onActionClicked Lambda that is executed when the Tile is selected
 * @param dividerType type for the divider at the bottom. Hidden if NULL
 */
@Composable
internal fun InfoBottomSheetTile(
    onActionClicked: () -> Unit,
    dividerType: DividerType? = DividerType.BigStartPadding,
) {
    MenuActionListTile(
        modifier = Modifier.testTag(BOTTOM_SHEET_TILE_INFO),
        text = stringResource(R.string.device_center_bottom_sheet_item_info),
        icon = painterResource(id = IconPackR.drawable.ic_info_medium_regular_outline),
        dividerType = dividerType,
        onActionClicked = onActionClicked,
    )
}

/**
 * A Preview Composable that displays the Info Tile
 */
@CombinedThemePreviews
@Composable
private fun PreviewInfoBottomSheetTile() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        InfoBottomSheetTile(onActionClicked = {})
    }
}