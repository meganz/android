package mega.privacy.android.feature.sync.ui.megapicker

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.triggered
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.feature.sync.ui.mapper.FileTypeIconMapper
import mega.privacy.android.feature.sync.ui.megapicker.MegaPickerAction.FolderClicked
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import nz.mega.sdk.MegaApiJava

@Composable
internal fun MegaPickerRoute(
    viewModel: MegaPickerViewModel,
    syncPermissionsManager: SyncPermissionsManager,
    folderSelected: () -> Unit,
    backClicked: () -> Unit,
    fileTypeIconMapper: FileTypeIconMapper
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        selectCurrentFolder(viewModel, syncPermissionsManager, context)
    }

    state.value.nodes?.let { nodes ->
        MegaPickerScreen(
            currentFolder = state.value.currentFolder,
            nodes = nodes,
            folderClicked = { viewModel.handleAction(FolderClicked(it)) },
            currentFolderSelected = {
                selectCurrentFolder(viewModel, syncPermissionsManager, context)
            },
            fileTypeIconMapper = fileTypeIconMapper
        )
    }

    val onBack = {
        if (state.value.currentFolder?.parentId?.longValue != MegaApiJava.INVALID_HANDLE) {
            viewModel.handleAction(MegaPickerAction.BackClicked)
        } else {
            backClicked()
        }
    }

    if (state.value.showAllFilesAccessDialog) {
        AllFilesAccessDialog(
            onConfirm = {
                permissionsLauncher.launch(
                    syncPermissionsManager.getManageExternalStoragePermissionIntent(
                        context
                    )
                )
                viewModel.handleAction(MegaPickerAction.AllFilesAccessPermissionDialogShown)
            },
            onDismiss = {
                viewModel.handleAction(MegaPickerAction.AllFilesAccessPermissionDialogShown)
            },
        )
    }
    if (state.value.showDisableBatteryOptimizationsDialog) {
        DisableBatteryOptimizationDialog(
            onConfirm = {
                permissionsLauncher.launch(
                    syncPermissionsManager.getDisableBatteryOptimizationsIntent(
                        context
                    )
                )
                viewModel.handleAction(MegaPickerAction.DisableBatteryOptimizationsDialogShown)
            },
            onDismiss = {
                viewModel.handleAction(MegaPickerAction.DisableBatteryOptimizationsDialogShown)
            },
        )
    }

    EventEffect(event = state.value.navigateNextEvent, onConsumed = {
        viewModel.handleAction(MegaPickerAction.NextScreenOpened)
    }) {
        // temporary navigation fix, state.value and viewmodel.state.value are out of sync
        // to avoid this workaround, we need to rearchitect the screen navigation logic
        if (viewModel.state.value.navigateNextEvent == triggered) {
            folderSelected()
        }
    }

    BackHandler(onBack = onBack)
}

private fun selectCurrentFolder(
    viewModel: MegaPickerViewModel,
    syncPermissionsManager: SyncPermissionsManager,
    context: Context,
) {
    viewModel.handleAction(
        MegaPickerAction.CurrentFolderSelected(
            allFilesAccessPermissionGranted = syncPermissionsManager.isManageExternalStoragePermissionGranted(),
            disableBatteryOptimizationPermissionGranted = syncPermissionsManager.isDisableBatteryOptimizationGranted(
                context
            )
        )
    )
}

@Composable
private fun AllFilesAccessDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        text = "Allow MEGA to read, modify or delete all files on this device.",
        confirmButtonText = "Allow",
        cancelButtonText = "Cancel",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        title = "Allow MEGA to access all files",
    )
}

@Composable
private fun DisableBatteryOptimizationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        text = "Allow MEGA to read, modify or delete all files on this device.",
        confirmButtonText = "Allow",
        cancelButtonText = "Cancel",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        title = "Battery optimization",
    )
}
