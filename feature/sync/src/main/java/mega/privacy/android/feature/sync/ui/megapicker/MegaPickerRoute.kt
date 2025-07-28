package mega.privacy.android.feature.sync.ui.megapicker

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.triggered
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.ui.megapicker.MegaPickerAction.FolderClicked
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedResR
import mega.privacy.mobile.analytics.event.AndroidSyncAllFilesAccessDialogConfirmButtonPressedEvent
import mega.privacy.mobile.analytics.event.AndroidSyncAllFilesAccessDialogDismissButtonPressedEvent
import mega.privacy.mobile.analytics.event.AndroidSyncAllFilesAccessDialogDisplayedEvent
import nz.mega.sdk.MegaApiJava

@Composable
internal fun MegaPickerRoute(
    viewModel: MegaPickerViewModel,
    syncPermissionsManager: SyncPermissionsManager,
    folderSelected: () -> Unit,
    backClicked: () -> Unit,
    fileTypeIconMapper: FileTypeIconMapper,
    isStopBackupMegaPicker: Boolean = false,
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        selectCurrentFolder(viewModel, syncPermissionsManager)
    }

    MegaPickerScreen(
        currentFolder = state.value.currentFolder,
        nodes = state.value.nodes,
        folderClicked = { viewModel.handleAction(FolderClicked(it)) },
        currentFolderSelected = {
            selectCurrentFolder(viewModel, syncPermissionsManager)
        },
        fileTypeIconMapper = fileTypeIconMapper,
        errorMessageId = state.value.errorMessageId,
        errorMessageShown = {
            viewModel.handleAction(MegaPickerAction.ErrorMessageShown)
        },
        onCreateNewFolderDialogSuccess = { newFolderName ->
            viewModel.createFolder(
                newFolderName = newFolderName, parentNode = state.value.currentFolder
            )
        },
        isLoading = state.value.isLoading,
        isSelectEnabled = state.value.isSelectEnabled,
        isStopBackupMegaPicker = isStopBackupMegaPicker,
    )

    val onBack = {
        if (state.value.currentFolder?.parentId?.longValue != MegaApiJava.INVALID_HANDLE) {
            viewModel.handleAction(MegaPickerAction.BackClicked)
        } else {
            backClicked()
        }
    }

    if (state.value.showAllFilesAccessDialog) {
        Analytics.tracker.trackEvent(AndroidSyncAllFilesAccessDialogDisplayedEvent)
        AllFilesAccessDialog(
            onConfirm = {
                viewModel.handleAction(MegaPickerAction.AllFilesAccessPermissionDialogShown)
                selectCurrentFolder(viewModel, syncPermissionsManager)
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
                    syncPermissionsManager.getDisableBatteryOptimizationsIntent()
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
) {
    viewModel.handleAction(
        MegaPickerAction.CurrentFolderSelected(
            allFilesAccessPermissionGranted = syncPermissionsManager.isManageExternalStoragePermissionGranted(),
            disableBatteryOptimizationPermissionGranted = syncPermissionsManager.isDisableBatteryOptimizationGranted(),
        )
    )
}

/**
 * Allow File access permission
 */
@Composable
fun AllFilesAccessDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        title = stringResource(id = sharedResR.string.sync_backup_access_storage_permission_dialog_title),
        text = stringResource(id = sharedResR.string.sync_backup_access_storage_permission_dialog_message),
        confirmButtonText = stringResource(id = R.string.sync_dialog_file_permission_positive_button),
        cancelButtonText = stringResource(id = R.string.sync_dialog_file_permission_negative_button),
        onConfirm = {
            Analytics.tracker.trackEvent(AndroidSyncAllFilesAccessDialogConfirmButtonPressedEvent)
            onConfirm()
        },
        onDismiss = {
            Analytics.tracker.trackEvent(AndroidSyncAllFilesAccessDialogDismissButtonPressedEvent)
            onDismiss()
        },
    )
}

@Composable
private fun DisableBatteryOptimizationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        text = stringResource(id = R.string.sync_battery_optimisation_banner),
        confirmButtonText = stringResource(id = R.string.sync_dialog_battery_optimization_positive_button),
        cancelButtonText = stringResource(id = R.string.sync_dialog_battery_optimization_negative_button),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        title = stringResource(id = R.string.sync_dialog_battery_optimization_title),
    )
}

@CombinedThemePreviews
@Composable
private fun AllFilesAccessDialogPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        AllFilesAccessDialog(
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun DisableBatteryOptimizationDialogPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        DisableBatteryOptimizationDialog(
            onConfirm = {},
            onDismiss = {},
        )
    }
}
