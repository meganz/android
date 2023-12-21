package mega.privacy.android.feature.sync.ui.synclist

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType
import mega.privacy.android.feature.sync.ui.model.SyncOption
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.feature.sync.ui.views.SyncOptionsDialog
import mega.privacy.mobile.analytics.event.AndroidSyncChooseLatestModifiedTimeEvent
import mega.privacy.mobile.analytics.event.AndroidSyncChooseLocalFileEvent
import mega.privacy.mobile.analytics.event.AndroidSyncChooseRemoteFileEvent
import mega.privacy.mobile.analytics.event.AndroidSyncClearResolvedIssuesEvent
import mega.privacy.mobile.analytics.event.AndroidSyncMergeFoldersEvent
import mega.privacy.mobile.analytics.event.AndroidSyncRemoveDuplicatesAndRemoveRestEvent
import mega.privacy.mobile.analytics.event.AndroidSyncRemoveDuplicatesEvent
import mega.privacy.mobile.analytics.event.AndroidSyncRenameAllItemsEvent
import mega.privacy.mobile.analytics.event.SyncOptionSelected
import mega.privacy.mobile.analytics.event.SyncOptionSelectedEvent

@Composable
internal fun SyncListRoute(
    viewModel: SyncListViewModel,
    syncPermissionsManager: SyncPermissionsManager,
    addFolderClicked: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val snackBarHostState = remember { SnackbarHostState() }

    var showSyncOptionsDialog by rememberSaveable { mutableStateOf(false) }

    val message = state.snackbarMessage?.let {
        stringResource(id = it)
    }

    SyncListScreen(
        stalledIssuesCount = state.stalledIssuesCount,
        addFolderClicked = addFolderClicked,
        actionSelected = { item, selectedAction ->
            when (selectedAction.resolutionActionType) {
                StalledIssueResolutionActionType.RENAME_ALL_ITEMS -> {
                    Analytics.tracker.trackEvent(AndroidSyncRenameAllItemsEvent)
                }

                StalledIssueResolutionActionType.REMOVE_DUPLICATES -> {
                    Analytics.tracker.trackEvent(AndroidSyncRemoveDuplicatesEvent)
                }

                StalledIssueResolutionActionType.MERGE_FOLDERS -> {
                    Analytics.tracker.trackEvent(AndroidSyncMergeFoldersEvent)
                }

                StalledIssueResolutionActionType.REMOVE_DUPLICATES_AND_REMOVE_THE_REST -> {
                    Analytics.tracker.trackEvent(AndroidSyncRemoveDuplicatesAndRemoveRestEvent)
                }

                StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE -> {
                    Analytics.tracker.trackEvent(AndroidSyncChooseLocalFileEvent)
                }

                StalledIssueResolutionActionType.CHOOSE_REMOTE_FILE -> {
                    Analytics.tracker.trackEvent(AndroidSyncChooseRemoteFileEvent)
                }

                StalledIssueResolutionActionType.CHOOSE_LATEST_MODIFIED_TIME -> {
                    Analytics.tracker.trackEvent(AndroidSyncChooseLatestModifiedTimeEvent)
                }

                else -> {

                }
            }
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
                    Analytics.tracker.trackEvent(AndroidSyncClearResolvedIssuesEvent)
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
                when (it) {
                    SyncOption.WI_FI_OR_MOBILE_DATA -> {
                        Analytics.tracker.trackEvent(
                            SyncOptionSelectedEvent(SyncOptionSelected.SelectionType.SyncOptionWifiAndMobileSelected)
                        )
                    }

                    SyncOption.WI_FI_ONLY -> {
                        Analytics.tracker.trackEvent(
                            SyncOptionSelectedEvent(SyncOptionSelected.SelectionType.SyncOptionWifiOnlySelected)
                        )
                    }
                }

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
