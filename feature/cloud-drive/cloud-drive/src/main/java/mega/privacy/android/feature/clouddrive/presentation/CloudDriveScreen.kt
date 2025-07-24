package mega.privacy.android.feature.clouddrive.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.LoadingView
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.privacy.android.core.nodecomponents.list.view.NodesView
import mega.privacy.android.shared.resources.R as sharedR


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudDriveScreen(
    viewModel: CloudDriveViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MegaScaffold(
        topBar = {
            MegaTopAppBar(
                title = stringResource(sharedR.string.general_section_cloud_drive),
                navigationType = AppBarNavigationType.Back(onBack),
            )
        },
        content = { innerPadding ->
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()

                    ) {
                        LoadingView(Modifier.align(alignment = Alignment.Center))
                    }
                }

                else -> NodesView(
                    listContentPadding = innerPadding,
                    items = uiState.items,
                    onMenuClick = { },
                    onItemClicked = { },
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
}