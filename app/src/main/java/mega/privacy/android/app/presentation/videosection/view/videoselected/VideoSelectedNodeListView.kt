package mega.privacy.android.app.presentation.videosection.view.videoselected

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.view.extension.folderInfo
import mega.privacy.android.app.presentation.view.extension.getIcon
import mega.privacy.android.app.presentation.view.previewdataprovider.SampleFolderNodeDataProvider
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.icon.pack.R
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem
import mega.privacy.android.legacy.core.ui.controls.lists.NodeListViewItem
import mega.privacy.android.shared.theme.MegaAppTheme

@Composable
internal fun <T : TypedNode> VideoSelectedNodeListView(
    nodeUIItemList: List<NodeUIItem<T>>,
    onItemClicked: (NodeUIItem<T>) -> Unit,
    sortOrder: String,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    showSortOrder: Boolean,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    showChangeViewType: Boolean = true,
    listContentPadding: PaddingValues = PaddingValues(0.dp),
    fileTypeIconMapper: FileTypeIconMapper
) {
    LazyColumn(state = listState, modifier = modifier, contentPadding = listContentPadding) {
        if (showSortOrder || showChangeViewType) {
            item(
                key = "header"
            ) {
                HeaderViewItem(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    onSortOrderClick = onSortOrderClick,
                    onChangeViewTypeClick = onChangeViewTypeClick,
                    onEnterMediaDiscoveryClick = {},
                    sortOrder = sortOrder,
                    isListView = true,
                    showSortOrder = showSortOrder,
                    showChangeViewType = showChangeViewType
                )
            }
        }

        items(count = nodeUIItemList.size, key = { nodeUIItemList[it].uniqueKey }) {
            val nodeEntity = nodeUIItemList[it].node

            NodeListViewItem(
                isSelected = nodeUIItemList[it].isSelected,
                folderInfo = nodeEntity
                    .let { node -> node as? FolderNode }
                    ?.folderInfo(),
                icon = nodeEntity
                    .let { node -> node as? TypedFolderNode }
                    ?.getIcon(fileTypeIconMapper)
                    ?: R.drawable.ic_video_medium_solid,
                fileSize = nodeEntity
                    .let { node -> node as? FileNode }
                    ?.let { file -> formatFileSize(file.size, LocalContext.current) },
                modifiedDate = nodeEntity.let { node -> node as? FileNode }?.let { fileNode ->
                    formatModifiedDate(
                        java.util.Locale(
                            Locale.current.language, Locale.current.region
                        ),
                        fileNode.modificationTime
                    )
                },
                name = nodeEntity.name,
                showMenuButton = false,
                isTakenDown = false,
                isFavourite = false,
                isSharedWithPublicLink = false,
                thumbnailData = ThumbnailRequest(nodeEntity.id),
                onClick = { onItemClicked(nodeUIItemList[it]) }
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
private fun VideoSelectedNodeListViewFoldersPreview(
    @PreviewParameter(SampleFolderNodeDataProvider::class) items: List<NodeUIItem<TypedFolderNode>>,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        VideoSelectedNodeListView(
            modifier = Modifier,
            nodeUIItemList = items,
            onItemClicked = {},
            sortOrder = "Name",
            onSortOrderClick = {},
            onChangeViewTypeClick = {},
            showSortOrder = true,
            listState = LazyListState(),
            showChangeViewType = true,
            listContentPadding = PaddingValues(0.dp),
            fileTypeIconMapper = FileTypeIconMapper()
        )
    }
}
