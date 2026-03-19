package mega.privacy.android.feature.photos.presentation.playlists.videoselect.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyVerticalGrid
import mega.privacy.android.shared.nodes.components.NodeHeaderItem
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.feature.photos.components.SelectVideoGridItem
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.model.SelectVideoItemUiEntity
import mega.privacy.android.shared.nodes.components.NodeGridViewItemSkeleton

@Composable
fun SelectVideoGridView(
    items: List<SelectVideoItemUiEntity>,
    gridState: LazyGridState,
    onItemClicked: (SelectVideoItemUiEntity) -> Unit,
    sortConfiguration: NodeSortConfiguration,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    showHiddenItems: Boolean,
    modifier: Modifier = Modifier,
    spanCount: Int = 2,
    showSortOrder: Boolean = true,
    showChangeViewType: Boolean = true,
    isNextPageLoading: Boolean = false,
    listContentPadding: PaddingValues = PaddingValues(0.dp),
) {
    FastScrollLazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(spanCount),
        totalItems = items.size,
        modifier = modifier
            .padding(horizontal = 8.dp)
            .semantics { testTagsAsResourceId = true },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = listContentPadding
    ) {
        if (showSortOrder || showChangeViewType) {
            item(
                key = "header",
                span = {
                    GridItemSpan(currentLineSpan = spanCount)
                }
            ) {
                NodeHeaderItem(
                    onSortOrderClick = onSortOrderClick,
                    onChangeViewTypeClick = onChangeViewTypeClick,
                    onEnterMediaDiscoveryClick = {},
                    sortConfiguration = sortConfiguration,
                    isListView = false,
                    showSortOrder = showSortOrder,
                    showChangeViewType = showChangeViewType,
                    showMediaDiscoveryButton = false,
                )
            }
        }
        items(
            count = items.size,
            key = {
                items[it].id.longValue
            },
        ) {
            val item = items[it]
            SelectVideoGridItem(
                name = item.title.text,
                icon = item.iconRes,
                onItemClicked = { onItemClicked(item) },
                duration = item.duration,
                thumbnailData = ThumbnailRequest(item.id),
                isSelected = item.isSelected,
                isSensitive = showHiddenItems && item.isSensitive,
                isTakenDown = item.isTakenDown,
                isFolder = item.isFolder,
                isAvailableSelected = item.isVideo,
                isEnabled = item.isVideo || item.isFolder
            )
        }

        if (isNextPageLoading) {
            items(count = 4, key = { "loading_$it" }) {
                NodeGridViewItemSkeleton()
            }
        }
    }
}