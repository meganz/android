package mega.privacy.android.app.presentation.settings.passcode.view.tile

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.lists.GenericTwoLineListItem
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Enable passcode tile
 *
 * @param isChecked
 * @param onItemClicked
 * @param modifier
 */
@Composable
internal fun EnablePasscodeTile(
    isChecked: Boolean,
    onItemClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        GenericTwoLineListItem(
            modifier = Modifier.testTag(ENABLE_PASSCODE_TILE),
            title = stringResource(R.string.settings_passcode_lock_switch),
            onItemClicked = onItemClicked,
            trailingIcons = {
                MegaSwitch(
                    modifier = Modifier.testTag(ENABLE_PASSCODE_TILE_SWITCH),
                    checked = isChecked,
                    onCheckedChange = null,
                )
            }
        )
        if (isChecked) {
            MegaDivider(
                modifier = Modifier.testTag(ENABLE_PASSCODE_TILE_DIVIDER),
                dividerType = DividerType.FullSize,
            )
        }
    }
}

/**
 * Enable passcode tile preview
 *
 * @param isChecked
 */
@CombinedThemePreviews
@Composable
private fun EnablePasscodeTilePreview(
    @PreviewParameter(BooleanProvider::class) isChecked: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        EnablePasscodeTile(
            isChecked = isChecked,
            onItemClicked = {},
        )
    }
}


internal const val ENABLE_PASSCODE_TILE = "enable_passcode_tile:generic_two_line_list_item"
internal const val ENABLE_PASSCODE_TILE_SWITCH = "enable_passcode_tile:mega_switch"
internal const val ENABLE_PASSCODE_TILE_DIVIDER = "enable_passcode_tile:mega_divider"