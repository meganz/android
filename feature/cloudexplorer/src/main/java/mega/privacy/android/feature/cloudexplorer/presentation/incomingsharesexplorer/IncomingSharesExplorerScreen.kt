package mega.privacy.android.feature.cloudexplorer.presentation.incomingsharesexplorer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.tabs.TabsScope
import mega.android.core.ui.model.TabItems
import mega.android.core.ui.preview.BooleanProvider
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.shares.ShareFolderNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.shares.hasFullAccessPermission
import mega.privacy.android.domain.entity.shares.hasWritePermission
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.cloudexplorer.presentation.components.CloudExplorerGridViewItem
import mega.privacy.android.feature.cloudexplorer.presentation.components.CloudExplorerListViewItem
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.ExplorerViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.INCOMING_TAB_INDEX
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.INCOMING_TAB_TAG
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.navigateToFolder
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.navigateToFolderPath
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.rememberVisibleItems
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NODES_EXPLORER_EMPTY_VIEW_TAG
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodeExplorerUiState
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.previewNodeExplorerData
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.toTabSignal
import mega.privacy.android.feature.cloudexplorer.presentation.search.IncomingSharesExplorerSearchContent
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.destination.ExplorerNavKey
import mega.privacy.android.shared.nodes.components.NodeViewWithHeader
import mega.privacy.android.shared.nodes.components.NodesViewSkeleton
import mega.privacy.android.shared.nodes.components.previewdata.LocalNodeHeaderPreviewData
import mega.privacy.android.shared.nodes.components.previewdata.previewIncomingShareFolderNodeUiItem
import mega.privacy.android.shared.nodes.model.NodeHeaderItemUiState
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeViewItem
import mega.privacy.android.shared.nodes.model.text
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun IncomingSharesExplorerContent(
    uiState: NodeExplorerUiState,
    onNavigateBack: () -> Unit,
    consumeNavigateBack: () -> Unit,
    onFolderClick: (NodeId) -> Unit,
    onRefreshNodes: () -> Unit,
    modifier: Modifier = Modifier,
    requiresFullAccessShares: Boolean = false,
    emptyView: @Composable () -> Unit = { EmptyFolder() },
) {
    val snackbarHostState = LocalSnackBarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val resources = LocalResources.current

    when (uiState) {
        NodeExplorerUiState.Loading -> NodesViewSkeleton()
        is NodeExplorerUiState.Data -> {
            val isHiddenNodesEnabled = uiState.isHiddenNodesEnabled
            val visibleItems = rememberVisibleItems(
                items = uiState.items,
                showHiddenNodes = uiState.showHiddenNodes,
                isHiddenNodesEnabled = isHiddenNodesEnabled,
            )
            val onItemClicked: (NodeViewItem<TypedNode>) -> Unit = { item ->
                when {
                    item.isFolderNode -> {
                        if ((item.node as? ShareFolderNode)?.shareData?.access
                                ?.satisfies(requiresFullAccessShares) == false
                        ) {
                            coroutineScope.launch {
                                snackbarHostState?.showSnackbar(
                                    resources.getString(
                                        if (requiresFullAccessShares) {
                                            sharedR.string.general_sync_share_non_full_access
                                        } else {
                                            sharedR.string.general_read_only_folder_warning
                                        }
                                    )
                                )
                            }
                        } else {
                            onFolderClick(item.id)
                        }
                    }
                }
            }

            EventEffect(
                event = uiState.navigateBack,
                onConsumed = consumeNavigateBack,
            ) { onNavigateBack() }

            NodeViewWithHeader(
                items = visibleItems,
                nodeSourceType = uiState.nodeSourceType,
                nodesLoadingState = uiState.nodesLoadingState,
                emptyView = emptyView,
                itemListView = {
                    CloudExplorerListViewItem(
                        title = it.title.text,
                        subtitle = it.subtitle.text(),
                        icon = it.iconRes,
                        description = it.formattedDescription?.text,
                        tags = it.tags,
                        thumbnailData = it.thumbnailData,
                        isTakenDown = it.isTakenDown,
                        showIsVerified = it.showIsVerified,
                        label = it.nodeLabel,
                        isSensitive = it.isSensitive && isHiddenNodesEnabled,
                        showBlurEffect = it.showBlurEffect && isHiddenNodesEnabled,
                        isHighlighted = it.isHighlighted,
                        onItemClicked = { onItemClicked(it) },
                        enabled = (it.node as? ShareFolderNode)?.shareData?.access
                            ?.satisfies(requiresFullAccessShares) != false,
                        enableClick = true,
                    )
                },
                itemGridView = {
                    CloudExplorerGridViewItem(
                        name = it.title.text,
                        iconRes = it.iconRes,
                        thumbnailData = it.thumbnailData,
                        isTakenDown = it.isTakenDown,
                        duration = it.duration,
                        isFolderNode = it.isFolderNode,
                        isVideoNode = it.isVideoNode,
                        onClick = { onItemClicked(it) },
                        isSensitive = it.isSensitive && isHiddenNodesEnabled,
                        showBlurEffect = it.showBlurEffect && isHiddenNodesEnabled,
                        isHighlighted = it.isHighlighted,
                        label = it.nodeLabel,
                        enabled = (it.node as? ShareFolderNode)?.shareData?.access
                            ?.satisfies(requiresFullAccessShares) != false,
                    )
                },
                onRefreshNodes = onRefreshNodes,
                modifier = modifier,
            )
        }
    }
}

private fun AccessPermission.satisfies(requiresFullAccess: Boolean) =
    if (requiresFullAccess) hasFullAccessPermission() else hasWritePermission()

@Composable
private fun EmptyFolder() {
    EmptyStateView(
        title = stringResource(sharedR.string.shares_screen_incoming_empty),
        imagePainter = painterResource(iconPackR.drawable.ic_folder_arrow_up_glass),
        modifier = Modifier.testTag(NODES_EXPLORER_EMPTY_VIEW_TAG),
    )
}

@Composable
internal fun TabsScope.IncomingExplorerTab(
    explorerMode: ExplorerMode,
    startNavKey: ExplorerNavKey,
    shareUris: List<UriPath>?,
    showSearch: Boolean,
    searchQuery: String?,
    onSearchQueryChanged: (String) -> Unit,
    onCloseSearch: () -> Unit,
    protectedUserTap: (() -> Unit) -> Unit,
    onNavigate: (NavKey) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val viewModel = hiltViewModel<IncomingSharesExplorerViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val signal = remember(uiState) { uiState.toTabSignal() }
    val explorerViewModel = hiltViewModel<ExplorerViewModel>()
    val explorerUiState by explorerViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(signal) { explorerViewModel.onTabSignal(INCOMING_TAB_INDEX, signal) }

    addTextTabWithScrollableContent(
        tabItem = TabItems(
            title = stringResource(sharedR.string.general_title_incoming_shares),
            testTag = INCOMING_TAB_TAG,
        ),
    ) { isActive, modifier ->
        if (showSearch) {
            IncomingSharesExplorerSearchContent(
                query = searchQuery,
                isConnected = explorerUiState.isConnected,
                onQueryChanged = onSearchQueryChanged,
                onNavigateToFolderPath = navigateToFolderPath(
                    nodeSourceType = NodeSourceType.INCOMING_SHARES,
                    explorerMode = explorerMode,
                    startNavKey = startNavKey,
                    shareUris = shareUris,
                    protectedUserTap = protectedUserTap,
                    onNavigate = onNavigate,
                ),
                onCloseSearch = onCloseSearch,
                recentSearchesEnabled = isActive,
                modifier = modifier,
            )
        } else {
            IncomingSharesExplorerContent(
                uiState = uiState,
                requiresFullAccessShares = explorerMode.requiresFullAccessShares,
                onNavigateBack = { protectedUserTap { onNavigateBack() } },
                consumeNavigateBack = viewModel::onNavigateBackEventConsumed,
                onFolderClick = navigateToFolder(
                    nodeSourceType = NodeSourceType.INCOMING_SHARES,
                    explorerMode = explorerMode,
                    startNavKey = startNavKey,
                    shareUris = shareUris,
                    protectedUserTap = protectedUserTap,
                    onNavigate = onNavigate,
                ),
                onRefreshNodes = viewModel::refreshNodes,
                modifier = modifier,
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun EmptyFolderPreview() {
    AndroidThemeForPreviews {
        CompositionLocalProvider(
            LocalNodeHeaderPreviewData provides NodeHeaderItemUiState.Data(
                ViewType.LIST,
                nodeSortConfiguration = NodeSortConfiguration.default,
            ),
        ) {
            IncomingSharesExplorerContent(
                uiState = previewNodeExplorerData(nodeSourceType = NodeSourceType.INCOMING_SHARES),
                onNavigateBack = {},
                consumeNavigateBack = {},
                onFolderClick = {},
                onRefreshNodes = {},
            )
        }
    }
}

@Composable
@CombinedThemePreviews
private fun IncomingSharesExplorerFolderDestinationScreenPreview(
    @PreviewParameter(BooleanProvider::class) isList: Boolean,
) {
    AndroidThemeForPreviews {
        CompositionLocalProvider(
            LocalNodeHeaderPreviewData provides NodeHeaderItemUiState.Data(
                viewType = if (isList) ViewType.LIST else ViewType.GRID,
                nodeSortConfiguration = NodeSortConfiguration.default,
            ),
        ) {
            IncomingSharesExplorerContent(
                uiState = previewNodeExplorerData(
                    items = previewFolders(),
                    nodeSourceType = NodeSourceType.INCOMING_SHARES,
                ),
                onNavigateBack = {},
                consumeNavigateBack = {},
                onFolderClick = {},
                onRefreshNodes = {},
            )
        }
    }
}

private fun previewFolders() = (1..15L).map { id ->
    previewIncomingShareFolderNodeUiItem(
        id = id,
        access = if (id % 3 == 0L) AccessPermission.READ else AccessPermission.READWRITE,
        user = "User $id",
        userFullName = "Full name",
    )
}
