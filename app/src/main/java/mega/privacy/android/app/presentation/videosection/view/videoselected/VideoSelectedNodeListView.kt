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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.view.extension.getNodeItemDescription
import mega.privacy.android.app.presentation.view.extension.getNodeItemThumbnail
import mega.privacy.android.app.presentation.view.previewdataprovider.SampleFolderNodeDataProvider
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem
import mega.privacy.android.shared.original.core.ui.controls.lists.NodeListViewItem
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_alpha_012_white_alpha_012

@Composable
internal fun <T : TypedNode> VideoSelectedNodeListView(
    nodeUIItemList: List<NodeUIItem<T>>,
    onItemClicked: (NodeUIItem<T>) -> Unit,
    sortOrder: String,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    showSortOrder: Boolean,
    listState: LazyListState,
    fileTypeIconMapper: FileTypeIconMapper,
    modifier: Modifier = Modifier,
    showChangeViewType: Boolean = true,
    listContentPadding: PaddingValues = PaddingValues(0.dp),
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
            val item = nodeUIItemList[it]
            NodeListViewItem(
                title = item.name,
                subtitle = item.node.getNodeItemDescription(showPublicLinkCreationTime = false),
                isSelected = nodeUIItemList[it].isSelected,
                icon = item.node.getNodeItemThumbnail(fileTypeIconMapper = fileTypeIconMapper),
                thumbnailData = ThumbnailRequest(item.node.id),
                onItemClicked = { onItemClicked(nodeUIItemList[it]) }
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
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
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
