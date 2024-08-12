package mega.privacy.android.app.presentation.documentsection.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.documentsection.model.DocumentUiEntity
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem
import mega.privacy.android.shared.original.core.ui.controls.lists.NodeGridViewItem

@Composable
internal fun DocumentGridView(
    items: List<DocumentUiEntity>,
    accountType: AccountType?,
    lazyGridState: LazyGridState,
    sortOrder: String,
    modifier: Modifier,
    onChangeViewTypeClick: () -> Unit,
    onClick: (item: DocumentUiEntity, index: Int) -> Unit,
    onMenuClick: (DocumentUiEntity) -> Unit,
    onSortOrderClick: () -> Unit,
    spanCount: Int = 2,
    isSelectionMode: Boolean = false,
    onLongClick: ((item: DocumentUiEntity, index: Int) -> Unit) = { _, _ -> },
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
            val documentItem = items[it]
            NodeGridViewItem(
                modifier = Modifier
                    .testTag("$DOCUMENT_SECTION_GRID_ITEM_VIEW_TEST_TAG$it")
                    .alpha(0.5f.takeIf {
                        accountType?.isPaid == true && (documentItem.isMarkedSensitive || documentItem.isSensitiveInherited)
                    } ?: 1f),
                isSensitive = accountType?.isPaid == true && (documentItem.isMarkedSensitive || documentItem.isSensitiveInherited),
                showBlurEffect = true,
                isSelected = documentItem.isSelected,
                name = documentItem.name,
                iconRes = documentItem.icon,
                thumbnailData = if (documentItem.thumbnail?.exists() == true) {
                    documentItem.thumbnail
                } else {
                    ThumbnailRequest(documentItem.id)
                },
                isTakenDown = documentItem.isTakenDown,
                onClick = { onClick(documentItem, it) },
                onMenuClick = { onMenuClick(documentItem) }.takeIf { !isSelectionMode },
                onLongClick = { onLongClick(documentItem, it) },
                isFolderNode = false,
                duration = null
            )
        }
    }
}

/**
 * Test tag for the gird item view.
 */
const val DOCUMENT_SECTION_GRID_ITEM_VIEW_TEST_TAG = "document_section_grid:item_view"