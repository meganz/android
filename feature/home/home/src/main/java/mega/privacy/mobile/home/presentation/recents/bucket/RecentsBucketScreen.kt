package mega.privacy.mobile.home.presentation.recents.bucket

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.privacy.android.core.nodecomponents.R as NodeComponentsR
import mega.privacy.android.core.nodecomponents.action.HandleNodeAction3
import mega.privacy.android.core.nodecomponents.list.NodeListView
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.mobile.home.presentation.recents.bucket.model.RecentsBucketUiState
import mega.privacy.mobile.home.presentation.recents.view.FormatRecentsDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentsBucketScreen(
    viewModel: RecentsBucketViewModel,
    onNavigate: (NavKey) -> Unit,
    transferHandler: TransferHandler,
    onBack: () -> Unit,
    nodeSourceType: NodeSourceType,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var openedFileNode by remember { mutableStateOf<Pair<TypedFileNode, NodeSourceType>?>(null) }
    val listState = rememberLazyListState()

    val title = pluralStringResource(
        NodeComponentsR.plurals.num_files_with_parameter,
        uiState.fileCount,
        uiState.fileCount
    )
    val subtitle = "${
        FormatRecentsDate(
            timestamp = uiState.timestamp,
            dateFormatPattern = "d MMM yyyy"
        )
    } · ${uiState.folderName}"

    MegaScaffoldWithTopAppBarScrollBehavior(
        topBar = {
            MegaTopAppBar(
                title = title,
                subtitle = subtitle,
                navigationType = AppBarNavigationType.Back(onBack),
                actions = emptyList(),
            )
        },
    ) { paddingValues ->
        RecentsBucketScreenContent(
            uiState = uiState,
            modifier = Modifier.padding(paddingValues),
            listState = listState,
            onFileClicked = { node ->
                openedFileNode = node to nodeSourceType
            },
            onMenuClick = { }, // TODO handle node option modal
            onLongClick = { }, // TODO handle selection mode
        )

        // TODO handle for recents, with list of node ids
        openedFileNode?.let { (node, source) ->
            HandleNodeAction3(
                typedFileNode = node,
                snackBarHostState = LocalSnackBarHostState.current,
                coroutineScope = coroutineScope,
                onActionHandled = { openedFileNode = null },
                nodeSourceType = source,
                onDownloadEvent = transferHandler::setTransferEvent,
                onNavigate = onNavigate,
            )
        }
    }
}

@Composable
internal fun RecentsBucketScreenContent(
    uiState: RecentsBucketUiState,
    listState: LazyListState,
    onFileClicked: (TypedFileNode) -> Unit,
    onMenuClick: (NodeUiItem<TypedNode>) -> Unit,
    onLongClick: (NodeUiItem<TypedNode>) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading -> {
            Box(modifier = modifier) {
                // TODO: Add loading view
            }
        }

        uiState.nodeUiItems.isEmpty() -> {
            Box(modifier = modifier) {
                // TODO: Add empty view
            }
        }

        else -> {
            NodeListView(
                nodeUiItemList = uiState.nodeUiItems,
                onMenuClick = onMenuClick,
                onItemClicked = { item ->
                    val node = item.node
                    if (node is TypedFileNode) {
                        onFileClicked(node)
                    }
                },
                onLongClick = onLongClick,
                onEnterMediaDiscoveryClick = { /** No-op */ },
                sortConfiguration = NodeSortConfiguration.default,
                onSortOrderClick = { /** No-op */ },
                onChangeViewTypeClick = { /** No-op */ },
                showSortOrder = false,
                listState = listState,
                showMediaDiscoveryButton = false,
                modifier = modifier,
                showChangeViewType = false,
            )
        }
    }
}