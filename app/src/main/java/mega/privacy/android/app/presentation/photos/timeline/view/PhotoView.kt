package mega.privacy.android.app.presentation.photos.timeline.view

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.model.PhotoDownload
import mega.privacy.android.app.presentation.photos.model.ZoomLevel
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.core.R as CoreUiR
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.shared.original.core.ui.theme.extensions.conditional
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_050_grey_700

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoView(
    photo: Photo,
    isSelected: Boolean,
    currentZoomLevel: ZoomLevel,
    shouldApplySensitiveMode: Boolean,
    onClick: (Photo) -> Unit,
    onLongPress: (Photo) -> Unit,
    downloadPhoto: PhotoDownload,
    isPreview: Boolean,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val span = remember(configuration.orientation, currentZoomLevel) {
        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            currentZoomLevel.portrait
        } else {
            currentZoomLevel.landscape
        }
    }
    val imageSize = remember(span) {
        with(density) {
            (windowInfo.containerSize.width / span).toDp()
        }
    }
    var photoBoxModifier = remember(currentZoomLevel, imageSize) {
        when (currentZoomLevel) {
            ZoomLevel.Grid_1 -> modifier
                .size(imageSize)
                .padding(bottom = 4.dp)

            ZoomLevel.Grid_3 -> modifier
                .size(imageSize)
                .padding(all = 1.5.dp)

            else -> modifier
                .size(imageSize)
                .padding(all = 1.dp)
        }
    }

    if (isSelected) {
        photoBoxModifier = photoBoxModifier
            .border(
                BorderStroke(
                    width = 2.dp,
                    color = colorResource(id = R.color.accent_900)
                ),
                shape = RoundedCornerShape(4.dp)
            )
            .clip(RoundedCornerShape(4.dp))
    }

    Box(modifier = photoBoxModifier) {
        PhotoCoverView(
            photo = photo,
            shouldApplySensitiveMode = shouldApplySensitiveMode,
            currentZoomLevel = currentZoomLevel,
            downloadPhoto = downloadPhoto,
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    onClick = { onClick(photo) },
                    onLongClick = { onLongPress(photo) }
                ),
            isPreview = isPreview
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
            MegaIcon(
                painter = painterResource(id = R.drawable.ic_favourite_white),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
            )
        }
    }

}

@Composable
private fun SelectedIconView(
    modifier: Modifier,
) {
    MegaIcon(
        painter = painterResource(id = CoreUiR.drawable.ic_select_folder),
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
private fun PhotoCoverView(
    photo: Photo,
    shouldApplySensitiveMode: Boolean,
    currentZoomLevel: ZoomLevel,
    downloadPhoto: PhotoDownload,
    isPreview: Boolean,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current

    Box(
        modifier = modifier
    ) {
        when (photo) {
            is Photo.Image -> {
                PhotoImageView(
                    photo = photo,
                    isPreview = isPreview,
                    downloadPhoto = downloadPhoto,
                    shouldApplySensitiveMode = shouldApplySensitiveMode,
                    showOverlayOnSuccess = false
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
                    shouldApplySensitiveMode = shouldApplySensitiveMode,
                    isPreview = isPreview,
                    downloadPhoto = downloadPhoto,
                    showOverlayOnSuccess = true
                )

                Text(
                    text = TimeUtils.getVideoDuration(photo.fileTypeInfo.duration.inWholeSeconds.toInt()),
                    color = Color.White,
                    modifier = Modifier
                        .wrapContentSize()
                        .align(Alignment.BottomEnd)
                        .padding(
                            if (currentZoomLevel == ZoomLevel.Grid_5) {
                                spacing.x4
                            } else {
                                spacing.x16
                            }
                        ),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

            }
        }
    }
}


/**
 * Photo image view for grid layout
 */
@Composable
fun PhotoImageView(
    photo: Photo,
    shouldApplySensitiveMode: Boolean,
    isPreview: Boolean,
    downloadPhoto: PhotoDownload,
    showOverlayOnSuccess: Boolean = false,
    alpha: Float = DefaultAlpha,
) {
    var showOverlayState by remember { mutableStateOf(false) }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.grey_050_grey_700)
    ) {
        if (!imageState.value.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest
                    .Builder(LocalContext.current)
                    .data(imageState.value)
                    .crossfade(true)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = {
                    if (showOverlayOnSuccess && imageState.value != null) {
                        showOverlayState = true
                    }
                },
                alpha = alpha,
                modifier = Modifier
                    .aspectRatio(1f)
                    .conditional(shouldApplySensitiveMode && (photo.isSensitive || photo.isSensitiveInherited)) {
                        this
                            .alpha(0.5f)
                            .blur(16.dp)
                    }
            )
            if (showOverlayState)
                Spacer(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            color = colorResource(id = R.color.grey_alpha_032)
                        )
                )
        }
    }
}
