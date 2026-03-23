package mega.privacy.android.shared.nodes.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyColumn
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyVerticalGrid
import mega.android.core.ui.modifiers.conditional
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.shared.nodes.components.previewdata.LocalNodeHeaderPreviewData
import mega.privacy.android.shared.nodes.model.NodeHeaderItemUiState
import mega.privacy.android.shared.nodes.model.NodeHeaderItemViewModel
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
import mega.privacy.android.shared.nodes.model.NodeUiItem
import mega.privacy.android.shared.resources.R as sharedR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeViewWithHeader(
    items: List<NodeUiItem<TypedNode>>,
    nodeSourceType: NodeSourceType,
    nodesLoadingState: NodesLoadingState,
    emptyView: @Composable () -> Unit,
    itemListView: @Composable (NodeUiItem<TypedNode>) -> Unit,
    itemGridView: @Composable (NodeUiItem<TypedNode>) -> Unit,
    onRefreshNodes: () -> Unit,
    modifier: Modifier = Modifier,
    loadingListContent: @Composable () -> Unit = {
        NodesViewSkeleton(
            isListView = true,
            delay = NodeSkeletons.defaultDelay,
        )
    },
    loadingGridContent: @Composable (Int) -> Unit = { spanCount ->
        NodesViewSkeleton(
            isListView = false,
            spanCount = spanCount,
            delay = NodeSkeletons.defaultDelay,
        )
    },
) {
    val previewHeaderData = LocalNodeHeaderPreviewData.current
    if (previewHeaderData != null) {
        NodeViewWithHeaderLoaded(
            headerData = previewHeaderData,
            items = items,
            nodeSourceType = nodeSourceType,
            nodesLoadingState = nodesLoadingState,
            emptyView = emptyView,
            itemListView = itemListView,
            itemGridView = itemGridView,
            onRefreshNodes = onRefreshNodes,
            modifier = modifier,
            loadingListContent = loadingListContent,
            loadingGridContent = loadingGridContent,
            onUpdateViewType = {},
            onUpdateNodeSortConfiguration = { },
        )
    } else {
        val viewModel =
            hiltViewModel<NodeHeaderItemViewModel, NodeHeaderItemViewModel.Factory> { factory ->
                factory.create(nodeSourceType = nodeSourceType)
            }
        val headerUiState by viewModel.uiState.collectAsStateWithLifecycle()

        if (headerUiState.isLoading) {
            loadingListContent()
        } else {
            NodeViewWithHeaderLoaded(
                headerData = headerUiState as NodeHeaderItemUiState.Data,
                items = items,
                nodeSourceType = nodeSourceType,
                nodesLoadingState = nodesLoadingState,
                emptyView = emptyView,
                itemListView = itemListView,
                itemGridView = itemGridView,
                onRefreshNodes = onRefreshNodes,
                modifier = modifier,
                loadingListContent = loadingListContent,
                loadingGridContent = loadingGridContent,
                onUpdateViewType = viewModel::updateViewType,
                onUpdateNodeSortConfiguration = viewModel::updateNodeSortConfiguration,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodeViewWithHeaderLoaded(
    headerData: NodeHeaderItemUiState.Data,
    items: List<NodeUiItem<TypedNode>>,
    nodeSourceType: NodeSourceType,
    nodesLoadingState: NodesLoadingState,
    emptyView: @Composable () -> Unit,
    itemListView: @Composable (NodeUiItem<TypedNode>) -> Unit,
    itemGridView: @Composable (NodeUiItem<TypedNode>) -> Unit,
    onRefreshNodes: () -> Unit,
    modifier: Modifier,
    loadingListContent: @Composable () -> Unit,
    loadingGridContent: @Composable (Int) -> Unit,
    onUpdateViewType: () -> Unit,
    onUpdateNodeSortConfiguration: (NodeSortConfiguration) -> Unit,
) {
    val sortBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSortBottomSheet by rememberSaveable { mutableStateOf(false) }
    val isNextPageLoading = nodesLoadingState is NodesLoadingState.PartiallyLoaded
    val isList = headerData.viewType == ViewType.LIST
    val spanCount = rememberDynamicSpanCount()

    when {
        nodesLoadingState is NodesLoadingState.Loading -> if (isList) {
            loadingListContent()
        } else {
            loadingGridContent(spanCount)
        }

        items.isEmpty() -> emptyView()
        else -> {
            val nodeSortConfiguration = headerData.nodeSortConfiguration
            val header: @Composable () -> Unit = {
                NodeHeaderItem(
                    onSortOrderClick = { showSortBottomSheet = true },
                    onChangeViewTypeClick = onUpdateViewType,
                    sortConfiguration = nodeSortConfiguration,
                    isListView = isList,
                    showSortOrder = true,
                    showChangeViewType = true,
                    modifier = Modifier.conditional(isList) {
                        padding(horizontal = 8.dp)
                        padding(bottom = 8.dp)
                    },
                )
            }
            if (isList) {
                NodesListView(
                    items = items,
                    isNextPageLoading = isNextPageLoading,
                    itemView = itemListView,
                    nodeHeader = header,
                    modifier = modifier,
                )
            } else {
                NodesGridView(
                    items = items,
                    isNextPageLoading = isNextPageLoading,
                    spanCount = spanCount,
                    itemView = itemGridView,
                    nodeHeader = header,
                    modifier = modifier,
                )
            }

            if (showSortBottomSheet) {
                SortBottomSheet(
                    title = stringResource(sharedR.string.action_sort_by_header),
                    options = NodeSortOption.getOptionsForSourceType(nodeSourceType),
                    sheetState = sortBottomSheetState,
                    selectedSort = SortBottomSheetResult(
                        sortOptionItem = nodeSortConfiguration.sortOption,
                        sortDirection = nodeSortConfiguration.sortDirection
                    ),
                    onSortOptionSelected = { result ->
                        result?.let {
                            onUpdateNodeSortConfiguration(
                                NodeSortConfiguration(
                                    sortOption = it.sortOptionItem,
                                    sortDirection = it.sortDirection
                                )
                            )
                            onRefreshNodes()
                            showSortBottomSheet = false
                        }
                    },
                    onDismissRequest = {
                        showSortBottomSheet = false
                    }
                )
            }
        }
    }
}

@Composable
private fun NodesListView(
    items: List<NodeUiItem<TypedNode>>,
    isNextPageLoading: Boolean,
    itemView: @Composable (NodeUiItem<TypedNode>) -> Unit,
    nodeHeader: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    FastScrollLazyColumn(
        state = listState,
        totalItems = items.size,
        modifier = modifier.semantics { testTagsAsResourceId = true },
    ) {
        item(key = "header") {
            nodeHeader()
        }

        items(
            count = items.size,
            key = { items[it].id.longValue }
        ) {
            itemView(items[it])
        }

        if (isNextPageLoading) {
            items(count = 5, key = { "loading_$it" }) {
                NodeListViewItemSkeleton()
            }
        }
    }
}

@Composable
private fun NodesGridView(
    items: List<NodeUiItem<TypedNode>>,
    isNextPageLoading: Boolean,
    spanCount: Int,
    itemView: @Composable (NodeUiItem<TypedNode>) -> Unit,
    nodeHeader: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()

    FastScrollLazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(spanCount),
        totalItems = items.size,
        modifier = modifier
            .padding(horizontal = 8.dp)
            .semantics { testTagsAsResourceId = true },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        item(
            key = "header",
            span = { GridItemSpan(currentLineSpan = spanCount) },
        ) {
            nodeHeader()
        }
        items(
            count = items.size,
            key = {
                items[it].id.longValue
            },
        ) {
            itemView(items[it])
        }
        if (isNextPageLoading) {
            items(count = 4, key = { "loading_$it" }) {
                NodeGridViewItemSkeleton()
            }
        }
    }
}
