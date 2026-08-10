package mega.privacy.android.feature.photos.presentation.mediadiscovery.view

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.domain.entity.photos.DateCard
import mega.privacy.android.domain.entity.photos.DateCardCount
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.nodes.components.NodeThumbnailView
import mega.privacy.android.shared.nodes.components.ThumbnailLayoutType
import java.time.LocalDateTime

// Todo: Update card to new designs when available from design team
@Composable
internal fun MediaDiscoveryCardListView(
    dateCards: List<DateCard>,
    onCardClick: (DateCard) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    fromFolderLink: Boolean = false,
    shouldApplySensitiveMode: Boolean = false,
) {
    val spanCount =
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
            DateCardCount.Grid.portrait
        } else {
            DateCardCount.Grid.landscape
        }

    LazyVerticalGrid(
        columns = GridCells.Fixed(spanCount),
        modifier = modifier.fillMaxSize(),
        state = state,
    ) {
        itemsIndexed(
            items = dateCards,
            key = { _, item -> item.key }
        ) { _, dateCard ->
            val photo = dateCard.photo
            val isSensitive = shouldApplySensitiveMode &&
                    (photo.isSensitive || photo.isSensitiveInherited)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 12.dp)
                    .clickable { onCardClick(dateCard) },
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(10.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    NodeThumbnailView(
                        data = ThumbnailRequest(
                            id = NodeId(photo.id),
                            isPublicNode = fromFolderLink,
                        ),
                        defaultImage = iconPackR.drawable.ic_image_medium_solid,
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        blurImage = isSensitive,
                        layoutType = ThumbnailLayoutType.Grid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .alpha(if (isSensitive) 0.5f else 1f),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(color = Color.Black.copy(alpha = 0.12f))
                    )
                    Text(
                        text = dateCard.date,
                        color = Color.White,
                        modifier = Modifier
                            .wrapContentSize()
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        fontSize = 20.sp,
                    )

                    if (dateCard is DateCard.DaysCard) {
                        val count = dateCard.photosCount.toIntOrNull() ?: 0
                        if (count > 1) {
                            Text(
                                text = "+${count - 1}",
                                color = Color.White,
                                modifier = Modifier
                                    .wrapContentSize()
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp),
                                fontSize = 18.sp,
                            )
                        }
                    }
                }
            }
        }
        item(
            span = { GridItemSpan(currentLineSpan = 1) }
        ) {
            Spacer(modifier = Modifier.height(56.dp))
        }
    }
}

@CombinedThemePreviews
@Composable
private fun MediaDiscoveryCardListViewYearsPreview() {
    AndroidThemeForPreviews {
        fun samplePhoto(id: Long = 1L) = Photo.Image(
            id = id,
            parentId = 1L,
            name = "photo_$id.jpg",
            isFavourite = false,
            creationTime = LocalDateTime.of(2024, 6, 15, 10, 0),
            modificationTime = LocalDateTime.of(2024, 6, 15, 10, 0),
            thumbnailFilePath = null,
            previewFilePath = null,
            fileTypeInfo = StaticImageFileTypeInfo(mimeType = "image/jpeg", extension = "jpg"),
        )

        MediaDiscoveryCardListView(
            dateCards = listOf(
                DateCard.YearsCard(date = "2024", photo = samplePhoto(1L)),
                DateCard.YearsCard(date = "2023", photo = samplePhoto(2L)),
                DateCard.YearsCard(date = "2022", photo = samplePhoto(3L)),
            ),
            onCardClick = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun MediaDiscoveryCardListViewDaysPreview() {
    AndroidThemeForPreviews {
        fun samplePhoto(id: Long = 1L) = Photo.Image(
            id = id,
            parentId = 1L,
            name = "photo_$id.jpg",
            isFavourite = false,
            creationTime = LocalDateTime.of(2024, 6, 15, 10, 0),
            modificationTime = LocalDateTime.of(2024, 6, 15, 10, 0),
            thumbnailFilePath = null,
            previewFilePath = null,
            fileTypeInfo = StaticImageFileTypeInfo(mimeType = "image/jpeg", extension = "jpg"),
        )

        MediaDiscoveryCardListView(
            dateCards = listOf(
                DateCard.DaysCard(
                    date = "15 Jun 2024",
                    photo = samplePhoto(1L),
                    photosCount = "12",
                ),
                DateCard.DaysCard(
                    date = "14 Jun 2024",
                    photo = samplePhoto(2L),
                    photosCount = "1",
                ),
            ),
            onCardClick = {},
        )
    }
}
