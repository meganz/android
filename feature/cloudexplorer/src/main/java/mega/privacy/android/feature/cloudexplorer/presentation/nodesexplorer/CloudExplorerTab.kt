package mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.tabs.TabsScope
import mega.android.core.ui.model.LocalizedText
import mega.android.core.ui.model.TabItems
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.CLOUD_TAB_TAG
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.navigateToFolder
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.navigateToFolderPath
import mega.privacy.android.feature.cloudexplorer.presentation.search.NodesExplorerSearchContent
import mega.privacy.android.navigation.destination.ExplorerNavKey
import mega.privacy.android.shared.nodes.selection.NodeSelectionState
import mega.privacy.android.shared.resources.R as sharedR

/**
 * The cloud-drive explorer tab. Owns its [NodesExplorerViewModel] (like the other source tabs) and
 * lifts the data the host [ExplorerScreen] needs through callbacks instead of exposing the state.
 */
@Composable
internal fun TabsScope.CloudExplorerTab(
    explorerMode: ExplorerMode,
    startNavKey: ExplorerNavKey,
    nodeExplorerId: NodeId,
    nodeSourceType: NodeSourceType,
    shareUris: List<UriPath>?,
    showSearch: Boolean,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onCloseSearch: () -> Unit,
    protectedUserTap: (() -> Unit) -> Unit,
    onNavigate: (NavKey) -> Unit,
    onNavigateBack: () -> Unit,
    selectionState: NodeSelectionState,
    isFileSelectionEnabled: Boolean,
    disabledNodeIds: Set<NodeId>,
    videosOnly: Boolean,
    onHasContentChanged: (Boolean) -> Unit,
    onFolderNameChanged: (LocalizedText) -> Unit,
    onConnectivityChanged: (Boolean) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onNoConnection: () -> Unit,
) {
    val viewModel =
        hiltViewModel<NodesExplorerViewModel, NodesExplorerViewModel.Factory> { factory ->
            factory.create(NodeExplorerSharedViewModel.Args(nodeExplorerId, nodeSourceType))
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        NodeExplorerUiState.Loading -> LaunchedEffect(Unit) {
            onLoadingChanged(true)
            onHasContentChanged(false)
        }

        is NodeExplorerUiState.Data -> {
            LaunchedEffect(Unit) { onLoadingChanged(false) }
            LaunchedEffect(state.items.isEmpty()) { onHasContentChanged(state.items.isNotEmpty()) }
            LaunchedEffect(state.folderName) { onFolderNameChanged(state.folderName) }
            LaunchedEffect(state.isConnected) { onConnectivityChanged(state.isConnected) }
            EventEffect(
                event = state.noConnectionEvent,
                onConsumed = viewModel::onNoConnectionEventConsumed,
            ) { onNoConnection() }
        }
    }

    addTextTabWithScrollableContent(
        tabItem = TabItems(
            title = stringResource(sharedR.string.general_section_cloud_drive),
            testTag = CLOUD_TAB_TAG,
        ),
    ) { isActive, modifier ->
        if (showSearch) {
            NodesExplorerSearchContent(
                query = searchQuery,
                onQueryChanged = onSearchQueryChanged,
                nodeSelectionState = selectionState,
                isFileSelectionEnabled = isFileSelectionEnabled,
                videosOnly = videosOnly,
                disabledNodeIds = disabledNodeIds,
                onNavigateToFolderPath = navigateToFolderPath(
                    nodeSourceType = nodeSourceType,
                    explorerMode = explorerMode,
                    startNavKey = startNavKey,
                    shareUris = shareUris,
                    disabledNodeIds = disabledNodeIds.toList(),
                    protectedUserTap = protectedUserTap,
                    onNavigate = onNavigate,
                ),
                onCloseSearch = onCloseSearch,
                recentSearchesEnabled = isActive,
                modifier = modifier,
            )
        } else {
            NodesExplorerScreenContent(
                uiState = uiState,
                onNavigateBack = { protectedUserTap { onNavigateBack() } },
                consumeNavigateBack = viewModel::onNavigateBackEventConsumed,
                onFolderClick = navigateToFolder(
                    nodeSourceType = nodeSourceType,
                    explorerMode = explorerMode,
                    startNavKey = startNavKey,
                    shareUris = shareUris,
                    disabledNodeIds = disabledNodeIds.toList(),
                    protectedUserTap = protectedUserTap,
                    onNavigate = onNavigate,
                ),
                onRefreshNodes = viewModel::refreshNodes,
                selectionState = selectionState,
                isSelectionModeEnabled = isFileSelectionEnabled,
                disabledNodeIds = disabledNodeIds,
                videosOnly = videosOnly,
                modifier = modifier,
            )
        }
    }
}
