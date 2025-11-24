package mega.privacy.android.feature.photos.presentation.timeline.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.components.chip.MegaChip
import mega.android.core.ui.components.chip.SelectionChipStyle
import mega.android.core.ui.components.list.SecondaryHeaderListItem
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyColumn
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.photos.DownloadPhotoResult
import mega.privacy.android.feature.photos.extensions.LocalDownloadPhotoResultMock
import mega.privacy.android.feature.photos.extensions.downloadAsStateWithLifecycle
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotoNodeListCardItem
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCard
import mega.privacy.android.icon.pack.R as IconPackR
import java.time.LocalDateTime
import kotlin.time.Duration

@Composable
internal fun PhotosNodeListCardListView(
    photos: ImmutableList<PhotosNodeListCard>,
    onClick: (photo: PhotosNodeListCard) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    state: LazyListState = rememberLazyListState(),
    header: (@Composable () -> Unit)? = null,
) {
    FastScrollLazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        totalItems = photos.size,
        state = state,
        tooltipText = { index ->
            photos.getOrNull(index)?.date.orEmpty()
        }
    ) {
        header?.let {
            item(key = "PhotosNodeListCardListView:Header") {
                it()
            }
        }

        items(
            items = photos,
            key = { it.key }
        ) { photo ->
            SecondaryHeaderListItem(
                modifier = Modifier.fillMaxWidth(),
                text = photo.date
            )

            Box(
                modifier = Modifier
                    .clickable { onClick(photo) }
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(shape = RoundedCornerShape(16.dp))
            ) {
                val downloadResult by photo.photoItem.photo.downloadAsStateWithLifecycle(isPreview = true)
                val filePath = when (val result = downloadResult) {
                    is DownloadPhotoResult.Success -> result.previewFilePath
                    else -> null
                }
                AsyncImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .alpha(0.5f.takeIf { photo.photoItem.isMarkedSensitive } ?: 1f)
                        .blur(16.dp.takeIf { photo.photoItem.isMarkedSensitive } ?: 0.dp)
                        .testTag(PHOTOS_NODE_LIST_CARD_LIST_VIEW_IMAGE_TAG),
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(filePath)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    placeholder = rememberAsyncImagePainter(model = IconPackR.drawable.ic_image_medium_solid),
                    error = rememberAsyncImagePainter(model = IconPackR.drawable.ic_image_medium_solid),
                    contentScale = ContentScale.Crop
                )

                if (photo is PhotosNodeListCard.Days && photo.photosCount > 1) {
                    MegaChip(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .testTag(PHOTOS_NODE_LIST_CARD_LIST_VIEW_PHOTO_COUNT_TAG),
                        onClick = {},
                        selected = false,
                        text = "+${photo.photosCount}",
                        style = SelectionChipStyle,
                    )
                }
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PhotosNodeListCardListViewPreview() {
    AndroidThemeForPreviews {
        CompositionLocalProvider(LocalDownloadPhotoResultMock provides DownloadPhotoResult.Idle) {
            PhotosNodeListCardListView(
                photos = persistentListOf(
                    PhotosNodeListCard.Days(
                        date = "2022-01-01",
                        photoItem = PhotoNodeListCardItem(
                            photo = PhotoUiState.Image(
                                id = 1L,
                                albumPhotoId = null,
                                parentId = 0L,
                                name = "test.jpg",
                                isFavourite = false,
                                creationTime = LocalDateTime.now(),
                                modificationTime = LocalDateTime.now(),
                                thumbnailFilePath = null,
                                previewFilePath = null,
                                fileTypeInfo = VideoFileTypeInfo(
                                    mimeType = "video/mp4",
                                    extension = "mp4",
                                    duration = Duration.ZERO
                                ),
                                size = 0L,
                                isTakenDown = false,
                                isSensitive = false,
                                isSensitiveInherited = false,
                                base64Id = null,
                            ),
                            isMarkedSensitive = false
                        ),
                        photosCount = 10
                    ),
                    PhotosNodeListCard.Months(
                        date = "2022-01-02",
                        photoItem = PhotoNodeListCardItem(
                            photo = PhotoUiState.Image(
                                id = 1L,
                                albumPhotoId = null,
                                parentId = 0L,
                                name = "test.jpg",
                                isFavourite = false,
                                creationTime = LocalDateTime.now(),
                                modificationTime = LocalDateTime.now(),
                                thumbnailFilePath = null,
                                previewFilePath = null,
                                fileTypeInfo = VideoFileTypeInfo(
                                    mimeType = "video/mp4",
                                    extension = "mp4",
                                    duration = Duration.ZERO
                                ),
                                size = 0L,
                                isTakenDown = false,
                                isSensitive = false,
                                isSensitiveInherited = false,
                                base64Id = null,
                            ),
                            isMarkedSensitive = true
                        ),
                    ),
                    PhotosNodeListCard.Years(
                        date = "2022-02-02",
                        photoItem = PhotoNodeListCardItem(
                            photo = PhotoUiState.Image(
                                id = 1L,
                                albumPhotoId = null,
                                parentId = 0L,
                                name = "test.jpg",
                                isFavourite = false,
                                creationTime = LocalDateTime.now(),
                                modificationTime = LocalDateTime.now(),
                                thumbnailFilePath = null,
                                previewFilePath = null,
                                fileTypeInfo = VideoFileTypeInfo(
                                    mimeType = "video/mp4",
                                    extension = "mp4",
                                    duration = Duration.ZERO
                                ),
                                size = 0L,
                                isTakenDown = false,
                                isSensitive = false,
                                isSensitiveInherited = false,
                                base64Id = null,
                            ),
                            isMarkedSensitive = false
                        ),
                    )
                ),
                onClick = {}
            )
        }
    }
}

internal const val PHOTOS_NODE_LIST_CARD_LIST_VIEW_IMAGE_TAG =
    "photos_node_list_card_list_view:image_item"
internal const val PHOTOS_NODE_LIST_CARD_LIST_VIEW_PHOTO_COUNT_TAG =
    "photos_node_list_card_list_view:text_photo_count"
