package mega.privacy.android.feature.photos.presentation.playlists.videoselect.view

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyColumn
import mega.privacy.android.shared.nodes.components.NodeHeaderItem
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.text
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.feature.photos.components.SelectVideoListItem
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.model.SelectVideoItemUiEntity
import mega.privacy.android.shared.nodes.components.NodeListViewItemSkeleton

@Composable
fun SelectVideoListView(
    items: List<SelectVideoItemUiEntity>,
    listState: LazyListState,
    sortConfiguration: NodeSortConfiguration,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    onItemsClicked: (SelectVideoItemUiEntity) -> Unit,
    showHiddenItems: Boolean,
    modifier: Modifier = Modifier,
    isNextPageLoading: Boolean = false,
    showSortOrder: Boolean = true,
    showChangeViewType: Boolean = true,
    listContentPadding: PaddingValues = PaddingValues(0.dp),
) {
    FastScrollLazyColumn(
        state = listState,
        totalItems = items.size,
        modifier = modifier.semantics { testTagsAsResourceId = true },
        contentPadding = listContentPadding
    ) {
        if (showSortOrder || showChangeViewType) {
            item(key = "header") {
                NodeHeaderItem(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .padding(bottom = 8.dp),
                    onSortOrderClick = onSortOrderClick,
                    onChangeViewTypeClick = onChangeViewTypeClick,
                    onEnterMediaDiscoveryClick = {},
                    sortConfiguration = sortConfiguration,
                    isListView = true,
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
            }
        ) {
            val item = items[it]
            SelectVideoListItem(
                title = item.title.text,
                subtitle = item.subtitle.text(),
                icon = item.iconRes,
                isSelected = item.isSelected,
                onItemClicked = {
                    onItemsClicked(item)
                },
                isSensitive = showHiddenItems && item.isSensitive,
                isTakenDown = item.isTakenDown,
                thumbnailData = ThumbnailRequest(item.id)
            )
        }

        if (isNextPageLoading) {
            items(count = 5, key = { "loading_$it" }) {
                NodeListViewItemSkeleton()
            }
        }
    }
}