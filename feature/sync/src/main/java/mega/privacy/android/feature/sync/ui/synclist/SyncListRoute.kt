package mega.privacy.android.feature.sync.ui.synclist

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager

@Composable
internal fun SyncListRoute(
    viewModel: SyncListViewModel,
    syncPermissionsManager: SyncPermissionsManager,
    addFolderClicked: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val snackBarHostState = remember { SnackbarHostState() }

    SyncListScreen(
        stalledIssuesCount = state.stalledIssuesCount,
        addFolderClicked = addFolderClicked,
        actionSelected = { item, selectedAction ->
            viewModel.handleAction(
                SyncListAction.ResolveStalledIssue(item, selectedAction)
            )
        },
        snackBarHostState = snackBarHostState,
        syncPermissionsManager = syncPermissionsManager
    )

    LaunchedEffect(key1 = state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackBarHostState.showSnackbar(it)
            viewModel.handleAction(SyncListAction.SnackBarShown)
        }
    }
}
