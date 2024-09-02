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
 * Fingerprint id tile
 *
 * @param isChecked
 * @param onItemClicked
 * @param modifier
 */
@Composable
internal fun FingerprintIdTile(
    isChecked: Boolean,
    onItemClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        GenericTwoLineListItem(
            modifier = Modifier.testTag(FINGERPRINT_ID_TILE),
            title = stringResource(R.string.setting_fingerprint),
            onItemClicked = onItemClicked,
            trailingIcons = {
                MegaSwitch(
                    modifier = Modifier.testTag(FINGERPRINT_ID_TILE_SWITCH),
                    checked = isChecked,
                    onCheckedChange = null,
                )
            }
        )
        MegaDivider(
            modifier = Modifier.testTag(FINGERPRINT_ID_TILE_DIVIDER),
            dividerType = DividerType.FullSize,
        )
    }
}

/**
 * Keep file names tile preview
 *
 * @param isChecked
 */
@CombinedThemePreviews
@Composable
private fun KeepFileNamesTilePreview(
    @PreviewParameter(BooleanProvider::class) isChecked: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        FingerprintIdTile(
            isChecked = isChecked,
            onItemClicked = {},
        )
    }
}


internal const val FINGERPRINT_ID_TILE = "fingerprint_id_tile:generic_two_line_list_item"
internal const val FINGERPRINT_ID_TILE_SWITCH = "fingerprint_id_tile:mega_switch"
internal const val FINGERPRINT_ID_TILE_DIVIDER = "fingerprint_id_tile:mega_divider"