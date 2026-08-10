package mega.privacy.android.feature.cloudexplorer.presentation.selectsyncfolder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.ExplorerPickerRestrictions
import mega.privacy.android.navigation.destination.ExplorerNavKey
import mega.privacy.android.navigation.destination.SelectStopBackupDestinationNavKey
import mega.privacy.android.shared.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.mobile.analytics.event.SyncMegaPickerFolderDisabledEvent

/**
 * Drives the sync MEGA folder picker on top of the shared explorer screens — mirroring how
 * `ShareToMegaUpload` layers the upload flow on top of the explorer. Returns `null` for any
 * non-sync [ExplorerMode] (no ViewModel is created), so the shared screens fall back to their
 * default behaviour.
 *
 * For the sync modes it creates the [SelectSyncFolderViewModel] and exposes the picker
 * restrictions to apply to the node list, the action to select the current folder, and the state
 * needed to render [SyncFolderPickerEffects].
 *
 * @param folderHandle Handle of the folder shown by the caller, or
 * [SelectSyncFolderViewModel.INVALID_FOLDER_HANDLE] for the entry screen (resolves the cloud drive
 * root).
 */
@Composable
internal fun rememberSyncFolderPicker(
    explorerMode: ExplorerMode,
    startNavKey: ExplorerNavKey,
    folderHandle: Long,
): SyncFolderPicker? {
    if (explorerMode != ExplorerMode.SelectSyncFolder &&
        explorerMode != ExplorerMode.SelectStopBackupDestination
    ) return null

    val viewModel =
        hiltViewModel<SelectSyncFolderViewModel, SelectSyncFolderViewModel.Factory> { factory ->
            factory.create(
                SelectSyncFolderViewModel.Args(
                    folderHandle = folderHandle,
                    isStopBackup = explorerMode == ExplorerMode.SelectStopBackupDestination,
                    stopBackupFolderName =
                        (startNavKey as? SelectStopBackupDestinationNavKey)?.folderName,
                )
            )
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val data = uiState as? SelectSyncFolderUiState.Data

    return SyncFolderPicker(
        uiState = uiState,
        syncPermissionsManager = viewModel.syncPermissionsManager,
        isProcessingAction = data?.isProcessing == true,
        restrictions = data?.let {
            ExplorerPickerRestrictions(
                restrictedNodeIds = it.restrictedNodes.keys,
                isPickEnabled = syncFolderPickEnabled(
                    explorerMode = explorerMode,
                    isSelectEnabled = it.isSelectEnabled,
                ),
                onRestrictedNodeClick = { nodeId ->
                    Analytics.tracker.trackEvent(SyncMegaPickerFolderDisabledEvent)
                    viewModel.handleAction(SelectSyncFolderAction.RestrictedFolderClicked(nodeId))
                },
            )
        },
        onCurrentFolderSelected = {
            viewModel.handleAction(SelectSyncFolderAction.CurrentFolderSelected)
        },
        onAction = viewModel::handleAction,
    )
}

/**
 * Holder returned by [rememberSyncFolderPicker] carrying everything the shared explorer screens
 * need to apply the sync picker behaviour.
 */
internal class SyncFolderPicker(
    val uiState: SelectSyncFolderUiState,
    val syncPermissionsManager: SyncPermissionsManager,
    val isProcessingAction: Boolean,
    val restrictions: ExplorerPickerRestrictions?,
    val onCurrentFolderSelected: () -> Unit,
    val onAction: (SelectSyncFolderAction) -> Unit,
)

/**
 * Whether the current folder can be selected as a sync target / stop-backup destination.
 *
 * - Stop backup can move into any folder; a destination name clash is validated on selection
 *   (and surfaced to the user) rather than disabling the action.
 * - For a new sync, [isSelectEnabled] comes from the sync domain logic and is already false at the
 *   root, when a child is used by a sync/backup, or when the folder is an ancestor of an existing
 *   sync/backup (selecting it would nest the existing one).
 */
internal fun syncFolderPickEnabled(
    explorerMode: ExplorerMode,
    isSelectEnabled: Boolean,
): Boolean = when (explorerMode) {
    ExplorerMode.SelectStopBackupDestination -> true
    else -> isSelectEnabled
}
