package mega.privacy.android.app.presentation.settings.camerauploads.tiles

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dividers.DividerType
import mega.privacy.android.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.core.ui.controls.lists.GenericTwoLineListItem
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * A [Composable] that displays the maximum aggregate Video Size that must be exceeded in order to
 * require Device charging when compressing Videos
 *
 * @param maximumNonChargingVideoCompressionSize The maximum aggregated Video Size that can be
 * compressed without having to charge the Device
 * @param onItemClicked Lambda to execute when the Tile is clicked
 * @param modifier The [Modifier]
 */
@Composable
internal fun VideoCompressionTile(
    maximumNonChargingVideoCompressionSize: Int,
    onItemClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        GenericTwoLineListItem(
            modifier = Modifier.testTag(VIDEO_COMPRESSION_TILE),
            title = stringResource(R.string.settings_video_compression_queue_size_title),
            subtitle = stringResource(
                R.string.label_file_size_mega_byte,
                maximumNonChargingVideoCompressionSize.toString(),
            ),
            onItemClicked = onItemClicked,
        )
        MegaDivider(
            modifier = Modifier.testTag(VIDEO_COMPRESSION_TILE_DIVIDER),
            dividerType = DividerType.FullSize,
        )
    }
}

/**
 * A Preview [Composable] for [VideoCompressionTile]
 */
@CombinedThemePreviews
@Composable
private fun VideoCompressionTilePreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        VideoCompressionTile(
            maximumNonChargingVideoCompressionSize = 500,
            onItemClicked = {},
        )
    }
}

/**
 * Test Tags for the Video Compression Tile
 */
internal const val VIDEO_COMPRESSION_TILE = "video_compression_tile:generic_two_line_list_item"
internal const val VIDEO_COMPRESSION_TILE_DIVIDER = "video_compression_tile:mega_divider"