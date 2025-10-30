package mega.privacy.android.feature.photos.presentation.albums.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.core.sharedcomponents.selectedBorder
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR

@Composable
internal fun AlbumPhotoItem(
    photo: Photo,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    isPreview: Boolean = false,
    isSensitive: Boolean = false,
) {
    // Todo add photo downloader when ready
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(null)
            .crossfade(true)
            .build(),
        contentDescription = null,
        placeholder = painterResource(id = iconPackR.drawable.ic_image_medium_solid),
        error = painterResource(id = iconPackR.drawable.ic_image_medium_solid),
        contentScale = ContentScale.Crop,
        modifier = modifier
            .width(width)
            .height(height)
            .aspectRatio(1f)
            .alpha(1f.takeIf { !isSensitive } ?: 0.5f)
            .blur(0.dp.takeIf { !isSensitive } ?: 16.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AlbumPhotoContainer(
    albumPhotoView: @Composable () -> Unit,
    photo: Photo,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (Photo) -> Unit = {},
    onLongPress: (Photo) -> Unit = {},
) {
    Box(
        modifier = modifier
            .selectedBorder(isSelected)
            .combinedClickable(
                onClick = { onClick(photo) },
                onLongClick = { onLongPress(photo) }
            )
    ) {

        albumPhotoView()

        if (photo.isFavourite) {
            BoxSurface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(RoundedCornerShape(2.dp)),
                surfaceColor = SurfaceColor.Inverse
            ) {
                MegaIcon(
                    modifier = Modifier.align(Alignment.Center),
                    painter = rememberVectorPainter(IconPack.Small.Thin.Solid.Heart),
                    tint = IconColor.OnColor
                )
            }
        }

        if (photo is Photo.Video) {
            val durationMapper = remember { DurationInSecondsTextMapper() }

            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
            ) {
                MegaText(
                    text = durationMapper(photo.fileTypeInfo.duration),
                    textColor = TextColor.OnColor,
                    style = AppTheme.typography.labelSmall,
                )
            }
        }

        if (isSelected) {
            BoxSurface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clickable {
                        onClick(photo)
                    }
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(RoundedCornerShape(2.dp)),
                surfaceColor = SurfaceColor.Inverse
            ) {
                MegaIcon(
                    modifier = Modifier.align(Alignment.Center),
                    painter = rememberVectorPainter(IconPack.Small.Thin.Outline.Check),
                    tint = IconColor.OnColor
                )
            }
        }
    }
}