package mega.privacy.android.feature.clouddrive.presentation.clouddrive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.LoadingView
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.privacy.android.core.nodecomponents.list.view.NodesView
import mega.privacy.android.domain.entity.node.NodeId


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudDriveScreen(
    onBack: () -> Unit,
    onNavigateToFolder: (NodeId) -> Unit,
    viewModel: CloudDriveViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    MegaScaffold(
        topBar = {
            MegaTopAppBar(
                title = uiState.title.text,
                navigationType = AppBarNavigationType.Back(onBack),
            )
        },
        content = { innerPadding ->
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
                    listContentPadding = innerPadding,
                    listState = listState,
                    gridState = gridState,
                    items = uiState.items,
                    onMenuClick = { },
                    onItemClicked = viewModel::onItemClicked,
                    onLongClicked = { },
                    sortOrder = "Name",
                    isListView = true,
                    onSortOrderClick = {},
                    onChangeViewTypeClick = {},
                    onLinkClicked = {},
                    onDisputeTakeDownClicked = {},
                    showMediaDiscoveryButton = false,
                    onEnterMediaDiscoveryClick = {},
                    fileTypeIconMapper = viewModel.fileTypeIconMapper,
                    inSelectionMode = uiState.isInSelectionMode,
                    shouldApplySensitiveMode = false,
                )
            }
        }
    )

    EventEffect(
        event = uiState.navigateToFolderEvent,
        onConsumed = viewModel::onNavigateToFolderEventConsumed
    ) { nodeId ->
        onNavigateToFolder(nodeId)
    }
}