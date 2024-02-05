package mega.privacy.android.app.presentation.videosection

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
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Visibility
import coil.compose.AsyncImage
import coil.request.ImageRequest
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.grey_050_grey_800
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.shared.theme.MegaAppTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun VideoPlaylistItemView(
    @DrawableRes icon: Int,
    title: String,
    numberOfVideos: Int,
    totalDuration: String?,
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
            icon = icon,
            modifier = modifier,
            thumbnailList = thumbnailList
        )

        VideoPlaylistInfoView(
            title = title,
            numberOfVideos = numberOfVideos,
            totalDuration = totalDuration,
            showMenuButton = showMenuButton,
            onMenuClick = onMenuClick,
            modifier = modifier
        )
    }
}

@Composable
internal fun VideoPlaylistThumbnailView(
    @DrawableRes icon: Int,
    modifier: Modifier,
    thumbnailList: List<Any?>?,
) {
    Box(contentAlignment = Alignment.TopStart) {
        val thumbnailModifier = modifier
            .width(126.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colors.grey_050_grey_800)

        ThumbnailListView(
            icon = icon,
            modifier = thumbnailModifier,
            thumbnailList = thumbnailList,
        )

        Image(
            painter = painterResource(id = R.drawable.ic_video_stack),
            contentDescription = "video stack",
            modifier = modifier
                .padding(top = 5.dp, end = 5.dp)
                .size(24.dp)
                .align(Alignment.TopEnd)
        )
    }
}

@Composable
internal fun ThumbnailListView(
    @DrawableRes icon: Int,
    modifier: Modifier,
    thumbnailList: List<Any?>?,
) {
    when {
        thumbnailList == null ->
            PlaylistEmptyView(icon = icon, modifier = modifier)

        thumbnailList.size < 4 ->
            thumbnailList.firstNotNullOfOrNull { thumbnailData ->
                thumbnailData?.let {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(it)
                            .crossfade(true)
                            .build(),
                        contentDescription = "VideoPlaylist",
                        placeholder = painterResource(id = icon),
                        error = painterResource(id = icon),
                        contentScale = ContentScale.FillBounds,
                        modifier = modifier
                    )
                } ?: PlaylistEmptyView(icon = icon, modifier = modifier)
            }

        else ->
            MultipleThumbnailsView(
                icon = icon,
                modifier = modifier,
                thumbnailList = thumbnailList,
            )
    }
}

@Composable
internal fun PlaylistEmptyView(
    @DrawableRes icon: Int,
    modifier: Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Image(
            modifier = Modifier.size(24.dp),
            painter = painterResource(id = icon),
            contentDescription = "VideoPlaylist",
        )
    }
}

@Composable
internal fun MultipleThumbnailsView(
    @DrawableRes icon: Int,
    modifier: Modifier,
    thumbnailList: List<Any?>,
) {
    Row(
        modifier = modifier
    ) {
        (thumbnailList.indices).chunked(2)
            .mapIndexed { rowIndex, indices ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    indices.mapIndexed { columnIndex, itemIndex ->
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(thumbnailList[itemIndex])
                                .crossfade(true)
                                .build(),
                            contentDescription = "Video",
                            placeholder = painterResource(id = icon),
                            error = painterResource(id = icon),
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f)
                        )
                        if (columnIndex == 0) {
                            Divider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                            )
                        }
                    }
                }
                if (rowIndex == 0) {
                    Divider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
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
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ConstraintLayout(
        modifier = modifier
            .padding(start = 25.dp)
            .height(80.dp),
    ) {
        val (videoPlaylistName, threeDots, playlistInfoText) = createRefs()
        Text(
            text = title,
            modifier = modifier
                .padding(bottom = 5.dp)
                .constrainAs(videoPlaylistName) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    end.linkTo(threeDots.start, margin = 10.dp)
                }
                .fillMaxWidth(),
            style = MaterialTheme.typography.subtitle1,
            color = colorResource(id = R.color.dark_grey_white),
            textAlign = TextAlign.Start,
            maxLines = 2
        )
        Image(
            painter = painterResource(id = mega.privacy.android.core.R.drawable.ic_dots_vertical_grey),
            contentDescription = "3 dots",
            modifier = modifier
                .constrainAs(threeDots) {
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    visibility = if (showMenuButton) Visibility.Visible else Visibility.Gone
                }
                .clickable { onMenuClick() }
        )

        Text(
            modifier = modifier.constrainAs(playlistInfoText) {
                start.linkTo(videoPlaylistName.start)
                top.linkTo(videoPlaylistName.bottom)
            },
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
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistItemView(
            icon = R.drawable.ic_playlist_item_empty,
            title = "New Playlist",
            thumbnailList = null,
            modifier = Modifier,
            numberOfVideos = 0,
            totalDuration = null,
            onClick = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoPlaylistItemViewWith1VideoPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistItemView(
            icon = R.drawable.ic_playlist_item_empty,
            title = "1 Video Playlist",
            thumbnailList = null,
            modifier = Modifier,
            numberOfVideos = 1,
            totalDuration = "00:05:55",
            onClick = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoPlaylistItemViewMultipleVideosPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistItemView(
            icon = R.drawable.ic_playlist_item_empty,
            title = "Multiple Video Playlist",
            thumbnailList = null,
            modifier = Modifier,
            numberOfVideos = 3,
            totalDuration = "1:00:55",
            onClick = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ThumbnailListViewWith4ThumbnailsPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistThumbnailView(
            icon = R.drawable.ic_playlist_item_empty,
            modifier = Modifier,
            thumbnailList = listOf(null, null, null, null),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ThumbnailListViewWith1NullThumbnailPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistThumbnailView(
            icon = R.drawable.ic_playlist_item_empty,
            modifier = Modifier,
            thumbnailList = listOf(null),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ThumbnailListViewWith1ThumbnailsPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistThumbnailView(
            icon = R.drawable.ic_playlist_item_empty,
            modifier = Modifier,
            thumbnailList = listOf(R.drawable.ic_playlist_item_empty),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ThumbnailListViewWhenThumbnailListIsNullPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistThumbnailView(
            icon = R.drawable.ic_playlist_item_empty,
            modifier = Modifier,
            thumbnailList = null,
        )
    }
}