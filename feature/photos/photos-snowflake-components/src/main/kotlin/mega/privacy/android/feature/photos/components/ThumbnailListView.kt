package mega.privacy.android.feature.photos.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.R as iconPackR

@Composable
internal fun ThumbnailListView(
    @DrawableRes emptyPlaylistIcon: Int,
    @DrawableRes noThumbnailIcon: Int,
    modifier: Modifier,
    thumbnailList: List<Any?>?,
) {
    when {
        thumbnailList == null ->
            PlaylistEmptyView(
                icon = emptyPlaylistIcon,
                modifier = modifier.testTag(THUMBNAIL_LIST_VIEW_EMPTY_VIEW_TEST_TAG)
            )

        thumbnailList.isEmpty() ->
            PlaylistEmptyView(
                icon = noThumbnailIcon,
                modifier = modifier.testTag(THUMBNAIL_LIST_VIEW_NO_THUMBNAIL_TEST_TAG)
            )

        else ->
            MultipleThumbnailsView(
                icon = emptyPlaylistIcon,
                modifier = modifier,
                thumbnails = thumbnailList,
            )
    }
}

@Composable
internal fun PlaylistEmptyView(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
) {
    Image(
        modifier = modifier,
        painter = painterResource(id = icon),
        contentDescription = "VideoPlaylist",
    )
}

@Composable
internal fun MultipleThumbnailsView(
    @DrawableRes icon: Int,
    modifier: Modifier,
    thumbnails: List<Any?>,
) {
    val thumbnailList = if (thumbnails.size < 4) {
        thumbnails + List(4 - thumbnails.size) { null }
    } else {
        thumbnails
    }
    Column(
        modifier = modifier
    ) {
        (thumbnailList.indices).chunked(2)
            .mapIndexed { columnIndex, indices ->
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                ) {
                    indices.mapIndexed { rowIndex, itemIndex ->
                        thumbnailList[itemIndex]?.let {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(it)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Video",
                                placeholder = painterResource(id = icon),
                                error = painterResource(id = icon),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .testTag("$THUMBNAIL_LIST_VIEW_THUMBNAIL_TEST_TAG$itemIndex")
                            )
                        } ?: Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f)
                        )
                        if (rowIndex == 0) {
                            VerticalDivider(
                                thickness = 1.dp,
                                color = DSTokens.colors.border.strong
                            )
                        }
                    }
                }
                if (columnIndex == 0) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = DSTokens.colors.border.strong
                    )
                }
            }
    }
}

@CombinedThemePreviews
@Composable
private fun ThumbnailListViewWith4ThumbnailsPreview() {
    AndroidThemeForPreviews {
        VideoPlaylistThumbnailView(
            emptyPlaylistIcon = iconPackR.drawable.ic_video_playlist_default_thumbnail,
            noThumbnailIcon = iconPackR.drawable.ic_video_playlist_no_thumbnail,
            modifier = Modifier,
            thumbnailList = listOf(
                iconPackR.drawable.ic_playlist_item_empty,
                iconPackR.drawable.ic_playlist_item_empty,
            )
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ThumbnailListViewWith1NullThumbnailPreview() {
    AndroidThemeForPreviews {
        ThumbnailListView(
            emptyPlaylistIcon = iconPackR.drawable.ic_video_playlist_default_thumbnail,
            noThumbnailIcon = iconPackR.drawable.ic_video_playlist_no_thumbnail,
            modifier = Modifier,
            thumbnailList = emptyList(),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ThumbnailListViewWith1ThumbnailsPreview() {
    AndroidThemeForPreviews {
        ThumbnailListView(
            emptyPlaylistIcon = iconPackR.drawable.ic_video_playlist_default_thumbnail,
            noThumbnailIcon = iconPackR.drawable.ic_video_playlist_no_thumbnail,
            modifier = Modifier,
            thumbnailList = listOf(iconPackR.drawable.ic_playlist_item_empty),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ThumbnailListViewWhenThumbnailListIsNullPreview() {
    AndroidThemeForPreviews {
        ThumbnailListView(
            emptyPlaylistIcon = iconPackR.drawable.ic_video_playlist_default_thumbnail,
            noThumbnailIcon = iconPackR.drawable.ic_video_playlist_no_thumbnail,
            modifier = Modifier,
            thumbnailList = null,
        )
    }
}

/**
 * Test tag for empty thumbnail
 */
const val THUMBNAIL_LIST_VIEW_EMPTY_VIEW_TEST_TAG = "thumbnail_list_view_empty_view_test_tag"

/**
 * Test tag for no thumbnail
 */
const val THUMBNAIL_LIST_VIEW_NO_THUMBNAIL_TEST_TAG = "thumbnail_list_view_no_thumbnail_test_tag"

/**
 * Test tag for item thumbnail
 */
const val THUMBNAIL_LIST_VIEW_THUMBNAIL_TEST_TAG = "thumbnail_list_view_thumbnail_test_tag"
