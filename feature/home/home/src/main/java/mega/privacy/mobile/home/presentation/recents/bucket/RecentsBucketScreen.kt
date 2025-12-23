package mega.privacy.mobile.home.presentation.recents.bucket

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.privacy.android.core.nodecomponents.R as NodeComponentsR
import mega.privacy.android.core.nodecomponents.action.HandleNodeAction3
import mega.privacy.android.core.nodecomponents.action.NodeActionHandler
import mega.privacy.android.core.nodecomponents.list.NodeListView
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.mobile.home.presentation.recents.bucket.model.RecentsBucketUiState
import mega.privacy.mobile.home.presentation.recents.bucket.view.RecentsMediaGridView
import mega.privacy.mobile.home.presentation.recents.view.RecentDateHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentsBucketScreen(
    viewModel: RecentsBucketViewModel,
    onNavigate: (NavKey) -> Unit,
    transferHandler: TransferHandler,
    nodeActionHandler: NodeActionHandler,
    onBack: () -> Unit,
    nodeSourceType: NodeSourceType,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var openedFileNode by remember { mutableStateOf<Pair<TypedFileNode, NodeSourceType>?>(null) }
    val listState = rememberLazyListState()

    MegaScaffoldWithTopAppBarScrollBehavior(
        topBar = {
            MegaTopAppBar(
                title = pluralStringResource(
                    NodeComponentsR.plurals.num_files_with_parameter,
                    uiState.fileCount,
                    uiState.fileCount
                ),
                subtitle = "Added to ${uiState.parentFolderName.text}", // TODO localize,
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
            onMenuClick = {
                onNavigate(
                    NodeOptionsBottomSheetNavKey(
                        nodeHandle = it.id.longValue,
                        nodeSourceType = nodeSourceType
                    )
                )
            },
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

    EventEffect(
        event = uiState.navigateBack,
        onConsumed = { viewModel.onNavigateBackEventConsumed() }
    ) {
        onBack()
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

        uiState.isEmpty -> {
            Box(modifier = modifier) {
                // TODO: Add empty view
            }
        }

        else -> {
            Column(
                modifier = modifier,
            ) {
                RecentDateHeader(
                    modifier = Modifier.padding(bottom = 1.dp),
                    timestamp = uiState.timestamp
                )

                if (uiState.isMediaBucket) {
                    RecentsMediaGridView(
                        nodeUiItems = uiState.items,
                        onItemClicked = { item ->
                            val node = item.node
                            if (node is TypedFileNode) {
                                onFileClicked(node)
                            }
                        },
                        onLongClick = onLongClick,
                    )
                } else {
                    NodeListView(
                        nodeUiItemList = uiState.items,
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
                        showChangeViewType = false,
                    )
                }
            }
        }
    }
}