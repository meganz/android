package mega.privacy.android.app.presentation.audiosection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.audiosection.model.UIAudio
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem

@Composable
internal fun AudioGridView(
    items: List<UIAudio>,
    lazyGridState: LazyGridState,
    sortOrder: String,
    modifier: Modifier,
    onChangeViewTypeClick: () -> Unit,
    onClick: (item: UIAudio, index: Int) -> Unit,
    onMenuClick: (UIAudio) -> Unit,
    onSortOrderClick: () -> Unit,
    spanCount: Int = 2,
    onLongClick: ((item: UIAudio, index: Int) -> Unit) = { _, _ -> },
) {
    LazyVerticalGrid(
        state = lazyGridState,
        columns = GridCells.Fixed(spanCount),
        modifier = modifier.padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item(
            key = "header",
            span = {
                GridItemSpan(currentLineSpan = spanCount)
            }
        ) {
            HeaderViewItem(
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                onSortOrderClick = onSortOrderClick,
                onChangeViewTypeClick = onChangeViewTypeClick,
                onEnterMediaDiscoveryClick = {},
                sortOrder = sortOrder,
                isListView = false,
                showSortOrder = true,
                showChangeViewType = true,
                showMediaDiscoveryButton = false,
            )
        }

        items(count = items.size, key = { items[it].id.longValue }) {
            val audioItem = items[it]
            AudioGridViewItem(
                isSelected = audioItem.isSelected,
                name = audioItem.name,
                thumbnailData = if (audioItem.thumbnail?.exists() == true) {
                    audioItem.thumbnail
                } else {
                    ThumbnailRequest(audioItem.id)
                },
                duration = audioItem.duration,
                isTakenDown = audioItem.isTakenDown,
                onClick = { onClick(audioItem, it) },
                onMenuClick = { onMenuClick(audioItem) },
                onLongClick = { onLongClick(audioItem, it) }
            )
        }
    }
}