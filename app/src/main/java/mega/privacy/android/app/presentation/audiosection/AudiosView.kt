package mega.privacy.android.app.presentation.audiosection

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.app.presentation.audiosection.model.UIAudio
import mega.privacy.android.app.presentation.videosection.model.UIVideo

/**
 * The compose view for displaying audios
 */
@Composable
fun AudiosView(
    items: List<UIAudio>,
    isListView: Boolean,
    listState: LazyListState = LazyListState(),
    gridState: LazyGridState = LazyGridState(),
    sortOrder: String,
    modifier: Modifier,
    onChangeViewTypeClick: () -> Unit,
    onClick: (item: UIVideo, index: Int) -> Unit,
    onMenuClick: (UIVideo) -> Unit,
    onSortOrderClick: () -> Unit,
    onLongClick: ((item: UIVideo, index: Int) -> Unit) = { _, _ -> },
) {
    if (isListView) {
        AudioListView(
            items = items,
            lazyListState = listState,
            sortOrder = sortOrder,
            modifier = modifier,
            onChangeViewTypeClick = onChangeViewTypeClick,
            onClick = onClick,
            onMenuClick = onMenuClick,
            onSortOrderClick = onSortOrderClick,
            onLongClick = onLongClick,
        )
    } else {
        AudioGridView(
            items = items,
            lazyGridState = gridState,
            sortOrder = sortOrder,
            modifier = modifier,
            onChangeViewTypeClick = onChangeViewTypeClick,
            onClick = onClick,
            onMenuClick = onMenuClick,
            onSortOrderClick = onSortOrderClick,
            onLongClick = onLongClick,
        )
    }
}