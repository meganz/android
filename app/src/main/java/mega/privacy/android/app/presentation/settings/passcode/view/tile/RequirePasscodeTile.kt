package mega.privacy.android.app.presentation.settings.passcode.view.tile

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.lists.GenericTwoLineListItem
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Require passcode tile
 *
 * @param onItemClicked
 * @param modifier
 */
@Composable
internal fun RequirePasscodeTile(
    onItemClicked: () -> Unit,
    subTitle: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        GenericTwoLineListItem(
            modifier = Modifier.testTag(REQUIRE_PASSCODE_TILE),
            title = stringResource(R.string.settings_require_passcode),
            subtitle = subTitle,
            onItemClicked = onItemClicked,
        )
    }
}

/**
 * Require passcode tile preview
 */
@CombinedThemePreviews
@Composable
private fun RequirePasscodeTilePreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        RequirePasscodeTile(
            onItemClicked = {},
            subTitle = "30 Seconds"
        )
    }
}

internal const val REQUIRE_PASSCODE_TILE = "require_passcode_tile:generic_two_line_list_item"