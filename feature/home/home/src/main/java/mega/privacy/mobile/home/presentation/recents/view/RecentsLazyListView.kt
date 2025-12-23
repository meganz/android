package mega.privacy.mobile.home.presentation.recents.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.mobile.home.presentation.home.widget.recents.mockRecentsUiItemList
import mega.privacy.mobile.home.presentation.recents.model.RecentsUiItem

/**
 * Composable for displaying recent actions in a scrollable list with sticky headers
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentsLazyListView(
    items: List<RecentsUiItem>,
    onFileClicked: (TypedFileNode, NodeSourceType) -> Unit,
    onMenuClicked: (TypedFileNode, NodeSourceType) -> Unit,
    onBucketClicked: (RecentsUiItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val grouped = remember(items) {
        items.groupBy { it.bucket.dateTimestamp }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        grouped.forEach { (dateTimestamp, itemsForDate) ->
            stickyHeader {
                RecentDateHeader(timestamp = dateTimestamp)
            }

            items(
                items = itemsForDate,
                key = { it.bucket.identifier }
            ) { item ->
                RecentsListItemView(
                    item = item,
                    onItemClicked = {
                        if (item.isSingleNode) {
                            item.firstNode?.let { node ->
                                onFileClicked(node, item.nodeSourceType)
                            }
                        } else {
                            onBucketClicked(item)
                        }
                    },
                    onMenuClicked = {
                        item.firstNode?.let { node ->
                            onMenuClicked(node, item.nodeSourceType)
                        }
                    }
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun RecentsLazyListViewPreview() {
    AndroidThemeForPreviews {
        RecentsLazyListView(
            items = mockRecentsUiItemList(),
            onFileClicked = { _, _ -> },
            onBucketClicked = { },
            onMenuClicked = { _, _ -> },
        )
    }
}

