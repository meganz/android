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
 * A [Composable] Bottom Sheet Tile that displays "Add backup"
 *
 * @param onActionClicked Lambda that is executed when the Tile is selected
 * @param dividerType type for the divider at the bottom. Hidden if NULL
 */
@Composable
internal fun AddBackupBottomSheetTile(
    isFreeAccount: Boolean,
    onActionClicked: () -> Unit,
    dividerType: DividerType? = DividerType.BigStartPadding,
) {
    MenuActionListTile(
        text = stringResource(id = sharedResR.string.device_center_add_backup_button_option),
        modifier = Modifier.testTag(TEST_TAG_BOTTOM_SHEET_TILE_ADD_BACKUP),
        icon = painterResource(id = iconPackR.drawable.ic_database),
        dividerType = dividerType,
        onActionClicked = onActionClicked,
        trailingItem = {
            if (isFreeAccount) {
                MegaText(
                    text = stringResource(id = sharedResR.string.general_pro_only_label),
                    textColor = TextColor.Primary,
                    modifier = Modifier.testTag(
                        TEST_TAG_BOTTOM_SHEET_TILE_ADD_BACKUP_PRO_ONLY_LABEL
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
private fun AddBackupBottomSheetTilePreview(
    @PreviewParameter(BooleanProvider::class) isFreeAccount: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        AddBackupBottomSheetTile(isFreeAccount = isFreeAccount, onActionClicked = {})
    }
}

/**
 * Test Tag for the "Add backup" Bottom Sheet Tile
 */
internal const val TEST_TAG_BOTTOM_SHEET_TILE_ADD_BACKUP =
    "device_bottom_sheet_tile:add_backup"

/**
 * Test Tag for the "Add backup" Bottom Sheet Tile "Pro only" label
 */
internal const val TEST_TAG_BOTTOM_SHEET_TILE_ADD_BACKUP_PRO_ONLY_LABEL =
    "device_bottom_sheet_tile:add_backup_pro_only_label"