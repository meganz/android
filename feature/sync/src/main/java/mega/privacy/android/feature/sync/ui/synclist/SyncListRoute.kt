package mega.privacy.android.feature.sync.ui.synclist

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersViewModel
import mega.privacy.android.feature.sync.ui.synclist.solvedissues.SyncSolvedIssuesViewModel
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesViewModel
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.mobile.analytics.event.AndroidSyncChooseLatestModifiedTimeEvent
import mega.privacy.mobile.analytics.event.AndroidSyncChooseLocalFileEvent
import mega.privacy.mobile.analytics.event.AndroidSyncChooseRemoteFileEvent
import mega.privacy.mobile.analytics.event.AndroidSyncClearResolvedIssuesEvent
import mega.privacy.mobile.analytics.event.AndroidSyncMergeFoldersEvent
import mega.privacy.mobile.analytics.event.AndroidSyncRemoveDuplicatesAndRemoveRestEvent
import mega.privacy.mobile.analytics.event.AndroidSyncRemoveDuplicatesEvent
import mega.privacy.mobile.analytics.event.AndroidSyncRenameAllItemsEvent
import mega.privacy.mobile.analytics.event.SyncFeatureUpgradeDialogCancelButtonPressedEvent
import mega.privacy.mobile.analytics.event.SyncFeatureUpgradeDialogDisplayedEvent
import mega.privacy.mobile.analytics.event.SyncFeatureUpgradeDialogUpgradeButtonPressedEvent

@Composable
internal fun SyncListRoute(
    viewModel: SyncListViewModel,
    syncPermissionsManager: SyncPermissionsManager,
    addFolderClicked: () -> Unit,
    onOpenUpgradeAccountClicked: () -> Unit,
    syncFoldersViewModel: SyncFoldersViewModel,
    syncStalledIssuesViewModel: SyncStalledIssuesViewModel,
    syncSolvedIssuesViewModel: SyncSolvedIssuesViewModel,
    title: String? = null,
    selectedChip: SyncChip = SyncChip.SYNC_FOLDERS,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val snackBarHostState = remember { SnackbarHostState() }

    var showUpgradeDialog by rememberSaveable { mutableStateOf(false) }

    val message = state.snackbarMessage?.let {
        stringResource(id = it)
    }

    SyncListScreen(
        stalledIssuesCount = state.stalledIssuesCount,
        addFolderClicked = {
            if (state.isFreeAccount) {
                showUpgradeDialog = true
            } else {
                addFolderClicked()
            }
        },
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
                is SyncListMenuAction.AddNewSync -> {
                    if (state.isFreeAccount) {
                        showUpgradeDialog = true
                    } else {
                        addFolderClicked()
                    }
                }

                is SyncListMenuAction.ClearSyncOptions -> {
                    Analytics.tracker.trackEvent(AndroidSyncClearResolvedIssuesEvent)
                    viewModel.onClearSyncOptionsPressed()
                }
            }
        },
        onOpenUpgradeAccountClicked = onOpenUpgradeAccountClicked,
        title = title,
        syncFoldersViewModel = syncFoldersViewModel,
        syncStalledIssuesViewModel = syncStalledIssuesViewModel,
        syncSolvedIssuesViewModel = syncSolvedIssuesViewModel,
        selectedChip = selectedChip,
    )

    if (showUpgradeDialog) {
        Analytics.tracker.trackEvent(SyncFeatureUpgradeDialogDisplayedEvent)
        MegaAlertDialog(
            title = stringResource(id = sharedR.string.device_center_sync_upgrade_dialog_title),
            body = stringResource(id = sharedR.string.device_center_sync_upgrade_dialog_message),
            confirmButtonText = stringResource(id = sharedR.string.device_center_sync_upgrade_dialog_confirm_button),
            cancelButtonText = stringResource(id = sharedR.string.device_center_sync_upgrade_dialog_cancel_button),
            onConfirm = {
                Analytics.tracker.trackEvent(SyncFeatureUpgradeDialogUpgradeButtonPressedEvent)
                onOpenUpgradeAccountClicked()
                showUpgradeDialog = false
            },
            onDismiss = {
                Analytics.tracker.trackEvent(SyncFeatureUpgradeDialogCancelButtonPressedEvent)
                showUpgradeDialog = false
            },
            modifier = Modifier.testTag(TEST_TAG_SYNC_LIST_SCREEN_UPGRADE_DIALOG)
        )
    }

    LaunchedEffect(key1 = state.snackbarMessage) {
        message?.let {
            snackBarHostState.showAutoDurationSnackbar(it)
            viewModel.handleAction(SyncListAction.SnackBarShown)
        }
    }

}

private fun prepareMenuActions(state: SyncListState): List<MenuAction> {
    val menuActionList = mutableListOf<MenuAction>()
    menuActionList.add(SyncListMenuAction.AddNewSync)
    if (state.shouldShowCleanSolvedIssueMenuItem) {
        menuActionList.add(SyncListMenuAction.ClearSyncOptions)
    }
    return menuActionList
}

internal const val TEST_TAG_SYNC_LIST_SCREEN_UPGRADE_DIALOG =
    "sync_list_screen:upgrade_dialog"
