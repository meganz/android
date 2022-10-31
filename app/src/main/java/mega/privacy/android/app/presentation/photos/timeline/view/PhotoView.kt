package mega.privacy.android.app.presentation.photos.timeline.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.model.PhotoDownload
import mega.privacy.android.app.presentation.photos.model.ZoomLevel
import mega.privacy.android.app.presentation.photos.view.isDownloadPreview
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.domain.entity.photos.Photo

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoView(
    photo: Photo,
    isSelected: Boolean,
    currentZoomLevel: ZoomLevel,
    onClick: (Photo) -> Unit,
    onLongPress: (Photo) -> Unit,
    downloadPhoto: PhotoDownload,
) {
    var modifier = remember {
        when (currentZoomLevel) {
            ZoomLevel.Grid_1 -> Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
            ZoomLevel.Grid_3 -> Modifier
                .fillMaxSize()
                .padding(all = 1.5.dp)
            ZoomLevel.Grid_5 -> Modifier
                .fillMaxSize()
                .padding(all = 1.dp)
        }
    }

    if (isSelected) {
        modifier = modifier
            .border(BorderStroke(
                width = 2.dp,
                color = colorResource(id = R.color.teal_300)),
                shape = RoundedCornerShape(4.dp)
            )
            .clip(RoundedCornerShape(4.dp))
    }

    Box(modifier = modifier) {
        PhotoCoverView(
            photo = photo,
            currentZoomLevel = currentZoomLevel,
            downloadPhoto = downloadPhoto,
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    onClick = { onClick(photo) },
                    onLongClick = { onLongPress(photo) }
                )
        )
        if (isSelected) {
            SelectedIconView(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
            )
        }
        if (
            photo.isFavourite && currentZoomLevel == ZoomLevel.Grid_1
            || photo.isFavourite && currentZoomLevel == ZoomLevel.Grid_3
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_favourite_white),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                tint = Color.Unspecified

            )
        }
    }

}

@Composable
fun SelectedIconView(
    modifier: Modifier,
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_select_folder),
        contentDescription = null,
        modifier = modifier,
        tint = Color.Unspecified
    )
}

@Composable
fun PhotoCoverView(
    photo: Photo,
    currentZoomLevel: ZoomLevel,
    modifier: Modifier,
    downloadPhoto: PhotoDownload,
) {
    val configuration = LocalConfiguration.current
    val isDownloadPreview = isDownloadPreview(configuration, currentZoomLevel)

    Box(
        modifier = modifier
    ) {
        when (photo) {
            is Photo.Image -> {
                PhotoImageView(
                    photo = photo,
                    isPreview = isDownloadPreview,
                    downloadPhoto = downloadPhoto
                )
                if (photo.isFavourite) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_overlay),
                        contentScale = ContentScale.FillBounds,
                        contentDescription = null,
                        modifier = Modifier
                            .matchParentSize()
                    )
                }
            }
            is Photo.Video -> {
                PhotoImageView(
                    photo = photo,
                    isPreview = isDownloadPreview,
                    downloadPhoto = downloadPhoto
                )
                Spacer(modifier = Modifier
                    .matchParentSize()
                    .background(
                        color = colorResource(id = R.color.grey_alpha_032)
                    )
                )

                Text(
                    text = TimeUtils.getVideoDuration(photo.duration),
                    color = colorResource(id = R.color.white),
                    modifier = Modifier
                        .wrapContentSize()
                        .align(Alignment.BottomEnd)
                        .padding(
                            if (currentZoomLevel == ZoomLevel.Grid_5) {
                                4.dp
                            } else {
                                16.dp
                            }
                        ),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

            }
        }
    }
}


@Composable
fun PhotoImageView(
    photo: Photo,
    isPreview: Boolean,
    downloadPhoto: PhotoDownload,
) {
    val imageState = produceState<String?>(initialValue = null) {
        downloadPhoto(isPreview, photo) { downloadSuccess ->
            if (downloadSuccess) {
                value = if (isPreview) {
                    photo.previewFilePath
                } else {
                    photo.thumbnailFilePath
                }
            }
        }
    }

    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageState.value)
            .crossfade(true)
            .build(),
        contentDescription = null,
        placeholder = painterResource(id = R.drawable.ic_image_thumbnail),
        error = painterResource(id = R.drawable.ic_image_thumbnail),
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    )
}



