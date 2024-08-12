package mega.privacy.android.app.presentation.audiosection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.audiosection.model.AudioUiEntity
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.icon.pack.R
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem
import mega.privacy.android.shared.original.core.ui.controls.lists.NodeGridViewItem

@Composable
internal fun AudioGridView(
    items: List<AudioUiEntity>,
    accountType: AccountType?,
    lazyGridState: LazyGridState,
    sortOrder: String,
    modifier: Modifier,
    onChangeViewTypeClick: () -> Unit,
    onClick: (item: AudioUiEntity, index: Int) -> Unit,
    onMenuClick: (AudioUiEntity) -> Unit,
    onSortOrderClick: () -> Unit,
    spanCount: Int = 2,
    inSelectionMode: Boolean = false,
    onLongClick: ((item: AudioUiEntity, index: Int) -> Unit) = { _, _ -> },
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
            NodeGridViewItem(
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
                onMenuClick = { onMenuClick(audioItem) }.takeIf { !inSelectionMode },
                onLongClick = { onLongClick(audioItem, it) },
                iconRes = R.drawable.ic_audio_medium_solid,
                modifier = Modifier
                    .alpha(0.5f.takeIf {
                        accountType?.isPaid == true && (audioItem.isMarkedSensitive || audioItem.isSensitiveInherited)
                    } ?: 1f),
                isSensitive = accountType?.isPaid == true && (audioItem.isMarkedSensitive || audioItem.isSensitiveInherited),
                showBlurEffect = true,
            )
        }
    }
}