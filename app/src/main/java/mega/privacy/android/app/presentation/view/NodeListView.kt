package mega.privacy.android.app.presentation.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.view.extension.folderInfo
import mega.privacy.android.app.presentation.view.extension.getIcon
import mega.privacy.android.app.presentation.view.previewdataprovider.SampleFolderNodeDataProvider
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem
import mega.privacy.android.legacy.core.ui.controls.lists.NodeListViewItem
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
 * @param listState the state of the list
 * @param showMediaDiscoveryButton whether to show media discovery button
 * @param modifier
 * @param showChangeViewType whether to show change view type button
 */
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
    showChangeViewType: Boolean = true,
    isPublicNode: Boolean = false,
) {
    LazyColumn(state = listState, modifier = modifier) {
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
        items(count = nodeUIItemList.size,
            key = {
                nodeUIItemList[it].node.id.longValue
            }) {
            val nodeEntity = nodeUIItemList[it].node
            NodeListViewItem(
                isSelected = nodeUIItemList[it].isSelected,
                folderInfo = nodeEntity
                    .let { node -> node as? FolderNode }
                    ?.folderInfo(),
                icon = nodeEntity
                    .let { node -> node as? TypedFolderNode }
                    ?.getIcon()
                    ?: MimeTypeList.typeForName(nodeUIItemList[it].node.name).iconResourceId,
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
                            fileNode.modificationTime
                        )
                    },
                name = nodeEntity.name,
                showMenuButton = true,
                isTakenDown = nodeEntity.isTakenDown,
                isFavourite = nodeEntity.isFavourite,
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
            showChangeViewType = true
        )
    }
}