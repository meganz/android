package mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.ui.res.stringResource
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.resources.R as sharedResR
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * A [Composable] Bottom Sheet Tile that displays "Add new sync"
 *
 * @param onActionClicked Lambda that is executed when the Tile is selected
 * @param dividerType type for the divider at the bottom. Hidden if NULL
 */
@Composable
internal fun AddNewSyncBottomSheetTile(
    isFreeAccount: Boolean,
    onActionClicked: () -> Unit,
    dividerType: DividerType? = DividerType.BigStartPadding,
) {
    MenuActionListTile(
        text = stringResource(id = sharedResR.string.device_center_sync_add_new_syn_button_option),
        modifier = Modifier.testTag(TEST_TAG_BOTTOM_SHEET_TILE_ADD_NEW_SYNC),
        icon = painterResource(id = iconPackR.drawable.ic_sync_01),
        dividerType = dividerType,
        onActionClicked = onActionClicked,
        trailingItem = {
            if (isFreeAccount) {
                MegaText(
                    text = stringResource(id = sharedResR.string.general_pro_only_label),
                    textColor = TextColor.Accent,
                    modifier = Modifier.testTag(
                        TEST_TAG_BOTTOM_SHEET_TILE_ADD_NEW_SYNC_PRO_ONLY_LABEL
                    )
                )
            }
        }
    )
}

/**
 * A Preview Composable that displays the Rename Tile
 */
@CombinedThemePreviews
@Composable
private fun AddNewSyncBottomSheetTilePreview(
    @PreviewParameter(BooleanProvider::class) isFreeAccount: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        AddNewSyncBottomSheetTile(isFreeAccount = isFreeAccount, onActionClicked = {})
    }
}

/**
 * Test Tag for the Add New Sync Bottom Sheet Tile
 */
internal const val TEST_TAG_BOTTOM_SHEET_TILE_ADD_NEW_SYNC =
    "device_bottom_sheet_tile:add_new_sync"

/**
 * Test Tag for the Add New Sync Bottom Sheet Tile "Pro only" label
 */
internal const val TEST_TAG_BOTTOM_SHEET_TILE_ADD_NEW_SYNC_PRO_ONLY_LABEL =
    "device_bottom_sheet_tile:add_new_sync_pro_only_label"