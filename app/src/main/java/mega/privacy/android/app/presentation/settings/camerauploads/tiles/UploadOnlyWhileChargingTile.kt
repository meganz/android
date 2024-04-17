package mega.privacy.android.app.presentation.settings.camerauploads.tiles

import mega.privacy.android.shared.resources.R as SharedR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.core.ui.controls.dividers.DividerType
import mega.privacy.android.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.core.ui.controls.lists.GenericTwoLineListItem
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.legacy.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * A [Composable] that displays a [MegaSwitch] to enable or disable the charging requirement for
 * Camera Uploads to upload content
 *
 * @param isChecked true if the [MegaSwitch] is checked
 * @param onCheckedChange Lambda to execute when the [MegaSwitch] checked state has changed
 */
@Composable
internal fun UploadOnlyWhileChargingTile(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        GenericTwoLineListItem(
            modifier = Modifier.testTag(UPLOAD_ONLY_WHILE_CHARGING_TILE),
            title = stringResource(SharedR.string.settings_camera_uploads_upload_only_while_charging_option_name),
            trailingIcons = {
                MegaSwitch(
                    modifier = Modifier.testTag(UPLOAD_ONLY_WHILE_CHARGING_TILE_SWITCH),
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                )
            }
        )
        MegaDivider(
            modifier = Modifier.testTag(UPLOAD_ONLY_WHILE_CHARGING_TILE_DIVIDER),
            dividerType = DividerType.FullSize,
        )
    }
}

/**
 * A [Composable] Preview for [UploadOnlyWhileChargingTile]
 *
 * @param isChecked [PreviewParameter] that controls the [MegaSwitch] checked state
 */
@CombinedThemePreviews
@Composable
private fun UploadOnlyWhileChargingTilePreview(
    @PreviewParameter(BooleanProvider::class) isChecked: Boolean,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        UploadOnlyWhileChargingTile(
            isChecked = isChecked,
            onCheckedChange = {},
        )
    }
}

/**
 * Test Tags for the Upload Only While Charging Tile
 */
internal const val UPLOAD_ONLY_WHILE_CHARGING_TILE =
    "upload_only_while_charging_tile:generic_two_line_list_item"
internal const val UPLOAD_ONLY_WHILE_CHARGING_TILE_SWITCH =
    "upload_only_while_charging_tile:mega_switch"
internal const val UPLOAD_ONLY_WHILE_CHARGING_TILE_DIVIDER =
    "upload_only_while_charging_tile:mega_divider"