package mega.privacy.android.app.presentation.settings.camerauploads.tiles

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.lists.GenericTwoLineListItem
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * A [Composable] that displays a [MegaSwitch] to enable or disable Location Tags when uploading
 * Photos using Camera Uploads
 *
 * @param isChecked true if the [MegaSwitch] is checked
 * @param onCheckedChange Lambda to execute when the [MegaSwitch] checked state has changed
 * @param modifier The [Modifier]
 */
@Composable
internal fun IncludeLocationTagsTile(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        GenericTwoLineListItem(
            modifier = Modifier.testTag(INCLUDE_LOCATION_TAGS_TILE),
            title = stringResource(R.string.settings_camera_upload_include_gps),
            subtitle = stringResource(R.string.settings_camera_upload_include_gps_helper_label),
            showEntireSubtitle = true,
            trailingIcons = {
                MegaSwitch(
                    modifier = Modifier.testTag(INCLUDE_LOCATION_TAGS_TILE_SWITCH),
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                )
            },
        )
        MegaDivider(
            modifier = Modifier.testTag(INCLUDE_LOCATION_TAGS_TILE_DIVIDER),
            dividerType = DividerType.FullSize,
        )
    }
}

/**
 * A [Composable] Preview for [IncludeLocationTagsTile]
 *
 * @param isChecked [PreviewParameter] that controls the [MegaSwitch] checked state
 */
@CombinedThemePreviews
@Composable
private fun IncludeLocationTagsTilePreview(
    @PreviewParameter(BooleanProvider::class) isChecked: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        IncludeLocationTagsTile(
            isChecked = isChecked,
            onCheckedChange = {},
        )
    }
}

/**
 * Test Tags for the Include Location Tags Tile
 */
internal const val INCLUDE_LOCATION_TAGS_TILE =
    "include_location_tags_tile:generic_two_line_list_item"
internal const val INCLUDE_LOCATION_TAGS_TILE_SWITCH = "include_location_tags_tile:mega_switch"
internal const val INCLUDE_LOCATION_TAGS_TILE_DIVIDER = "include_location_tags_tile:mega_divider"