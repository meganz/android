package mega.privacy.android.app.presentation.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.view.extension.getIcon
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem
import mega.privacy.android.shared.original.core.ui.controls.lists.NodeGridViewItem

/**
This method will show [NodeUIItem] in Grid manner based on span and getting thumbnail using [ThumbnailRequest]
 *
 * @param nodeUIItems List of [NodeUIItem]
 * @param onMenuClick three dots click
 * @param onItemClicked on item click
 * @param onLongClick on long item click
 * @param onEnterMediaDiscoveryClick on enter media discovery click
 * @param sortOrder the sort order of the list
 * @param onSortOrderClick on sort order click
 * @param onChangeViewTypeClick on change view type click
 * @param showSortOrder whether to show change sort order button
 * @param gridState the state of the grid
 * @param showMediaDiscoveryButton whether to show media discovery button
 * @param modifier
 * @param spanCount the span count of the grid
 * @param showChangeViewType whether to show change view type button
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T : TypedNode> NodeGridView(
    nodeUIItems: List<NodeUIItem<T>>,
    onMenuClick: (NodeUIItem<T>) -> Unit,
    onItemClicked: (NodeUIItem<T>) -> Unit,
    onLongClick: (NodeUIItem<T>) -> Unit,
    onEnterMediaDiscoveryClick: () -> Unit,
    sortOrder: String,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    showSortOrder: Boolean,
    gridState: LazyGridState,
    showMediaDiscoveryButton: Boolean,
    fileTypeIconMapper: FileTypeIconMapper,
    modifier: Modifier = Modifier,
    spanCount: Int = 2,
    showChangeViewType: Boolean = true,
    isPublicNode: Boolean = false,
    inSelectionMode: Boolean = false,
    listContentPadding: PaddingValues = PaddingValues(0.dp),
    accountType: AccountType? = null,
    nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(spanCount),
        modifier = modifier
            .padding(horizontal = 4.dp)
            .semantics { testTagsAsResourceId = true },
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = listContentPadding
    ) {
        if (showSortOrder || showChangeViewType) {
            item(
                key = "header",
                span = {
                    GridItemSpan(currentLineSpan = spanCount)
                }
            ) {
                HeaderViewItem(
                    modifier = modifier.padding(bottom = 4.dp),
                    onSortOrderClick = onSortOrderClick,
                    onChangeViewTypeClick = onChangeViewTypeClick,
                    onEnterMediaDiscoveryClick = onEnterMediaDiscoveryClick,
                    sortOrder = sortOrder,
                    isListView = false,
                    showSortOrder = showSortOrder,
                    showChangeViewType = showChangeViewType,
                    showMediaDiscoveryButton = showMediaDiscoveryButton,
                )
            }
        }
        items(
            count = nodeUIItems.size,
            key = {
                if (nodeUIItems[it].isInvisible) {
                    it
                } else {
                    nodeUIItems[it].uniqueKey
                }
            },
        ) {
            val node = nodeUIItems[it].node
            NodeGridViewItem(
                isSelected = nodeUIItems[it].isSelected,
                name = nodeUIItems[it].node.name,
                iconRes = nodeUIItems[it].node.getIcon(fileTypeIconMapper = fileTypeIconMapper),
                thumbnailData = ThumbnailRequest(nodeUIItems[it].node.id, isPublicNode),
                duration = nodeUIItems[it].fileDuration,
                isTakenDown = nodeUIItems[it].isTakenDown,
                onClick = { onItemClicked(nodeUIItems[it]) },
                onLongClick = { onLongClick(nodeUIItems[it]) },
                onMenuClick = { onMenuClick(nodeUIItems[it]) }.takeIf { !inSelectionMode },
                isVideoNode = (nodeUIItems[it].node as? FileNode)?.type is VideoFileTypeInfo,
                isFolderNode = nodeUIItems[it].node is TypedFolderNode,
                inVisible = nodeUIItems[it].isInvisible,
                isSensitive = nodeSourceType != NodeSourceType.INCOMING_SHARES
                        && nodeSourceType != NodeSourceType.OUTGOING_SHARES
                        && nodeSourceType != NodeSourceType.LINKS
                        && accountType?.isPaid == true && (node.isMarkedSensitive || node.isSensitiveInherited),
                showBlurEffect = (nodeUIItems[it].node as? FileNode)?.type?.let { fileTypeInfo ->
                    fileTypeInfo is ImageFileTypeInfo || fileTypeInfo is VideoFileTypeInfo || fileTypeInfo is PdfFileTypeInfo || fileTypeInfo is AudioFileTypeInfo
                } ?: false,
            )
        }
    }
}
