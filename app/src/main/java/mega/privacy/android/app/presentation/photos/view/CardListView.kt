package mega.privacy.android.app.presentation.photos.view

import mega.privacy.android.icon.pack.R as IconPackR
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.model.DateCard
import mega.privacy.android.app.presentation.photos.model.DateCardCount
import mega.privacy.android.app.presentation.photos.model.PhotoDownload
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.shared.original.core.ui.controls.layouts.FastScrollLazyVerticalGrid

@Composable
fun CardListView(
    dateCards: List<DateCard>,
    photoDownload: PhotoDownload,
    onCardClick: (DateCard) -> Unit,
    state: LazyGridState = rememberLazyGridState(),
    accountType: AccountType? = null,
    cardListViewHeaderView: @Composable () -> Unit = {},
) {
    val spanCount =
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
            DateCardCount.Grid.portrait
        } else {
            DateCardCount.Grid.landscape
        }

    FastScrollLazyVerticalGrid(
        totalItems = dateCards.size + 2,
        columns = GridCells.Fixed(spanCount),
        modifier = Modifier.fillMaxSize(),
        state = state,
        tooltipText = { index ->
            dateCards.getOrNull(index)?.date ?: ""
        },
    ) {
        item(
            span = { GridItemSpan(currentLineSpan = maxCurrentLineSpan) }
        ) {
            cardListViewHeaderView()
        }

        itemsIndexed(
            items = dateCards,
            key = { _, item ->
                item.key
            }
        ) { _, dateCard ->
            val photo = dateCard.photo
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 12.dp)
                    .clickable {
                        onCardClick(dateCard)
                    },
                shape = RoundedCornerShape(8.dp),
                elevation = 10.dp
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {

                    val imageState = produceState<String?>(initialValue = null) {

                        photoDownload(
                            true,
                            dateCard.photo,
                        ) { downloadSuccess ->
                            if (downloadSuccess) {
                                value = dateCard.photo.previewFilePath
                            }
                        }
                    }

                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageState.value)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        placeholder = painterResource(id = IconPackR.drawable.ic_image_medium_solid),
                        error = painterResource(id = IconPackR.drawable.ic_image_medium_solid),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .alpha(0.5f.takeIf {
                                accountType?.isPaid == true && (photo.isSensitive || photo.isSensitiveInherited)
                            } ?: 1f)
                            .blur(16.dp.takeIf {
                                accountType?.isPaid == true && (photo.isSensitive || photo.isSensitiveInherited)
                            } ?: 0.dp)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(color = colorResource(id = R.color.grey_alpha_012))
                    ) {
                        Text(
                            text = dateCard.date,
                            color = colorResource(id = R.color.white),
                            modifier = Modifier
                                .wrapContentSize()
                                .align(Alignment.TopStart)
                                .padding(8.dp),
                            style = MaterialTheme.typography.subtitle1,
                            fontSize = 20.sp
                        )

                        val text = if (dateCard is DateCard.DaysCard) {
                            if (dateCard.photosCount.toInt() - 1 == 0) {
                                ""
                            } else {
                                "+" + (dateCard.photosCount.toInt() - 1)
                            }
                        } else ""

                        Text(
                            text = text,
                            color = colorResource(id = R.color.white),
                            modifier = Modifier
                                .wrapContentSize()
                                .align(Alignment.BottomEnd)
                                .padding(8.dp),
                            style = MaterialTheme.typography.subtitle1,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
        item(
            span = { GridItemSpan(currentLineSpan = 1) }
        ) {
            Spacer(
                modifier = Modifier.height(50.dp)
            )
        }
    }
}