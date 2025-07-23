package mega.privacy.android.core.nodecomponents.list.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyVerticalGrid
import mega.privacy.android.core.nodecomponents.extension.getIcon
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest

/**
This method will show [NodeUIItem] in Grid manner based on span and getting thumbnail using [ThumbnailRequest]
 *
 * @param nodeUiItems List of [NodeUIItem]
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
    nodeUiItems: List<NodeUiItem<T>>,
    onMenuClick: (NodeUiItem<T>) -> Unit,
    onItemClicked: (NodeUiItem<T>) -> Unit,
    onLongClick: (NodeUiItem<T>) -> Unit,
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
    shouldApplySensitiveMode: Boolean = false,
    listContentPadding: PaddingValues = PaddingValues(0.dp),
    nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
) {
    FastScrollLazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(spanCount),
        totalItems = nodeUiItems.size,
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
                NodeHeaderItem(
                    modifier = modifier.padding(bottom = 12.dp),
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
            count = nodeUiItems.size,
            key = {
                if (nodeUiItems[it].isInvisible) {
                    it
                } else {
                    nodeUiItems[it].uniqueKey
                }
            },
        ) {
            val node = nodeUiItems[it].node
            NodeGridViewItem(
                isSelected = nodeUiItems[it].isSelected,
                name = nodeUiItems[it].node.name,
                iconRes = nodeUiItems[it].node.getIcon(fileTypeIconMapper = fileTypeIconMapper),
                thumbnailData = ThumbnailRequest(nodeUiItems[it].node.id, isPublicNode),
                duration = nodeUiItems[it].fileDuration,
                isTakenDown = nodeUiItems[it].isTakenDown,
                onClick = { onItemClicked(nodeUiItems[it]) },
                onLongClick = { onLongClick(nodeUiItems[it]) },
                onMenuClick = { onMenuClick(nodeUiItems[it]) },
                isInSelectionMode = inSelectionMode,
                isVideoNode = (nodeUiItems[it].node as? FileNode)?.type is VideoFileTypeInfo,
                isFolderNode = nodeUiItems[it].node is TypedFolderNode,
                isInvisible = nodeUiItems[it].isInvisible,
                isSensitive = nodeSourceType != NodeSourceType.INCOMING_SHARES
                        && nodeSourceType != NodeSourceType.OUTGOING_SHARES
                        && nodeSourceType != NodeSourceType.LINKS
                        && shouldApplySensitiveMode && (node.isMarkedSensitive || node.isSensitiveInherited),
                showBlurEffect = (nodeUiItems[it].node as? FileNode)?.type?.let { fileTypeInfo ->
                    fileTypeInfo is ImageFileTypeInfo || fileTypeInfo is VideoFileTypeInfo || fileTypeInfo is PdfFileTypeInfo || fileTypeInfo is AudioFileTypeInfo
                } ?: false,
                isHighlighted = nodeUiItems[it].isHighlighted,
            )
        }
    }
}
