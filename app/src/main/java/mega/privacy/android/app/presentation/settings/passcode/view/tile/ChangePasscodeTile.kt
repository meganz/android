package mega.privacy.android.app.presentation.settings.passcode.view.tile

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.lists.GenericTwoLineListItem
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Change passcode tile
 *
 * @param onItemClicked
 * @param modifier
 */
@Composable
internal fun ChangePasscodeTile(
    onItemClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        GenericTwoLineListItem(
            modifier = Modifier.testTag(CHANGE_PASSCODE_TILE),
            title = stringResource(R.string.settings_change_passcode),
            onItemClicked = onItemClicked,
        )
        MegaDivider(
            modifier = Modifier.testTag(CHANGE_PASSCODE_TILE_DIVIDER),
            dividerType = DividerType.FullSize,
        )
    }
}

/**
 * Change passcode tile preview
 */
@CombinedThemePreviews
@Composable
private fun ChangePasscodeTilePreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ChangePasscodeTile(
            onItemClicked = {},
        )
    }
}

internal const val CHANGE_PASSCODE_TILE = "change_passcode_tile:generic_two_line_list_item"
internal const val CHANGE_PASSCODE_TILE_DIVIDER = "change_passcode_tile:mega_divider"