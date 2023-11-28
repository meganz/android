package mega.privacy.android.feature.sync.ui.megapicker

import mega.privacy.android.core.R as CoreUIR
import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.ui.mapper.FileTypeIconMapper
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem
import mega.privacy.android.legacy.core.ui.controls.lists.NodeListViewItem

@Composable
internal fun MegaFolderPickerView(
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    nodesList: List<TypedNode>,
    sortOrder: String,
    showSortOrder: Boolean,
    showChangeViewType: Boolean,
    listState: LazyListState,
    onFolderClick: (TypedNode) -> Unit,
    fileTypeIconMapper: FileTypeIconMapper,
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

            val icon = when (nodeEntity) {
                is FolderNode -> {
                    nodeEntity.getIcon()
                }

                is FileNode -> {
                    fileTypeIconMapper(nodeEntity)
                }

                else -> {
                    iconPackR.drawable.ic_generic_list
                }
            }
            NodeListViewItem(
                isSelected = false,
                folderInfo = (nodeEntity as? FolderNode)
                    ?.folderInfo(),
                icon = icon,
                thumbnailData = ThumbnailRequest(nodeEntity.id),
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
            modifier = Modifier,
            showChangeViewType = true,
            onFolderClick = {},
            fileTypeIconMapper = FileTypeIconMapper()
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
