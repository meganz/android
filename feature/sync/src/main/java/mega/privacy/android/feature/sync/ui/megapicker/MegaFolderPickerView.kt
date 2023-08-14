package mega.privacy.android.feature.sync.ui.megapicker

import mega.privacy.android.core.R as CoreUIR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.core.ui.controls.lists.HeaderViewItem
import mega.privacy.android.core.ui.controls.lists.NodeListViewItem
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.feature.sync.R
import java.io.File

@Composable
internal fun MegaFolderPickerView(
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    nodesList: List<TypedNode>,
    sortOrder: String,
    showSortOrder: Boolean,
    showChangeViewType: Boolean,
    listState: LazyListState,
    getThumbnail: ((handle: Long, onFinished: (file: File?) -> Unit) -> Unit),
    onFolderClick: (TypedNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(state = listState, modifier = modifier) {
        item(
            key = "header"
        ) {
            HeaderViewItem(
                onSortOrderClick = onSortOrderClick,
                onChangeViewTypeClick = onChangeViewTypeClick,
                onEnterMediaDiscoveryClick = {},
                sortOrder = sortOrder,
                isListView = true,
                showSortOrder = showSortOrder,
                showChangeViewType = showChangeViewType
            )
        }
        items(count = nodesList.size,
            key = {
                nodesList[it].id.longValue
            }) {
            val nodeEntity = nodesList[it]
            val imageState = produceState<File?>(initialValue = null) {
                getThumbnail(nodeEntity.id.longValue) { file ->
                    value = file
                }
            }

            NodeListViewItem(
                isSelected = false,
                folderInfo = (nodeEntity as? FolderNode)
                    ?.folderInfo(),
                icon = (nodeEntity as? FolderNode)
                    ?.getIcon()
                // In future, instead of this line we will get icon based on the file extension
                    ?: CoreUIR.drawable.ic_generic_list,
                fileSize = (nodeEntity as? FileNode)
                    ?.let { node -> formatFileSize(node.size, LocalContext.current) },
                modifiedDate = (nodeEntity as? FileNode)
                    ?.let { node ->
                        formatModifiedDate(
                            java.util.Locale(
                                Locale.current.language, Locale.current.region
                            ),
                            node.modificationTime
                        )
                    },
                name = nodeEntity.name,
                showMenuButton = false,
                isTakenDown = false,
                isFavourite = false,
                isSharedWithPublicLink = false,
                imageState = imageState,
                onClick = { onFolderClick(nodesList[it]) },
                isEnabled = nodeEntity is FolderNode,
            )
            Divider(
                modifier = Modifier
                    .fillMaxWidth(),
                color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
                thickness = 1.dp
            )
        }
    }
}

@Composable
@CombinedThemePreviews
private fun PreviewMegaFolderPickerView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MegaFolderPickerView(
            nodesList = SampleNodeDataProvider.values,
            sortOrder = "Name",
            onSortOrderClick = {},
            onChangeViewTypeClick = { },
            showSortOrder = true,
            listState = LazyListState(),
            getThumbnail = { _, _ -> },
            modifier = Modifier,
            showChangeViewType = true,
            onFolderClick = {}
        )
    }
}

@Composable
private fun FolderNode.folderInfo(): String {
    return if (childFolderCount == 0 && childFileCount == 0) {
        stringResource(R.string.file_browser_empty_folder)
    } else if (childFolderCount == 0 && childFileCount > 0) {
        pluralStringResource(R.plurals.num_files_with_parameter, childFileCount, childFileCount)
    } else if (childFileCount == 0 && childFolderCount > 0) {
        pluralStringResource(
            R.plurals.num_folders_with_parameter,
            childFolderCount,
            childFolderCount
        )
    } else {
        pluralStringResource(
            R.plurals.num_folders_num_files,
            childFolderCount,
            childFolderCount
        ) + pluralStringResource(
            R.plurals.num_folders_num_files_2,
            childFileCount,
            childFileCount
        )
    }
}

@Composable
private fun FolderNode.getIcon(): Int {
    return if (isIncomingShare) {
        CoreUIR.drawable.ic_folder_incoming
    } else if (isShared || isPendingShare) {
        CoreUIR.drawable.ic_folder_outgoing
    } else {
        CoreUIR.drawable.ic_folder_list
    }
}
