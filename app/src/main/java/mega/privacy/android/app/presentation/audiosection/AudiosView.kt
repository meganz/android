package mega.privacy.android.app.presentation.audiosection

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.app.presentation.audiosection.model.AudioUiEntity
import mega.privacy.android.domain.entity.AccountType

/**
 * The compose view for displaying audios
 */
@Composable
fun AudiosView(
    items: List<AudioUiEntity>,
    accountType: AccountType?,
    isListView: Boolean,
    sortOrder: String,
    modifier: Modifier,
    onChangeViewTypeClick: () -> Unit,
    onClick: (item: AudioUiEntity, index: Int) -> Unit,
    onMenuClick: (AudioUiEntity) -> Unit,
    onSortOrderClick: () -> Unit,
    inSelectionMode: Boolean = false,
    listState: LazyListState = LazyListState(),
    gridState: LazyGridState = LazyGridState(),
    onLongClick: ((item: AudioUiEntity, index: Int) -> Unit) = { _, _ -> },
) {
    if (isListView) {
        AudioListView(
            items = items,
            accountType = accountType,
            lazyListState = listState,
            sortOrder = sortOrder,
            modifier = modifier,
            onChangeViewTypeClick = onChangeViewTypeClick,
            onClick = onClick,
            onMenuClick = onMenuClick,
            onSortOrderClick = onSortOrderClick,
            onLongClick = onLongClick,
            inSelectionMode = inSelectionMode,
        )
    } else {
        AudioGridView(
            items = items,
            accountType = accountType,
            lazyGridState = gridState,
            sortOrder = sortOrder,
            modifier = modifier,
            onChangeViewTypeClick = onChangeViewTypeClick,
            onClick = onClick,
            onMenuClick = onMenuClick,
            onSortOrderClick = onSortOrderClick,
            onLongClick = onLongClick,
            inSelectionMode = inSelectionMode,
        )
    }
}