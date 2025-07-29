package mega.privacy.android.feature.clouddrive.presentation.clouddrive

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.LoadingView
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.privacy.android.core.nodecomponents.list.view.NodesView
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.core.nodecomponents.selectionmode.NodeSelectionModeAppBar
import mega.privacy.android.core.nodecomponents.selectionmode.NodeSelectionModeBottomBar
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveUiState

/**
 * Cloud Drive Screen, used to display contents of a folder
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudDriveScreen(
    onBack: () -> Unit,
    onNavigateToFolder: (NodeId) -> Unit,
    viewModel: CloudDriveViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = uiState.isInSelectionMode) {
        viewModel.deselectAllItems()
    }

    MegaScaffold(
        topBar = {
            if (uiState.isInSelectionMode) {
                NodeSelectionModeAppBar(
                    count = uiState.selectedNodeIds.size,
                    onSelectAllClicked = viewModel::selectAllItems,
                    onCancelSelectionClicked = viewModel::deselectAllItems
                )
            } else {
                MegaTopAppBar(
                    title = uiState.title.text,
                    navigationType = AppBarNavigationType.Back(onBack),
                )
            }
        },
        bottomBar = {
            NodeSelectionModeBottomBar(
                count = uiState.selectedNodeIds.size,
                visible = uiState.isInSelectionMode,
                onActionPressed = {
                    // TODO
                }
            )
        },
        content = { innerPadding ->
            CloudDriveContent(
                uiState = uiState,
                contentPadding = innerPadding,
                fileTypeIconMapper = viewModel.fileTypeIconMapper,
                onItemClicked = viewModel::onItemClicked,
                onItemLongClicked = viewModel::onItemLongClicked,
                onChangeViewTypeClicked = viewModel::onChangeViewTypeClicked,
                onNavigateToFolder = onNavigateToFolder,
                onNavigateToFolderEventConsumed = viewModel::onNavigateToFolderEventConsumed,
            )
        }
    )
}

@Composable
internal fun CloudDriveContent(
    uiState: CloudDriveUiState,
    fileTypeIconMapper: FileTypeIconMapper,
    onItemClicked: (NodeUiItem<TypedNode>) -> Unit,
    onItemLongClicked: (NodeUiItem<TypedNode>) -> Unit,
    onChangeViewTypeClicked: () -> Unit,
    onNavigateToFolder: (NodeId) -> Unit,
    onNavigateToFolderEventConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp, 0.dp),
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState(),
) {
    when {
        uiState.isLoading -> {
            if (uiState.currentFolderId.longValue == -1L) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()

                ) {
                    LoadingView(Modifier.align(alignment = Alignment.Center))
                }
            }
        }

        else -> NodesView(
            modifier = modifier,
            listContentPadding = contentPadding,
            listState = listState,
            gridState = gridState,
            items = uiState.items,
            onMenuClick = { },
            onItemClicked = onItemClicked,
            onLongClicked = onItemLongClicked,
            sortOrder = "Name",
            isListView = uiState.currentViewType == ViewType.LIST,
            onSortOrderClick = {},
            onChangeViewTypeClicked = onChangeViewTypeClicked,
            onLinkClicked = {},
            onDisputeTakeDownClicked = {},
            showMediaDiscoveryButton = false,
            onEnterMediaDiscoveryClick = {},
            fileTypeIconMapper = fileTypeIconMapper,
            inSelectionMode = uiState.isInSelectionMode,
            shouldApplySensitiveMode = false,
        )
    }

    EventEffect(
        event = uiState.navigateToFolderEvent,
        onConsumed = onNavigateToFolderEventConsumed
    ) { nodeId ->
        onNavigateToFolder(nodeId)
    }
}