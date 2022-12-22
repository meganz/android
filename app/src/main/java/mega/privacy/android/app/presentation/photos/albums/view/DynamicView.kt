package mega.privacy.android.app.presentation.photos.albums.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.photos.albums.model.AlbumPhotoItem
import mega.privacy.android.app.presentation.photos.model.PhotoDownload
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.presentation.theme.grey_100
import mega.privacy.android.presentation.theme.grey_300
import mega.privacy.android.presentation.theme.grey_600
import mega.privacy.android.presentation.theme.grey_900

@Composable
internal fun DynamicView(
    photos: List<Photo>,
    smallWidth: Dp,
    photoDownload: PhotoDownload,
    onClick: (Photo) -> Unit = {},
    onLongPress: (Photo) -> Unit = {},
    selectedPhotoIds: Set<Long>,
) {
    val dynamicList = remember(photos) {
        photos.chunked(3).mapIndexed { i, list ->
            if (i % 4 == 0) {
                AlbumPhotoItem.BigSmall2Item(
                    list
                )
            } else if (i % 4 == 1 || i % 4 == 3) {
                AlbumPhotoItem.Small3Item(
                    list
                )
            } else {
                AlbumPhotoItem.Small2BigItem(
                    list
                )
            }
        }
    }

    LazyColumn(
        state = rememberSaveable(saver = LazyListState.Saver) {
            LazyListState()
        },
    ) {
        this.items(
            dynamicList,
            key = { it.key }
        ) { item ->
            when (item) {
                is AlbumPhotoItem.BigSmall2Item -> {
                    PhotosBig2SmallItems(
                        size = smallWidth,
                        photos = item.photos,
                        photoDownload = photoDownload,
                        onClick = onClick,
                        onLongPress = onLongPress,
                        selectedPhotoIds = selectedPhotoIds
                    )
                }
                is AlbumPhotoItem.Small3Item -> {
                    Photos3SmallItems(
                        size = smallWidth,
                        photos = item.photos,
                        downloadPhoto = photoDownload,
                        onClick = onClick,
                        onLongPress = onLongPress,
                        selectedPhotoIds = selectedPhotoIds
                    )
                }
                is AlbumPhotoItem.Small2BigItem -> {
                    Photos2SmallBigItems(
                        size = smallWidth,
                        photos = item.photos,
                        downloadPhoto = photoDownload,
                        onClick = onClick,
                        onLongPress = onLongPress,
                        selectedPhotoIds = selectedPhotoIds
                    )
                }
            }
        }
    }
}

@Composable
internal fun EmptyView(
    imageResId: Int,
    textResId: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = imageResId),
            contentDescription = "Empty",
            modifier = Modifier.padding(bottom = 30.dp),
            tint = if (!MaterialTheme.colors.isLight) {
                Color.Gray
            } else {
                Color.Unspecified
            }
        )

        val placeHolderStart = "[B]"
        val placeHolderEnd = "[/B]"
        val boldPlaceHolderStart = "[A]"
        val boldPlaceHolderEnd = "[/A]"

        val text: String =
            stringResource(id = textResId)

        Text(
            color = if (MaterialTheme.colors.isLight) grey_600 else grey_300,
            text = buildAnnotatedString {
                append(text.substring(0, text.indexOf(placeHolderStart)))

                append(
                    text.substring(
                        text.indexOf(placeHolderStart),
                        text.indexOf(placeHolderEnd)
                    ).replace("[B]", "")
                )

                append(
                    text.substring(
                        text.indexOf(placeHolderEnd),
                        text.indexOf(boldPlaceHolderStart)
                    ).replace("[/B]", "")
                )

                withStyle(SpanStyle(if (MaterialTheme.colors.isLight) grey_900 else grey_100)) {
                    append(
                        text.substring(
                            text.indexOf(boldPlaceHolderStart),
                            text.indexOf(boldPlaceHolderEnd)
                        ).replace("[A]", "")
                    )
                }

                append(
                    text.substring(text.indexOf(boldPlaceHolderEnd)).replace("[/A]", "")
                )
            }
        )
    }
}