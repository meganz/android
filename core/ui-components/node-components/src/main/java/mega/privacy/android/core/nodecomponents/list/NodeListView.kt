package mega.privacy.android.core.nodecomponents.list

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyColumn
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.core.nodecomponents.list.previewdata.FolderNodePreviewDataProvider
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * This method will show [NodeUiItem] in vertical list using [ThumbnailRequest] to load thumbnails
 *
 * @param nodeUiItemList list of [NodeUiItem] to show
 * @param onMenuClick callback to handle menu click
 * @param onItemClicked callback to handle item click
 * @param onLongClick callback to handle long click
 * @param onEnterMediaDiscoveryClick callback to handle media discovery click
 * @param sortOrder the sort order of the list
 * @param onSortOrderClick callback to handle sort order click
 * @param onChangeViewTypeClick callback to handle change view type click
 * @param showSortOrder whether to show change sort order button
 * @param showLinkIcon whether to show public share link icon
 * @param listState the state of the list
 * @param showMediaDiscoveryButton whether to show media discovery button
 * @param modifier
 * @param showChangeViewType whether to show change view type button
 * @param listContentPadding the content padding of the list/lazyColumn
 * @param isContactVerificationOn whether contact verification is enabled
 */
@Composable
fun <T : TypedNode> NodeListView(
    nodeUiItemList: List<NodeUiItem<T>>,
    onMenuClick: (NodeUiItem<T>) -> Unit,
    onItemClicked: (NodeUiItem<T>) -> Unit,
    onLongClick: (NodeUiItem<T>) -> Unit,
    onEnterMediaDiscoveryClick: () -> Unit,
    sortOrder: String,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    showSortOrder: Boolean,
    listState: LazyListState,
    showMediaDiscoveryButton: Boolean,
    modifier: Modifier = Modifier,
    highlightText: String = "",
    showLinkIcon: Boolean = true,
    showChangeViewType: Boolean = true,
    listContentPadding: PaddingValues = PaddingValues(0.dp),
    inSelectionMode: Boolean = false,
    isContactVerificationOn: Boolean = false,
) {
    FastScrollLazyColumn(
        state = listState,
        totalItems = nodeUiItemList.size,
        modifier = modifier.semantics { testTagsAsResourceId = true },
        contentPadding = listContentPadding
    ) {
        if (showSortOrder || showChangeViewType) {
            item(key = "header") {
                NodeHeaderItem(
                    modifier = Modifier
                        .padding(horizontal = DSTokens.spacings.s3)
                        .padding(bottom = DSTokens.spacings.s3),
                    onSortOrderClick = onSortOrderClick,
                    onChangeViewTypeClick = onChangeViewTypeClick,
                    onEnterMediaDiscoveryClick = onEnterMediaDiscoveryClick,
                    sortOrder = sortOrder,
                    isListView = true,
                    showSortOrder = showSortOrder,
                    showChangeViewType = showChangeViewType,
                    showMediaDiscoveryButton = showMediaDiscoveryButton,
                )
            }
        }

        items(
            count = nodeUiItemList.size,
            key = {
                nodeUiItemList[it].id.longValue
            }
        ) {
            NodeListViewItem(
                nodeUiItem = nodeUiItemList[it],
                isInSelectionMode = inSelectionMode,
                highlightText = highlightText,
                onMoreClicked = onMenuClick,
                onItemClicked = onItemClicked,
                onLongClicked = onLongClick,
            )
        }
    }
}


@CombinedThemePreviews
@Composable
private fun NodeListViewPreview(
    @PreviewParameter(FolderNodePreviewDataProvider::class) items: List<NodeUiItem<TypedFolderNode>>,
) {
    AndroidThemeForPreviews {
        NodeListView(
            nodeUiItemList = items,
            onMenuClick = {},
            onItemClicked = {},
            onLongClick = {},
            onEnterMediaDiscoveryClick = {},
            sortOrder = "",
            onSortOrderClick = {},
            onChangeViewTypeClick = {},
            showSortOrder = true,
            listState = LazyListState(),
            showMediaDiscoveryButton = false,
            modifier = Modifier,
            showChangeViewType = true
        )
    }
}