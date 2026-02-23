package mega.privacy.android.app.presentation.videosection.view.videoselected

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.view.extension.getNodeTitle
import mega.privacy.android.core.nodecomponents.extension.getIcon
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
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem
import mega.privacy.android.shared.original.core.ui.controls.layouts.FastScrollLazyVerticalGrid
import mega.privacy.android.shared.original.core.ui.controls.lists.NodeGridViewItem

@Composable
fun <T : TypedNode> VideoSelectedNodeGridView(
    nodeUIItems: List<NodeUIItem<T>>,
    onItemClicked: (NodeUIItem<T>) -> Unit,
    sortOrder: String,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    showSortOrder: Boolean,
    gridState: LazyGridState,
    fileTypeIconMapper: FileTypeIconMapper,
    modifier: Modifier = Modifier,
    spanCount: Int = 2,
    showChangeViewType: Boolean = true,
    isPublicNode: Boolean = false,
    shouldApplySensitiveMode: Boolean = false,
    listContentPadding: PaddingValues = PaddingValues(0.dp),
    nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
) {
    FastScrollLazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(spanCount),
        totalItems = nodeUIItems.size,
        modifier = modifier
            .padding(horizontal = 8.dp)
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
                    modifier = modifier.padding(bottom = 12.dp),
                    onSortOrderClick = onSortOrderClick,
                    onChangeViewTypeClick = onChangeViewTypeClick,
                    onEnterMediaDiscoveryClick = {},
                    sortOrder = sortOrder,
                    isListView = false,
                    showSortOrder = showSortOrder,
                    showChangeViewType = showChangeViewType,
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
                name = nodeUIItems[it].node.getNodeTitle(),
                iconRes = nodeUIItems[it].node.getIcon(fileTypeIconMapper = fileTypeIconMapper),
                thumbnailData = ThumbnailRequest(nodeUIItems[it].node.id, isPublicNode),
                duration = nodeUIItems[it].fileDuration,
                isTakenDown = nodeUIItems[it].isTakenDown,
                onClick = { onItemClicked(nodeUIItems[it]) },
                isVideoNode = (nodeUIItems[it].node as? FileNode)?.type is VideoFileTypeInfo,
                isFolderNode = nodeUIItems[it].node is TypedFolderNode,
                isInvisible = nodeUIItems[it].isInvisible,
                isSensitive = nodeSourceType != NodeSourceType.INCOMING_SHARES
                        && nodeSourceType != NodeSourceType.OUTGOING_SHARES
                        && nodeSourceType != NodeSourceType.LINKS
                        && shouldApplySensitiveMode && (node.isMarkedSensitive || node.isSensitiveInherited),
                showBlurEffect = (nodeUIItems[it].node as? FileNode)?.type?.let { fileTypeInfo ->
                    fileTypeInfo is ImageFileTypeInfo || fileTypeInfo is VideoFileTypeInfo || fileTypeInfo is PdfFileTypeInfo || fileTypeInfo is AudioFileTypeInfo
                } ?: false,
                isHighlighted = nodeUIItems[it].isHighlighted,
            )
        }
    }
}