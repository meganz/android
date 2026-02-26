package mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR

/**
 * A [Composable] Bottom Sheet Tile that displays "Remove connection"
 *
 * @param onActionClicked Lambda that is executed when the Tile is selected
 */
@Composable
internal fun RemoveConnectionBottomSheetTile(
    onActionClicked: () -> Unit,
    dividerType: DividerType? = DividerType.BigStartPadding,
) {
    MenuActionListTile(
        modifier = Modifier.testTag(BOTTOM_SHEET_TILE_REMOVE_CONNECTION),
        dividerType = dividerType,
        text = stringResource(sharedR.string.device_center_bottom_sheet_item_remove_connection),
        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Trash),
        isDestructive = false,
        onActionClicked = onActionClicked,
    )
}

/**
 * A Preview Composable that displays the Remove Connection Tile
 */
@CombinedThemePreviews
@Composable
private fun PreviewRemoveConnectionBottomSheetTile() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        RemoveConnectionBottomSheetTile(onActionClicked = {})
    }
}

/**
 * Test Tag for the Remove Connection Bottom Sheet Tile
 */
internal const val BOTTOM_SHEET_TILE_REMOVE_CONNECTION =
    "remove_connection_bottom_sheet_tile:menu_action_list_tile_remove_connection"
