package mega.privacy.android.app.presentation.settings.camerauploads.tiles

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.buttons.MegaCheckbox
import mega.privacy.android.core.ui.controls.dividers.DividerType
import mega.privacy.android.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.core.ui.controls.lists.GenericTwoLineListItem
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * A [Composable] that displays a [MegaCheckbox] decide if the existing filenames should be used
 * when content is being uploaded by Camera Uploads
 *
 * @param isChecked true if the [MegaCheckbox] is checked
 * @param onCheckedChange Lambda to execute whenever the [MegaCheckbox] state has changed
 * @param modifier The [Modifier]
 */
@Composable
internal fun KeepFileNamesTile(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        GenericTwoLineListItem(
            modifier = Modifier.testTag(KEEP_FILE_NAMES_TILE),
            title = stringResource(R.string.settings_keep_file_names),
            trailingIcons = {
                MegaCheckbox(
                    modifier = Modifier.testTag(KEEP_FILE_NAMES_TILE_CHECKBOX),
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                    rounded = false,
                )
            }
        )
        MegaDivider(
            modifier = Modifier.testTag(KEEP_FILE_NAMES_TILE_DIVIDER),
            dividerType = DividerType.FullSize,
        )
    }
}

/**
 * A [Composable] Preview for [KeepFileNamesTile]
 *
 * @param isChecked [PreviewParameter] that controls the [MegaCheckbox] checked state
 */
@CombinedThemePreviews
@Composable
private fun KeepFileNamesTilePreview(
    @PreviewParameter(BooleanProvider::class) isChecked: Boolean,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        KeepFileNamesTile(
            isChecked = isChecked,
            onCheckedChange = {},
        )
    }
}

/**
 * Test Tag for the Keep File Names Tile
 */
internal const val KEEP_FILE_NAMES_TILE = "keep_file_names_tile:generic_two_line_list_item"
internal const val KEEP_FILE_NAMES_TILE_CHECKBOX = "keep_file_names_tile:mega_checkbox"
internal const val KEEP_FILE_NAMES_TILE_DIVIDER = "keep_file_names_tile:mega_divider"