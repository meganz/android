package mega.privacy.android.feature.sync.ui.synclist

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.feature.sync.ui.views.SyncOptionsDialog

@Composable
internal fun SyncListRoute(
    viewModel: SyncListViewModel,
    syncPermissionsManager: SyncPermissionsManager,
    addFolderClicked: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val snackBarHostState = remember { SnackbarHostState() }

    var showSyncOptionsDialog by remember { mutableStateOf(false) }

    val message = state.snackbarMessage?.let {
        stringResource(id = it)
    }

    SyncListScreen(
        stalledIssuesCount = state.stalledIssuesCount,
        addFolderClicked = addFolderClicked,
        actionSelected = { item, selectedAction ->
            viewModel.handleAction(
                SyncListAction.ResolveStalledIssue(item, selectedAction)
            )
        },
        snackBarHostState = snackBarHostState,
        syncPermissionsManager = syncPermissionsManager,
        actions = prepareMenuActions(state),
        onActionPressed = {
            when (it) {
                is SyncListMenuAction.ClearSyncOptions -> {
                    viewModel.onClearSyncOptionsPressed()
                }

                is SyncListMenuAction.SyncOptions -> {
                    showSyncOptionsDialog = true
                }
            }
        }
    )

    if (showSyncOptionsDialog) {
        SyncOptionsDialog(
            onDismiss = {
                showSyncOptionsDialog = false
            },
            selectedOption = state.selectedSyncOption,
            onSyncOptionsClicked = {
                viewModel.handleAction(SyncListAction.SyncOptionsSelected(it))
                showSyncOptionsDialog = false
            },
        )
    }

    LaunchedEffect(key1 = state.snackbarMessage) {
        message?.let {
            snackBarHostState.showSnackbar(it)
            viewModel.handleAction(SyncListAction.SnackBarShown)
        }
    }

}

internal const val SYNC_OPTIONS_TEST_TAG =
    "sync_options_test_tag"
internal const val CLEAN_SOLVED_ISSUES_TEST_TAG =
    "clean_solved_issues_test_tag"

private fun prepareMenuActions(state: SyncListState): List<MenuAction> {
    val menuActionList = mutableListOf<MenuAction>()
    if (state.shouldShowSyncOptionsMenuItem) {
        menuActionList.add(SyncListMenuAction.SyncOptions)
    }
    if (state.shouldShowCleanSolvedIssueMenuItem) {
        menuActionList.add(SyncListMenuAction.ClearSyncOptions)
    }
    return menuActionList
}
