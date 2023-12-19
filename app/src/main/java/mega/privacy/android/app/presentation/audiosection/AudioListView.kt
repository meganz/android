package mega.privacy.android.app.presentation.audiosection

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.audiosection.model.UIAudio
import mega.privacy.android.app.presentation.videosection.model.UIVideo
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem

@Composable
internal fun AudioListView(
    items: List<UIAudio>,
    lazyListState: LazyListState,
    sortOrder: String,
    modifier: Modifier,
    onChangeViewTypeClick: () -> Unit,
    onClick: (item: UIVideo, index: Int) -> Unit,
    onMenuClick: (UIVideo) -> Unit,
    onSortOrderClick: () -> Unit,
    onLongClick: ((item: UIVideo, index: Int) -> Unit) = { _, _ -> },
) {
    LazyColumn(state = lazyListState, modifier = modifier) {
        item(
            key = "header"
        ) {
            HeaderViewItem(
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                onSortOrderClick = onSortOrderClick,
                onChangeViewTypeClick = onChangeViewTypeClick,
                onEnterMediaDiscoveryClick = {},
                sortOrder = sortOrder,
                isListView = true,
                showSortOrder = true,
                showChangeViewType = false,
                showMediaDiscoveryButton = false,
            )
        }

        items(count = items.size, key = { items[it].id.longValue }) {
        }
    }
}