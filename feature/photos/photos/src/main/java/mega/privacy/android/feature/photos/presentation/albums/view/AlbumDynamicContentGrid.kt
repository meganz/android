package mega.privacy.android.feature.photos.presentation.albums.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.feature.photos.model.AlbumSortConfiguration
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.icon.pack.IconPack

@Composable
fun AlbumDynamicContentGrid(
    lazyListState: LazyListState,
    photos: ImmutableList<PhotoUiState>,
    smallWidth: Dp,
    selectedPhotos: ImmutableSet<PhotoUiState>,
    modifier: Modifier = Modifier,
    sortConfiguration: AlbumSortConfiguration? = null,
    endSpacing: Dp = 0.dp,
    shouldApplySensitiveMode: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onClick: (PhotoUiState) -> Unit = {},
    onLongPress: (PhotoUiState) -> Unit = {},
    onSortOrderClick: () -> Unit = {},
) {
    val albumContentLayouts = remember(photos) {
        photos.chunked(3).mapIndexed { i, chunkedPhotos ->
            val immutableChunkedPhotos = chunkedPhotos.toImmutableList()
            if (i % 4 == 0) {
                AlbumContentLayout.HighlightStart(immutableChunkedPhotos)
            } else if (i % 4 == 1 || i % 4 == 3) {
                AlbumContentLayout.Uniform(immutableChunkedPhotos)
            } else {
                AlbumContentLayout.HighlightEnd(immutableChunkedPhotos)
            }
        }
    }

    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(1.dp),
        contentPadding = contentPadding
    ) {
        if (sortConfiguration != null) {
            this.item(key = "sort_option") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .clickable(onClick = onSortOrderClick)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .align(Alignment.CenterStart)
                            .testTag(ALBUM_DYNAMIC_CONTENT_GRID_SORT_ITEM)
                    ) {
                        MegaText(
                            style = AppTheme.typography.titleSmall,
                            textColor = TextColor.Secondary,
                            text = stringResource(sortConfiguration.sortOption.displayName),
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        MegaIcon(
                            imageVector = if (sortConfiguration.sortDirection == SortDirection.Ascending) {
                                IconPack.Small.Thin.Outline.ArrowUp
                            } else {
                                IconPack.Small.Thin.Outline.ArrowDown
                            },
                            tint = IconColor.Secondary,
                            contentDescription = "DropDown arrow",
                            modifier = Modifier
                                .align(CenterVertically)
                                .size(16.dp),
                        )
                    }
                }
            }
        }

        this.items(
            albumContentLayouts,
            key = { it.key },
        ) { layout ->
            when (layout) {
                is AlbumContentLayout.HighlightStart -> {
                    AlbumContentHighlightStart(
                        size = smallWidth,
                        photos = layout.content,
                        onClick = onClick,
                        onLongPress = onLongPress,
                        selectedPhotos = selectedPhotos,
                        shouldApplySensitiveMode = shouldApplySensitiveMode,
                    )
                }

                is AlbumContentLayout.Uniform -> {
                    AlbumContentUniform(
                        size = smallWidth,
                        photos = layout.content,
                        onClick = onClick,
                        onLongPress = onLongPress,
                        selectedPhotos = selectedPhotos,
                        shouldApplySensitiveMode = shouldApplySensitiveMode,
                    )
                }

                is AlbumContentLayout.HighlightEnd -> {
                    AlbumContentHighlightEnd(
                        size = smallWidth,
                        photos = layout.content,
                        onClick = onClick,
                        onLongPress = onLongPress,
                        selectedPhotos = selectedPhotos,
                        shouldApplySensitiveMode = shouldApplySensitiveMode,
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(endSpacing))
        }
    }
}

@Composable
fun AlbumDynamicContentGridSkeleton(
    size: Dp,
    modifier: Modifier = Modifier,
    count: Int = 1
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        repeat(count) {
            AlbumContentHighlightStartShimmer(size)
            AlbumContentUniformShimmer(size)
            AlbumContentHighlightEndShimmer(size)
        }
    }
}

const val ALBUM_DYNAMIC_CONTENT_GRID_SORT_ITEM =
    "album_dynamic_content_grid:sort_item"