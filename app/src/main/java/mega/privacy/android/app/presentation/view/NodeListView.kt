package mega.privacy.android.app.presentation.view

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.view.extension.fileSize
import mega.privacy.android.app.presentation.view.extension.folderInfo
import mega.privacy.android.app.presentation.view.extension.formattedModifiedDate
import mega.privacy.android.app.presentation.view.extension.getIcon
import mega.privacy.android.core.ui.controls.NodeListViewItem
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import java.io.File

/**
 * This method will show [NodeUIItem] in vertical list
 * @param modifier
 * @param nodeUIItemList
 * @param onMenuClick
 * @param onItemClicked
 * @param onLongClick
 * @param sortOrder
 * @param onSortOrderClick
 * @param onChangeViewTypeClick
 * @param getThumbnail
 */
@Composable
fun <T : TypedNode> NodeListView(
    nodeUIItemList: List<NodeUIItem<T>>,
    onMenuClick: (NodeUIItem<T>) -> Unit,
    onItemClicked: (NodeUIItem<T>) -> Unit,
    onLongClick: (NodeUIItem<T>) -> Unit,
    sortOrder: String,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    showSortOrder: Boolean,
    listState: LazyListState,
    getThumbnail: ((handle: Long, onFinished: (file: File?) -> Unit) -> Unit),
    modifier: Modifier = Modifier,
    showChangeViewType: Boolean = true,
) {
    LazyColumn(state = listState, modifier = modifier) {
        if (showSortOrder || showChangeViewType) {
            item(
                key = "header"
            ) {
                HeaderViewItem(
                    modifier = modifier,
                    onSortOrderClick = onSortOrderClick,
                    onChangeViewTypeClick = onChangeViewTypeClick,
                    sortOrder = sortOrder,
                    isListView = true,
                    showSortOrder = showSortOrder,
                    showChangeViewType = showChangeViewType
                )
            }
        }
        items(count = nodeUIItemList.size,
            key = {
                nodeUIItemList[it].node.id.longValue
            }) {
            val nodeEntity = nodeUIItemList[it].node
            val imageState = produceState<File?>(initialValue = null) {
                getThumbnail(nodeEntity.id.longValue) { file ->
                    value = file
                }
            }
            NodeListViewItem(
                modifier = modifier,
                isSelected = nodeUIItemList[it].isSelected,
                folderInfo = nodeEntity
                    .let { node -> node as? FolderNode }
                    ?.folderInfo(),
                icon = nodeEntity
                    .let { node -> node as? FolderNode }
                    ?.getIcon()
                    ?: MimeTypeList.typeForName(nodeUIItemList[it].node.name).iconResourceId,
                fileSize = nodeEntity
                    .let { node -> node as? FileNode }
                    ?.fileSize(),
                modifiedDate = nodeEntity
                    .let { node -> node as? FileNode }
                    ?.formattedModifiedDate(
                        java.util.Locale(
                            Locale.current.language, Locale.current.region
                        )
                    ),
                name = nodeEntity.name,
                isTakenDown = nodeEntity.isTakenDown,
                isFavourite = nodeEntity.isFavourite,
                isSharedWithPublicLink = nodeEntity.exportedData != null,
                onMenuClick = { onMenuClick(nodeUIItemList[it]) },
                onItemClicked = { onItemClicked(nodeUIItemList[it]) },
                onLongClick = { onLongClick(nodeUIItemList[it]) },
                imageState = imageState
            )
        }
    }
}