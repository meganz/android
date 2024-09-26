package mega.privacy.android.app.presentation.videosection.view.playlist

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_050_grey_800
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorSecondary

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun VideoPlaylistItemView(
    @DrawableRes emptyPlaylistIcon: Int,
    @DrawableRes noThumbnailIcon: Int,
    title: String,
    numberOfVideos: Int,
    totalDuration: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    thumbnailList: List<Any?>?,
    modifier: Modifier = Modifier,
    showMenuButton: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onMenuClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .fillMaxWidth()
            .height(87.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VideoPlaylistThumbnailView(
            emptyPlaylistIcon = emptyPlaylistIcon,
            noThumbnailIcon = noThumbnailIcon,
            modifier = Modifier,
            thumbnailList = thumbnailList
        )

        VideoPlaylistInfoView(
            title = title,
            numberOfVideos = numberOfVideos,
            totalDuration = totalDuration,
            showMenuButton = showMenuButton,
            onMenuClick = onMenuClick,
            modifier = Modifier.weight(1f),
            isSelected = isSelected
        )

        Image(
            painter = painterResource(
                id = if (isSelected)
                    R.drawable.ic_select_thumbnail
                else
                    mega.privacy.android.core.R.drawable.ic_dots_vertical_grey
            ),
            contentDescription = "3 dots",
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .clickable {
                    if (!isSelected) {
                        onMenuClick()
                    }
                },
            alignment = Alignment.CenterEnd,
        )
    }
}

@Composable
internal fun VideoPlaylistThumbnailView(
    @DrawableRes emptyPlaylistIcon: Int,
    @DrawableRes noThumbnailIcon: Int,
    modifier: Modifier,
    thumbnailList: List<Any?>?,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopStart
    ) {
        val thumbnailModifier = Modifier
            .width(126.dp)
            .aspectRatio(1.77f)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colors.grey_050_grey_800)

        ThumbnailListView(
            emptyPlaylistIcon = emptyPlaylistIcon,
            noThumbnailIcon = noThumbnailIcon,
            modifier = thumbnailModifier,
            thumbnailList = thumbnailList,
        )

        Image(
            painter = painterResource(id = R.drawable.ic_video_stack),
            contentDescription = "video stack",
            modifier = Modifier
                .padding(top = 5.dp, end = 5.dp)
                .size(24.dp)
                .align(Alignment.TopEnd)
        )
    }
}

@Composable
internal fun ThumbnailListView(
    @DrawableRes emptyPlaylistIcon: Int,
    @DrawableRes noThumbnailIcon: Int,
    modifier: Modifier,
    thumbnailList: List<Any?>?,
) {
    when {
        thumbnailList == null ->
            PlaylistEmptyView(icon = emptyPlaylistIcon, modifier = modifier)

        thumbnailList.isEmpty() ->
            PlaylistEmptyView(icon = noThumbnailIcon, modifier = modifier)

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
    modifier: Modifier,
) {
    Image(
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
                            )
                        } ?: Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f)
                        )
                        if (rowIndex == 0) {
                            Divider(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp)
                            )
                        }
                    }
                }
                if (columnIndex == 0) {
                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                    )
                }
            }
    }
}

@Composable
internal fun VideoPlaylistInfoView(
    title: String,
    numberOfVideos: Int,
    totalDuration: String?,
    showMenuButton: Boolean,
    isSelected: Boolean,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(vertical = 5.dp, horizontal = 10.dp)
            .fillMaxHeight()
    ) {
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.subtitle1,
            color = colorResource(id = R.color.dark_grey_white),
            textAlign = TextAlign.Start,
            maxLines = 2
        )

        Text(
            modifier = Modifier.padding(vertical = 5.dp),
            text = if (numberOfVideos != 0) {
                val numberOfVideosText = if (numberOfVideos == 1) {
                    "1 Video"
                } else {
                    "$numberOfVideos Videos"
                }
                "$numberOfVideosText • $totalDuration"
            } else {
                "Empty"
            },
            maxLines = 1,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.textColorSecondary,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoPlaylistItemViewWithoutVideosPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistItemView(
            emptyPlaylistIcon = iconPackR.drawable.ic_video_playlist_default_thumbnail,
            noThumbnailIcon = iconPackR.drawable.ic_video_playlist_no_thumbnail,
            title = "New Playlist",
            thumbnailList = null,
            modifier = Modifier,
            numberOfVideos = 0,
            totalDuration = null,
            onClick = {},
            isSelected = false
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoPlaylistItemViewWith1VideoPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistItemView(
            emptyPlaylistIcon = iconPackR.drawable.ic_video_playlist_default_thumbnail,
            noThumbnailIcon = iconPackR.drawable.ic_video_playlist_no_thumbnail,
            title = "1 Video Playlist",
            thumbnailList = null,
            modifier = Modifier,
            numberOfVideos = 1,
            totalDuration = "00:05:55",
            onClick = {},
            isSelected = false
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoPlaylistItemViewMultipleVideosPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistItemView(
            emptyPlaylistIcon = iconPackR.drawable.ic_video_playlist_default_thumbnail,
            noThumbnailIcon = iconPackR.drawable.ic_video_playlist_no_thumbnail,
            title = "Multiple Video Playlist",
            thumbnailList = null,
            modifier = Modifier,
            numberOfVideos = 3,
            totalDuration = "1:00:55",
            onClick = {},
            isSelected = true
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ThumbnailListViewWith4ThumbnailsPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
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
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistThumbnailView(
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
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistThumbnailView(
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
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistThumbnailView(
            emptyPlaylistIcon = iconPackR.drawable.ic_video_playlist_default_thumbnail,
            noThumbnailIcon = iconPackR.drawable.ic_video_playlist_no_thumbnail,
            modifier = Modifier,
            thumbnailList = null,
        )
    }
}