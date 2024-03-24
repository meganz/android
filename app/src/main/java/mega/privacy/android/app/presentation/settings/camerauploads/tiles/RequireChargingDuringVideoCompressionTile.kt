package mega.privacy.android.app.presentation.settings.camerauploads.tiles

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dividers.DividerType
import mega.privacy.android.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.core.ui.controls.lists.GenericTwoLineListItem
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.legacy.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * A [Composable] that displays a [MegaSwitch] to require or not require the User to charge his/her
 * Device when the total size of Videos to be compressed exceeds a specific size
 * threshold set by [maximumNonChargingVideoCompressionSize]
 *
 * @param maximumNonChargingVideoCompressionSize The maximum aggregated Video Size that can be
 * compressed without having to charge the Device
 * @param isChecked true if the [MegaSwitch] is checked
 * @param onCheckedChange Lambda to execute when the [MegaSwitch] checked state has changed
 * @param modifier The [Modifier]
 */
@Composable
internal fun RequireChargingDuringVideoCompressionTile(
    maximumNonChargingVideoCompressionSize: Int,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        GenericTwoLineListItem(
            modifier = Modifier.testTag(REQUIRE_CHARGING_DURING_VIDEO_COMPRESSION_TILE),
            title = stringResource(R.string.settings_camera_upload_require_plug_in),
            subtitle = stringResource(
                R.string.settings_camera_upload_charging_helper_label,
                stringResource(
                    R.string.label_file_size_mega_byte,
                    maximumNonChargingVideoCompressionSize.toString(),
                )
            ),
            showEntireSubtitle = true,
            trailingIcons = {
                MegaSwitch(
                    modifier = Modifier.testTag(
                        REQUIRE_CHARGING_DURING_VIDEO_COMPRESSION_TILE_SWITCH
                    ),
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                )
            }
        )
        MegaDivider(
            modifier = Modifier.testTag(REQUIRE_CHARGING_DURING_VIDEO_COMPRESSION_TILE_DIVIDER),
            dividerType = DividerType.FullSize,
        )
    }
}

/**
 * A [Composable] Preview for [RequireChargingDuringVideoCompressionTile]
 *
 * @param isChecked [PreviewParameter] that controls the [MegaSwitch] checked state
 */
@CombinedThemePreviews
@Composable
private fun RequireChargingDuringVideoCompressionTilePreview(
    @PreviewParameter(BooleanProvider::class) isChecked: Boolean,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        RequireChargingDuringVideoCompressionTile(
            maximumNonChargingVideoCompressionSize = 500,
            isChecked = isChecked,
            onCheckedChange = {},
        )
    }
}

/**
 * Test Tags for the Require Charging During Video Compression Tile
 */
internal const val REQUIRE_CHARGING_DURING_VIDEO_COMPRESSION_TILE =
    "require_charging_during_video_compression_tile:generic_two_line_list_item"
internal const val REQUIRE_CHARGING_DURING_VIDEO_COMPRESSION_TILE_SWITCH =
    "require_charging_during_video_compression_tile:mega_switch"
internal const val REQUIRE_CHARGING_DURING_VIDEO_COMPRESSION_TILE_DIVIDER =
    "require_charging_during_video_compression_tile:mega_divider"