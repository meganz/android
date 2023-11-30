package mega.privacy.android.app.presentation.videosection

import mega.privacy.android.core.R as CoreUiR
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
import mega.privacy.android.core.theme.tokens.MegaAppTheme
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun VideoItemView(
    @DrawableRes icon: Int,
    name: String,
    fileSize: String?,
    duration: String?,
    isFavourite: Boolean,
    onClick: () -> Unit,
    thumbnailData: Any?,
    modifier: Modifier = Modifier,
    showMenuButton: Boolean = true,
    nodeAvailableOffline: Boolean = false,
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
        VideoThumbnailView(
            icon = icon,
            modifier = modifier,
            thumbnailData = thumbnailData,
            duration = duration,
            isFavourite = isFavourite
        )

        VideoInfoView(
            name = name,
            fileSize = fileSize,
            showMenuButton = showMenuButton,
            nodeAvailableOffline = nodeAvailableOffline,
            onMenuClick = onMenuClick,
            modifier = modifier
        )
    }
}

@Composable
internal fun VideoThumbnailView(
    @DrawableRes icon: Int,
    modifier: Modifier,
    thumbnailData: Any?,
    duration: String?,
    isFavourite: Boolean,
) {
    Box(contentAlignment = Alignment.TopStart) {
        val thumbnailModifier = modifier
            .width(126.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(colorResource(id = R.color.white_045_grey_045))

        thumbnailData?.let {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnailData)
                    .crossfade(true)
                    .build(),
                contentDescription = "Video",
                placeholder = painterResource(id = icon),
                error = painterResource(id = icon),
                contentScale = ContentScale.FillBounds,
                modifier = thumbnailModifier
            )
        } ?: run {
            Image(
                modifier = thumbnailModifier,
                painter = painterResource(id = icon),
                contentDescription = "Video",
            )
        }
        duration?.let {
            Text(
                modifier = modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 5.dp, end = 5.dp)
                    .height(16.dp),
                text = it,
                style = MaterialTheme.typography.caption,
                color = Color.White
            )
        }
        Image(
            painter = painterResource(id = R.drawable.ic_play_circle),
            contentDescription = "play",
            modifier = modifier
                .size(16.dp)
                .align(Alignment.Center)
        )
        if (isFavourite) {
            Image(
                painter = painterResource(id = R.drawable.ic_favourite_white),
                contentDescription = "favourite",
                modifier = modifier
                    .padding(top = 5.dp, end = 5.dp)
                    .size(12.dp)
                    .align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
internal fun VideoInfoView(
    name: String,
    fileSize: String?,
    showMenuButton: Boolean,
    nodeAvailableOffline: Boolean,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ConstraintLayout(
        modifier = modifier
            .padding(start = 25.dp)
            .height(80.dp),
    ) {
        val (videoName, threeDots, fileSizeText, offlineIcon) = createRefs()
        Text(
            text = name,
            modifier = modifier
                .padding(bottom = 5.dp)
                .constrainAs(videoName) {
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
            painter = painterResource(id = CoreUiR.drawable.ic_dots_vertical_grey),
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
            modifier = modifier.constrainAs(fileSizeText) {
                start.linkTo(videoName.start)
                top.linkTo(offlineIcon.top)
                bottom.linkTo(offlineIcon.bottom)
                visibility = if (fileSize.isNullOrEmpty()) Visibility.Gone else Visibility.Visible
            },
            text = fileSize ?: "",
            maxLines = 1,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.textColorSecondary,
        )

        Image(
            modifier = modifier
                .padding(start = 10.dp)
                .constrainAs(offlineIcon) {
                    start.linkTo(fileSizeText.end)
                    top.linkTo(videoName.bottom)
                    visibility =
                        if (nodeAvailableOffline) Visibility.Visible else Visibility.Invisible
                },
            colorFilter = ColorFilter.tint(
                MaterialTheme.colors.textColorSecondary
            ),
            painter = painterResource(id = CoreUiR.drawable.ic_offline_indicator),
            contentDescription = "Available Offline"
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoItemViewWithFavouritePreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        VideoItemView(
            icon = mega.privacy.android.icon.pack.R.drawable.ic_video_list,
            name = "testing_video_file_name.mp4",
            fileSize = "1.3MB",
            duration = "04:00",
            isFavourite = true,
            onClick = {},
            thumbnailData = null,
            nodeAvailableOffline = true,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoItemViewWithoutFavouritePreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        VideoItemView(
            icon = mega.privacy.android.icon.pack.R.drawable.ic_video_list,
            name = "name.mp4",
            fileSize = "1.3MB",
            duration = "04:00",
            isFavourite = false,
            onClick = {},
            thumbnailData = null
        )
    }
}