package mega.privacy.android.feature.photos.presentation.timeline.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import mega.android.core.ui.components.chip.MegaChip
import mega.android.core.ui.components.chip.SelectionChipStyle
import mega.android.core.ui.components.list.SecondaryHeaderListItem
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyColumn
import mega.android.core.ui.modifiers.conditional
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.photos.thumbnail.MediaThumbnailRequest
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCard
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCardPeriod
import mega.privacy.android.icon.pack.R as IconPackR

@Composable
internal fun PhotosNodeListCardListView(
    photos: List<PhotosNodeListCard>,
    isHiddenNodesEnabled: Boolean,
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
            photos.getOrNull(index)?.formattedDate.orEmpty()
        }
    ) {
        header?.let {
            item(key = "PhotosNodeListCardListView:Header") {
                it()
            }
        }

        itemsIndexed(
            items = photos,
            key = { _, item -> item.key }
        ) { index, photo ->
            SecondaryHeaderListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (index == 0) 8.dp else 0.dp),
                text = photo.formattedDate
            )

            Box(
                modifier = Modifier
                    .clickable { onClick(photo) }
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(shape = RoundedCornerShape(16.dp))
            ) {
                val context = LocalContext.current
                val request = remember(photo.id) {
                    ImageRequest.Builder(context)
                        .data(
                            MediaThumbnailRequest(
                                id = photo.id,
                                isPreview = true,
                                thumbnailFilePath = photo.thumbnailFilePath,
                                previewFilePath = photo.previewFilePath,
                                isPublicNode = false,
                                fileExtension = photo.extension
                            )
                        )
                        .crossfade(enable = true)
                        .build()
                }
                val isSensitiveBlur = isHiddenNodesEnabled && photo.isSensitive
                AsyncImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .conditional(isSensitiveBlur) {
                            Modifier
                                .alpha(0.5f)
                                .blur(16.dp)
                        }
                        .testTag(PHOTOS_NODE_LIST_CARD_LIST_VIEW_IMAGE_TAG),
                    model = request,
                    contentDescription = null,
                    placeholder = rememberAsyncImagePainter(model = IconPackR.drawable.ic_image_medium_solid),
                    error = rememberAsyncImagePainter(model = IconPackR.drawable.ic_image_medium_solid),
                    contentScale = ContentScale.Crop
                )

                if (photo.period == PhotosNodeListCardPeriod.Day && photo.count > 1) {
                    MegaChip(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .testTag(PHOTOS_NODE_LIST_CARD_LIST_VIEW_PHOTO_COUNT_TAG),
                        onClick = { onClick(photo) },
                        selected = false,
                        text = "+${photo.count}",
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
        PhotosNodeListCardListView(
            photos = listOf(
                PhotosNodeListCard(
                    period = PhotosNodeListCardPeriod.Day,
                    key = 3L,
                    id = 3L,
                    day = 1,
                    month = 1,
                    year = 1,
                    formattedDate = "2022-01-01",
                    thumbnailFilePath = null,
                    previewFilePath = null,
                    extension = "",
                    isSensitive = false,
                    count = 10
                ),
                PhotosNodeListCard(
                    period = PhotosNodeListCardPeriod.Month,
                    key = 3L,
                    id = 3L,
                    day = 1,
                    month = 1,
                    year = 1,
                    formattedDate = "2022-01-02",
                    thumbnailFilePath = null,
                    previewFilePath = null,
                    extension = "",
                    isSensitive = false,
                    count = 10
                ),
                PhotosNodeListCard(
                    period = PhotosNodeListCardPeriod.Year,
                    key = 3L,
                    id = 3L,
                    day = 1,
                    month = 1,
                    year = 1,
                    formattedDate = "2022-02-02",
                    thumbnailFilePath = null,
                    previewFilePath = null,
                    extension = "",
                    isSensitive = false,
                    count = 10
                )
            ),
            isHiddenNodesEnabled = false,
            onClick = {}
        )
    }
}

internal const val PHOTOS_NODE_LIST_CARD_LIST_VIEW_IMAGE_TAG =
    "photos_node_list_card_list_view:image_item"
internal const val PHOTOS_NODE_LIST_CARD_LIST_VIEW_PHOTO_COUNT_TAG =
    "photos_node_list_card_list_view:text_photo_count"
