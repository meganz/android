package mega.privacy.android.app.presentation.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
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
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.view.extension.folderInfo
import mega.privacy.android.app.presentation.view.extension.getIcon
import mega.privacy.android.app.presentation.view.previewdataprovider.SampleFolderNodeDataProvider
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.shares.ShareFolderNode
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem
import mega.privacy.android.legacy.core.ui.controls.lists.NodeListViewItem
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
            val nodeEntity = nodeUIItemList[it].node

            var nodeName = nodeEntity.name
            // Process data for shares
            var shareSubtitle: String? = null
            var isUnverifiedShare = false
            var sharesIcon: Int? = null
            var verifiedIcon: Int? = null
            (nodeEntity as? ShareFolderNode)?.shareData?.let { shareData ->
                val count = shareData.count
                shareSubtitle = when (count) {
                    0 -> if (!shareData.isVerified) shareData.user else null
                    1 -> if (shareData.isVerified) shareData.userFullName else null
                    else -> pluralStringResource(
                        id = R.plurals.general_num_shared_with,
                        count = count,
                        count
                    )
                }
                isUnverifiedShare = shareData.isUnverifiedDistinctNode
                sharesIcon = if (isUnverifiedShare) {
                    mega.privacy.android.core.R.drawable.ic_alert_triangle
                } else if (nodeEntity.node.isIncomingShare) {
                    when (shareData.access) {
                        AccessPermission.FULL -> R.drawable.ic_shared_fullaccess
                        AccessPermission.READWRITE -> R.drawable.ic_shared_read_write
                        else -> R.drawable.ic_shared_read
                    }
                } else null

                if (nodeEntity.isIncomingShare) {
                    if (isUnverifiedShare && !nodeEntity.isNodeKeyDecrypted) {
                        nodeName =
                            stringResource(id = R.string.shared_items_verify_credentials_undecrypted_folder)
                    }

                    if (shareData.isContactCredentialsVerified) {
                        verifiedIcon = R.drawable.ic_verified
                    }
                }
            }

            NodeListViewItem(
                isSelected = nodeUIItemList[it].isSelected,
                folderInfo = nodeEntity
                    .let { node -> node as? FolderNode }
                    ?.folderInfo(),
                icon = when (nodeEntity) {
                    is TypedFolderNode -> {
                        nodeEntity.getIcon(fileTypeIconMapper)
                    }

                    is TypedFileNode -> {
                        fileTypeIconMapper(nodeEntity.type.extension)
                    }

                    else -> IconPackR.drawable.ic_generic_medium_solid
                },
                fileSize = nodeEntity
                    .let { node -> node as? FileNode }
                    ?.let { file -> formatFileSize(file.size, LocalContext.current) },
                modifiedDate = nodeEntity
                    .let { node -> node as? FileNode }
                    ?.let { fileNode ->
                        formatModifiedDate(
                            java.util.Locale(
                                Locale.current.language, Locale.current.region
                            ),
                            if (showPublicLinkCreationTime)
                                fileNode.exportedData?.publicLinkCreationTime
                                    ?: fileNode.modificationTime
                            else
                                fileNode.modificationTime
                        )
                    },
                name = nodeName,
                sharesSubtitle = shareSubtitle,
                isUnverifiedShare = isUnverifiedShare,
                sharesIcon = sharesIcon,
                verifiedIcon = verifiedIcon,
                showMenuButton = true,
                showLinkIcon = showLinkIcon,
                isTakenDown = nodeEntity.isTakenDown,
                isFavourite = nodeEntity.isFavourite && !nodeEntity.isIncomingShare,
                isSharedWithPublicLink = nodeEntity.exportedData != null,
                thumbnailData = ThumbnailRequest(nodeEntity.id, isPublicNode),
                onClick = { onItemClicked(nodeUIItemList[it]) },
                onLongClick = { onLongClick(nodeUIItemList[it]) },
                onMenuClick = { onMenuClick(nodeUIItemList[it]) },
                labelColor = if (nodeEntity.label != MegaNode.NODE_LBL_UNKNOWN)
                    colorResource(
                        id = MegaNodeUtil.getNodeLabelColor(
                            nodeEntity.label
                        )
                    ) else null,
                nodeAvailableOffline = nodeUIItemList[it].isAvailableOffline
            )
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 72.dp),
                color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
                thickness = 1.dp
            )
        }
    }
}

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