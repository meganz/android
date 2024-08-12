package mega.privacy.android.app.presentation.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.view.extension.getNodeItemDescription
import mega.privacy.android.app.presentation.view.extension.getNodeItemThumbnail
import mega.privacy.android.app.presentation.view.extension.getNodeLabel
import mega.privacy.android.app.presentation.view.extension.getNodeTitle
import mega.privacy.android.app.presentation.view.extension.getSharesIcon
import mega.privacy.android.app.presentation.view.previewdataprovider.SampleFolderNodeDataProvider
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
import mega.privacy.android.domain.entity.node.shares.ShareFolderNode
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.lists.NodeListViewItem
import mega.privacy.android.shared.original.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * This method will show [NodeUIItem] in vertical list using [ThumbnailRequest] to load thumbnails
 *
 * @param nodeUIItemList list of [NodeUIItem] to show
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
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T : TypedNode> NodeListView(
    nodeUIItemList: List<NodeUIItem<T>>,
    onMenuClick: (NodeUIItem<T>) -> Unit,
    onItemClicked: (NodeUIItem<T>) -> Unit,
    onLongClick: (NodeUIItem<T>) -> Unit,
    onEnterMediaDiscoveryClick: () -> Unit,
    sortOrder: String,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    showSortOrder: Boolean,
    listState: LazyListState,
    showMediaDiscoveryButton: Boolean,
    fileTypeIconMapper: FileTypeIconMapper,
    modifier: Modifier = Modifier,
    highlightText: String = "",
    showLinkIcon: Boolean = true,
    showChangeViewType: Boolean = true,
    isPublicNode: Boolean = false,
    showPublicLinkCreationTime: Boolean = false,
    listContentPadding: PaddingValues = PaddingValues(0.dp),
    inSelectionMode: Boolean = false,
    accountType: AccountType? = null,
    nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.semantics { testTagsAsResourceId = true },
        contentPadding = listContentPadding
    ) {
        if (showSortOrder || showChangeViewType) {
            item(
                key = "header"
            ) {
                HeaderViewItem(
                    modifier = Modifier.padding(8.dp),
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
            count = nodeUIItemList.size,
            key = {
                nodeUIItemList[it].uniqueKey
            }
        ) {
            val nodeUiItem = nodeUIItemList[it]
            NodeListViewItem(
                title = nodeUiItem.node.getNodeTitle(),
                titleOverflow = LongTextBehaviour.MiddleEllipsis,
                subtitle = nodeUiItem.node.getNodeItemDescription(
                    showPublicLinkCreationTime = showPublicLinkCreationTime
                ),
                description = nodeUiItem.node.description,
                icon = nodeUiItem.node.getNodeItemThumbnail(fileTypeIconMapper = fileTypeIconMapper),
                thumbnailData = ThumbnailRequest(nodeUiItem.id, isPublicNode),
                isSelected = nodeUiItem.isSelected,
                onMoreClicked = { onMenuClick(nodeUiItem) }.takeUnless { _ -> inSelectionMode },
                onItemClicked = { onItemClicked(nodeUiItem) },
                onLongClick = { onLongClick(nodeUiItem) },
                accessPermissionIcon = (nodeUiItem.node as? ShareFolderNode).getSharesIcon(),
                labelColor = nodeUiItem.node.getNodeLabel(),
                highlightText = highlightText,
                showOffline = nodeUiItem.isAvailableOffline,
                showLink = showLinkIcon && nodeUiItem.exportedData != null,
                showFavourite = nodeUiItem.isFavourite && nodeUiItem.isIncomingShare.not(),
                showIsVerified = nodeUiItem.isIncomingShare && (nodeUiItem.node as? ShareFolderNode)?.shareData?.isContactCredentialsVerified == true,
                showVersion = nodeUiItem.hasVersion,
                isTakenDown = nodeUiItem.isTakenDown,
                isSensitive = nodeSourceType != NodeSourceType.INCOMING_SHARES
                        && nodeSourceType != NodeSourceType.OUTGOING_SHARES
                        && nodeSourceType != NodeSourceType.LINKS
                        && accountType?.isPaid == true && (nodeUiItem.isMarkedSensitive || nodeUiItem.isSensitiveInherited),
                showBlurEffect = (nodeUiItem.node as? FileNode)?.type?.let { fileTypeInfo ->
                    fileTypeInfo is ImageFileTypeInfo || fileTypeInfo is VideoFileTypeInfo || fileTypeInfo is PdfFileTypeInfo || fileTypeInfo is AudioFileTypeInfo
                } ?: false,
            )
            MegaDivider(dividerType = DividerType.BigStartPadding)
        }
    }
}


@CombinedThemePreviews
@Composable
private fun NodeListViewPreview(
    @PreviewParameter(SampleFolderNodeDataProvider::class) items: List<NodeUIItem<TypedFolderNode>>,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        NodeListView(
            nodeUIItemList = items,
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
            showChangeViewType = true,
            fileTypeIconMapper = FileTypeIconMapper()
        )
    }
}