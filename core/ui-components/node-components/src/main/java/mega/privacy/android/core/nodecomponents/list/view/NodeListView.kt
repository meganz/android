package mega.privacy.android.core.nodecomponents.list.view

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
import mega.privacy.android.core.nodecomponents.extension.getNodeItemDescription
import mega.privacy.android.core.nodecomponents.extension.getNodeItemThumbnail
import mega.privacy.android.core.nodecomponents.extension.getSharesIcon
import mega.privacy.android.core.nodecomponents.list.model.NodeUiItem
import mega.privacy.android.core.nodecomponents.list.view.previewdata.FolderNodePreviewDataProvider
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.shares.ShareFolderNode
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest

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
    shouldApplySensitiveMode: Boolean,
    fileTypeIconMapper: FileTypeIconMapper,
    modifier: Modifier = Modifier,
    highlightText: String = "",
    showLinkIcon: Boolean = true,
    showChangeViewType: Boolean = true,
    isPublicNode: Boolean = false,
    showPublicLinkCreationTime: Boolean = false,
    listContentPadding: PaddingValues = PaddingValues(0.dp),
    inSelectionMode: Boolean = false,
    isContactVerificationOn: Boolean = false,
    nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
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
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
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
                nodeUiItemList[it].uniqueKey
            }
        ) {
            val nodeUiItem = nodeUiItemList[it]
            NodeListViewItem(
                title = nodeUiItem.node.name, // TODO: Implement title logic
                subtitle = nodeUiItem.node.getNodeItemDescription(showPublicLinkCreationTime),
                description = nodeUiItem.node.description?.replace("\n", " "),
                tags = nodeUiItem.node.tags.takeIf { nodeSourceType != NodeSourceType.RUBBISH_BIN },
                icon = nodeUiItem.node.getNodeItemThumbnail(fileTypeIconMapper = fileTypeIconMapper),
                thumbnailData = ThumbnailRequest(nodeUiItem.id, isPublicNode),
                isSelected = nodeUiItem.isSelected,
                onMoreClicked = { onMenuClick(nodeUiItem) }.takeUnless { _ -> inSelectionMode },
                onItemClicked = { onItemClicked(nodeUiItem) },
                onLongClicked = { onLongClick(nodeUiItem) },
                accessPermissionIcon = (nodeUiItem.node as? ShareFolderNode)
                    .getSharesIcon(isContactVerificationOn),
                labelColor = null, // TODO: Implement label color logic
                highlightText = highlightText,
                showOffline = nodeUiItem.isAvailableOffline,
                showLink = showLinkIcon && nodeUiItem.exportedData != null,
                showFavourite = nodeUiItem.isFavourite && nodeUiItem.isIncomingShare.not(),
                showIsVerified = isContactVerificationOn && nodeUiItem.isIncomingShare && (nodeUiItem.node as? ShareFolderNode)?.shareData?.isContactCredentialsVerified == true,
                showVersion = nodeUiItem.hasVersion,
                isTakenDown = nodeUiItem.isTakenDown,
                isInSelectionMode = inSelectionMode,
                isSensitive = nodeSourceType != NodeSourceType.INCOMING_SHARES
                        && nodeSourceType != NodeSourceType.OUTGOING_SHARES
                        && nodeSourceType != NodeSourceType.LINKS
                        && shouldApplySensitiveMode && (nodeUiItem.isMarkedSensitive || nodeUiItem.isSensitiveInherited),
                showBlurEffect = (nodeUiItem.node as? FileNode)?.type?.let { fileTypeInfo ->
                    fileTypeInfo is ImageFileTypeInfo || fileTypeInfo is VideoFileTypeInfo || fileTypeInfo is PdfFileTypeInfo || fileTypeInfo is AudioFileTypeInfo
                } ?: false,
                isHighlighted = nodeUiItem.isHighlighted,
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
            shouldApplySensitiveMode = false,
            modifier = Modifier,
            showChangeViewType = true,
            fileTypeIconMapper = FileTypeIconMapper()
        )
    }
}