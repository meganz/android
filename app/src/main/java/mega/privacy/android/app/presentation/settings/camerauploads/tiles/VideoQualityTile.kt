package mega.privacy.android.app.presentation.settings.camerauploads.tiles

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.model.VideoQualityUiItem
import mega.privacy.android.core.ui.controls.dividers.DividerType
import mega.privacy.android.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.core.ui.controls.lists.GenericTwoLineListItem
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * A [Composable] that shows the Video Quality of Videos being uploaded by Camera Uploads
 *
 * @param videoQualityUiItem Determines the Video Quality of Videos being uploaded by Camera Uploads
 * @param onItemClicked Lambda to execute when the Tile is clicked
 * @param modifier The [Modifier]
 */
@Composable
internal fun VideoQualityTile(
    videoQualityUiItem: VideoQualityUiItem,
    onItemClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        GenericTwoLineListItem(
            modifier = Modifier.testTag(VIDEO_QUALITY_TILE),
            title = stringResource(R.string.settings_video_upload_quality),
            subtitle = stringResource(videoQualityUiItem.textRes),
            onItemClicked = onItemClicked,
        )
        MegaDivider(
            modifier = Modifier.testTag(VIDEO_QUALITY_TILE_DIVIDER),
            dividerType = DividerType.FullSize,
        )
    }
}

/**
 * A Preview [Composable] for [VideoQualityTile]
 *
 * @param videoQualityUiItem Determines the Video Quality of Videos being uploaded by Camera Uploads
 */
@CombinedThemePreviews
@Composable
private fun VideoQualityTileProvider(
    @PreviewParameter(VideoQualityUiItemParameterProvider::class) videoQualityUiItem: VideoQualityUiItem,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        VideoQualityTile(
            videoQualityUiItem = videoQualityUiItem,
            onItemClicked = {},
        )
    }
}

private class VideoQualityUiItemParameterProvider : PreviewParameterProvider<VideoQualityUiItem> {
    override val values: Sequence<VideoQualityUiItem>
        get() = VideoQualityUiItem.entries.asSequence()
}

/**
 * Test Tags for the Video Quality Tile
 */
internal const val VIDEO_QUALITY_TILE = "video_quality_tile:generic_two_line_list_item"
internal const val VIDEO_QUALITY_TILE_DIVIDER = "video_quality_tile:mega_divider"