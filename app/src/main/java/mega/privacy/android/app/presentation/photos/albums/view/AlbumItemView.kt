package mega.privacy.android.app.presentation.photos.albums.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.model.PhotoDownload
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.presentation.theme.grey_alpha_032
import mega.privacy.android.presentation.theme.white

@Composable
internal fun PhotosBig2SmallItems(
    size: Dp, photos: List<Photo>,
    photoDownload: PhotoDownload,
    onClick: (Photo) -> Unit = {},
    onLongPress: (Photo) -> Unit = {},
    selectedPhotoIds: MutableSet<Long>,
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            AlbumPhotoContainer(
                onClick = onClick,
                onLongPress = onLongPress,
                albumPhotoView = {
                    AlbumPhotoView(
                        size = size * 2,
                        photo = photos[0],
                        photoDownload = photoDownload,
                    )
                },
                photo = photos[0],
                isSelected = photos[0].id in selectedPhotoIds
            )
            if (photos.size >= 2) {
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    AlbumPhotoContainer(
                        onClick = onClick,
                        onLongPress = onLongPress,
                        albumPhotoView = {
                            AlbumPhotoView(
                                size = size,
                                photo = photos[1],
                                photoDownload = photoDownload,
                            )
                        },
                        photo = photos[1],
                        isSelected = photos[1].id in selectedPhotoIds
                    )
                    if (photos.size == 3) {
                        AlbumPhotoContainer(
                            onClick = onClick,
                            onLongPress = onLongPress,
                            albumPhotoView = {
                                AlbumPhotoView(
                                    size = size,
                                    photo = photos[2],
                                    photoDownload = photoDownload,
                                )
                            },
                            photo = photos[2],
                            isSelected = photos[2].id in selectedPhotoIds
                        )
                    }
                }
            }
        }
        Spacer(
            modifier = Modifier.height(1.dp)
        )
    }
}

@Composable
internal fun Photos3SmallItems(
    size: Dp,
    photos: List<Photo>,
    downloadPhoto: PhotoDownload,
    onClick: (Photo) -> Unit = {},
    onLongPress: (Photo) -> Unit = {},
    selectedPhotoIds: MutableSet<Long>,
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            AlbumPhotoContainer(
                onClick = onClick,
                onLongPress = onLongPress,
                albumPhotoView = {
                    AlbumPhotoView(
                        size = size,
                        photo = photos[0],
                        photoDownload = downloadPhoto,
                    )
                },
                photo = photos[0],
                isSelected = photos[0].id in selectedPhotoIds
            )
            if (photos.size >= 2) {
                AlbumPhotoContainer(
                    onClick = onClick,
                    onLongPress = onLongPress,
                    albumPhotoView = {
                        AlbumPhotoView(
                            size = size,
                            photo = photos[1],
                            photoDownload = downloadPhoto,
                        )
                    },
                    photo = photos[1],
                    isSelected = photos[1].id in selectedPhotoIds
                )
                if (photos.size == 2) {
                    Spacer(modifier = Modifier.size(size))
                }
            }
            if (photos.size == 3) {
                AlbumPhotoContainer(
                    onClick = onClick,
                    onLongPress = onLongPress,
                    albumPhotoView = {
                        AlbumPhotoView(
                            size = size,
                            photo = photos[2],
                            photoDownload = downloadPhoto,
                        )
                    },
                    photo = photos[2],
                    isSelected = photos[2].id in selectedPhotoIds
                )
            }
        }
        Spacer(
            modifier = Modifier.height(1.dp)
        )
    }
}

@Composable
internal fun Photos2SmallBigItems(
    size: Dp, photos: List<Photo>,
    downloadPhoto: PhotoDownload,
    onClick: (Photo) -> Unit = {},
    onLongPress: (Photo) -> Unit = {},
    selectedPhotoIds: MutableSet<Long>,
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                AlbumPhotoContainer(
                    onClick = onClick,
                    onLongPress = onLongPress,
                    albumPhotoView = {
                        AlbumPhotoView(
                            size = size,
                            photo = photos[0],
                            photoDownload = downloadPhoto,
                        )
                    },
                    photo = photos[0],
                    isSelected = photos[0].id in selectedPhotoIds
                )

                if (photos.size == 3) {
                    AlbumPhotoContainer(
                        onClick = onClick,
                        onLongPress = onLongPress,
                        albumPhotoView = {
                            AlbumPhotoView(
                                size = size,
                                photo = photos[2],
                                photoDownload = downloadPhoto,
                            )
                        },
                        photo = photos[2],
                        isSelected = photos[2].id in selectedPhotoIds
                    )
                }
            }
            if (photos.size >= 2) {
                AlbumPhotoContainer(
                    onClick = onClick,
                    onLongPress = onLongPress,
                    albumPhotoView = {
                        AlbumPhotoView(
                            size = size * 2,
                            photo = photos[1],
                            photoDownload = downloadPhoto,
                        )
                    },
                    photo = photos[1],
                    isSelected = photos[1].id in selectedPhotoIds
                )
            }
        }
        Spacer(
            modifier = Modifier.height(1.dp)
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumPhotoContainer(
    onClick: (Photo) -> Unit = {},
    onLongPress: (Photo) -> Unit = {},
    albumPhotoView: @Composable () -> Unit,
    photo: Photo,
    isSelected: Boolean,
) {
    Box(
        modifier = isSelected(isSelected)
            .combinedClickable(
                onClick = { onClick(photo) },
                onLongClick = { onLongPress(photo) }
            )
    ) {
        albumPhotoView()
        if (photo.isFavourite) {
            if (photo is Photo.Image) {
                Image(
                    painter = painterResource(id = R.drawable.ic_overlay),
                    contentScale = ContentScale.FillBounds,
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                )
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_favourite_white),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                tint = Color.Unspecified
            )
        }
        if (photo is Photo.Video) {
            Spacer(modifier = Modifier
                .matchParentSize()
                .background(
                    color = grey_alpha_032
                )
            )

            Text(
                text = TimeUtils.getVideoDuration(photo.duration),
                color = white,
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (isSelected) {
            Icon(
                painter = painterResource(id = R.drawable.ic_select_folder),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                tint = Color.Unspecified
            )
        }
    }
}

@Composable
private fun AlbumPhotoView(
    photo: Photo,
    size: Dp,
    photoDownload: PhotoDownload,
) {
    val imageState = produceState<String?>(initialValue = null) {

        photoDownload(
            false,
            photo,
        ) { downloadSuccess ->
            if (downloadSuccess) {
                value = photo.thumbnailFilePath
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
            .size(size)
            .aspectRatio(1f)
    )
}

@Composable
private fun isSelected(isSelected: Boolean) = if (isSelected) Modifier
    .border(BorderStroke(
        width = 2.dp,
        color = colorResource(id = R.color.teal_300)),
        shape = RoundedCornerShape(4.dp)
    )
    .clip(RoundedCornerShape(4.dp)) else Modifier

