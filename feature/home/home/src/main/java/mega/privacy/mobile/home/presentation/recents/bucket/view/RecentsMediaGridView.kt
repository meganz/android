package mega.privacy.mobile.home.presentation.recents.bucket.view

import MediaGridViewItem
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyVerticalGrid
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.TypedNode

@Composable
fun <T : TypedNode> RecentsMediaGridView(
    modifier: Modifier = Modifier,
    nodeUiItems: List<NodeUiItem<T>>,
    onItemClicked: (NodeUiItem<T>) -> Unit,
    onLongClick: (NodeUiItem<T>) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isInLandscapeMode = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    FastScrollLazyVerticalGrid(
        modifier = modifier,
        state = rememberLazyGridState(),
        totalItems = nodeUiItems.size,
        columns = GridCells.Fixed(if (isInLandscapeMode) 6 else 3),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        items(nodeUiItems) { uiItem ->
            MediaGridViewItem(
                thumbnailData = uiItem.thumbnailData,
                defaultImage = uiItem.iconRes,
                duration = uiItem.duration,
                isSelected = uiItem.isSelected,
                showFavourite = uiItem.showFavourite,
                isSensitive = uiItem.isSensitive,
                onClick = { onItemClicked(uiItem) },
                onLongClick = { onLongClick(uiItem) },
            )
        }
    }
}