package mega.privacy.android.feature.cloudexplorer.presentation.selectsyncfolder

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.shared.sync.ui.permissions.SyncPermissionsManager

/**
 * Renders the side effects of the sync MEGA folder picker on top of the explorer: snackbar
 * messages, the permission dialog shown when the current folder is selected, the remove folder
 * connection dialog and the folder confirmed event.
 *
 * Dialog visibility is owned by the view; the [uiState] only emits the events that toggle it.
 */
@Composable
internal fun SyncFolderPickerEffects(
    uiState: SelectSyncFolderUiState,
    syncPermissionsManager: SyncPermissionsManager,
    onAction: (SelectSyncFolderAction) -> Unit,
    onFolderConfirmed: () -> Unit,
) {
    val data = uiState as? SelectSyncFolderUiState.Data ?: return
    val snackbarHostState = LocalSnackBarHostState.current
    val context = LocalContext.current

    var showDisableBatteryOptimizationsDialog by rememberSaveable { mutableStateOf(false) }
    // Only the device name is kept in the view (saveable, so it survives config changes); the node
    // used by the action lives in the state.
    var removeConnectionNodeName by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(data.removeConnectionNode) {
        removeConnectionNodeName = data.removeConnectionNode?.let { it.deviceName.orEmpty() }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        onAction(SelectSyncFolderAction.CurrentFolderSelected)
    }

    EventEffect(
        event = data.disableBatteryOptimizationsEvent,
        onConsumed = { onAction(SelectSyncFolderAction.DisableBatteryOptimizationsEventConsumed) },
    ) {
        showDisableBatteryOptimizationsDialog = true
    }
    EventEffect(
        event = data.warningEvent,
        onConsumed = { onAction(SelectSyncFolderAction.WarningEventConsumed) },
    ) { message ->
        snackbarHostState?.showAutoDurationSnackbar(message.get(context))
    }
    EventEffect(
        event = data.folderConfirmedEvent,
        onConsumed = { onAction(SelectSyncFolderAction.FolderConfirmedEventConsumed) },
    ) {
        onFolderConfirmed()
    }

    if (showDisableBatteryOptimizationsDialog) {
        DisableBatteryOptimizationDialog(
            onConfirm = {
                showDisableBatteryOptimizationsDialog = false
                permissionsLauncher.launch(
                    syncPermissionsManager.getDisableBatteryOptimizationsIntent()
                )
                onAction(SelectSyncFolderAction.DisableBatteryOptimizationsHandled)
            },
            onDismiss = {
                showDisableBatteryOptimizationsDialog = false
                onAction(SelectSyncFolderAction.DisableBatteryOptimizationsHandled)
                onAction(SelectSyncFolderAction.CurrentFolderSelected)
            },
        )
    }
    removeConnectionNodeName?.let { deviceName ->
        RemoveFolderConnectionDialog(
            deviceName = deviceName,
            onConfirm = {
                removeConnectionNodeName = null
                onAction(SelectSyncFolderAction.RemoveConnectionConfirmed)
            },
            onDismiss = {
                removeConnectionNodeName = null
                onAction(SelectSyncFolderAction.RemoveConnectionNodeConsumed)
            },
        )
    }
}
