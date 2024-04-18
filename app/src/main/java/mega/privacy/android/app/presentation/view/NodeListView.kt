package mega.privacy.android.app.presentation.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.view.extension.folderInfo
import mega.privacy.android.app.presentation.view.extension.getIcon
import mega.privacy.android.app.presentation.view.previewdataprovider.SampleFolderNodeDataProvider
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.core.ui.controls.dividers.DividerType
import mega.privacy.android.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.core.ui.controls.lists.NodeListViewItem
import mega.privacy.android.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.shares.ShareFolderNode
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem
import mega.privacy.android.shared.theme.MegaAppTheme
import nz.mega.sdk.MegaNode

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
    modifier: Modifier = Modifier,
    showLinkIcon: Boolean = true,
    showChangeViewType: Boolean = true,
    isPublicNode: Boolean = false,
    showPublicLinkCreationTime: Boolean = false,
    listContentPadding: PaddingValues = PaddingValues(0.dp),
    fileTypeIconMapper: FileTypeIconMapper,
    inSelectionMode: Boolean = false,
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
                    modifier = Modifier.padding(horizontal = 8.dp),
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
                title = getNodeTitle(nodeUiItem.node),
                titleOverflow = LongTextBehaviour.MiddleEllipsis,
                subtitle = getNodeItemDescription(
                    nodeEntity = nodeUiItem.node,
                    showPublicLinkCreationTime = showPublicLinkCreationTime
                ),
                icon = getNodeItemThumbnail(
                    nodeEntity = nodeUiItem.node,
                    fileTypeIconMapper = fileTypeIconMapper,
                ),
                thumbnailData = ThumbnailRequest(nodeUiItem.id, isPublicNode),
                isSelected = nodeUiItem.isSelected,
                onMoreClicked = { onMenuClick(nodeUiItem) }.takeUnless { _ -> inSelectionMode },
                onItemClicked = { onItemClicked(nodeUiItem) },
                onLongClick = { onLongClick(nodeUiItem) },
                accessPermissionIcon = getSharesIcon(nodeUiItem.node as? ShareFolderNode),
                labelColor = getNodeLabel(nodeUiItem.node),
                showOffline = nodeUiItem.isAvailableOffline,
                showLink = showLinkIcon && nodeUiItem.exportedData != null,
                showFavourite = nodeUiItem.isFavourite && nodeUiItem.isIncomingShare.not(),
                showIsVerified = nodeUiItem.isIncomingShare && (nodeUiItem.node as? ShareFolderNode)?.shareData?.isContactCredentialsVerified == true,
                showVersion = nodeUiItem.hasVersion,
            )
            MegaDivider(dividerType = DividerType.BigStartPadding)
        }
    }
}

@Composable
private fun <T : TypedNode> getNodeItemDescription(
    nodeEntity: T,
    showPublicLinkCreationTime: Boolean,
) = getSharedNodeItemDescription(nodeEntity = nodeEntity) ?: when (nodeEntity) {
    is FileNode -> formatFileSize(nodeEntity.size, LocalContext.current)
        .plus(" · ")
        .plus(
            formatModifiedDate(
                java.util.Locale(
                    Locale.current.language, Locale.current.region
                ),
                if (showPublicLinkCreationTime) {
                    nodeEntity.exportedData?.publicLinkCreationTime
                        ?: nodeEntity.modificationTime
                } else nodeEntity.modificationTime
            )
        )

    is FolderNode -> nodeEntity.folderInfo()
    else -> ""
}

@Composable
private fun <T : TypedNode> getSharedNodeItemDescription(
    nodeEntity: T,
) = (nodeEntity as? ShareFolderNode)?.shareData?.let { shareData ->
    when (val count = shareData.count) {
        0 -> if (!shareData.isVerified) shareData.user else null
        1 -> if (shareData.isVerified) shareData.userFullName else null
        else -> pluralStringResource(
            id = R.plurals.general_num_shared_with,
            count = count,
            count
        )
    }
}


@Composable
private fun <T : TypedNode> getNodeItemThumbnail(
    nodeEntity: T,
    fileTypeIconMapper: FileTypeIconMapper,
) = when (nodeEntity) {
    is TypedFolderNode -> nodeEntity.getIcon(fileTypeIconMapper)
    is TypedFileNode -> fileTypeIconMapper(nodeEntity.type.extension)
    else -> mega.privacy.android.icon.pack.R.drawable.ic_generic_medium_solid
}

@Composable
private fun getSharesIcon(nodeEntity: ShareFolderNode?): Int? =
    nodeEntity?.shareData?.let { shareData ->
        if (shareData.isUnverifiedDistinctNode) {
            mega.privacy.android.core.R.drawable.ic_alert_triangle
        } else if (nodeEntity.node.isIncomingShare) {
            when (shareData.access) {
                AccessPermission.FULL -> R.drawable.ic_shared_fullaccess
                AccessPermission.READWRITE -> R.drawable.ic_shared_read_write
                else -> R.drawable.ic_shared_read
            }
        } else null
    }

@Composable
private fun <T : TypedNode> getNodeTitle(
    nodeEntity: T,
): String {
    val isUnverifiedShare =
        (nodeEntity as? ShareFolderNode)?.shareData?.isUnverifiedDistinctNode == true
    return if (nodeEntity.isIncomingShare && isUnverifiedShare && nodeEntity.isNodeKeyDecrypted.not())
        stringResource(id = R.string.shared_items_verify_credentials_undecrypted_folder)
    else nodeEntity.name
}

@Composable
private fun <T : TypedNode> getNodeLabel(nodeEntity: T) =
    if (nodeEntity.label != MegaNode.NODE_LBL_UNKNOWN)
        colorResource(
            id = MegaNodeUtil.getNodeLabelColor(
                nodeEntity.label
            )
        ) else null

@CombinedThemePreviews
@Composable
private fun NodeListViewPreview(
    @PreviewParameter(SampleFolderNodeDataProvider::class) items: List<NodeUIItem<TypedFolderNode>>,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
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