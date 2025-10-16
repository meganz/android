package mega.privacy.android.feature.devicecenter.ui.view

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.feature.devicecenter.R
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.DeviceBottomSheetBody
import mega.privacy.android.feature.devicecenter.ui.lists.loading.DeviceCenterLoadingScreen
import mega.privacy.android.feature.devicecenter.ui.model.BackupDeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterUINode
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterUiState
import mega.privacy.android.feature.devicecenter.ui.model.DeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.NonBackupDeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.renamedevice.RenameDeviceDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * A [androidx.compose.runtime.Composable] that serves as the main View for the Device Center (Material 3 version)
 *
 * This component does not include scaffold - it's provided by the parent (DeviceCenterScreenDestination)
 *
 * @param uiState The UI State
 * @param onDeviceClicked Lambda that performs a specific action when a Device is clicked
 * @param onDeviceMenuClicked Lambda that performs a specific action when a Device's Menu Icon is clicked
 * @param onBackupFolderClicked Lambda that performs a specific action when a Backup Folder is clicked
 * @param onNonBackupFolderClicked Lambda that performs a specific action when a Non Backup Folder is clicked
 * @param onCameraUploadsClicked Lambda that performs a specific action when Camera Uploads is clicked
 * @param onInfoOptionClicked Lambda that performs a specific action when the User clicks the "Info" Bottom Dialog Option
 * @param onAddNewSyncOptionClicked Lambda that performs a specific action when the User clicks the "Add new sync" Bottom Dialog Option
 * @param onAddBackupOptionClicked Lambda that performs a specific action when the User clicks the "Add backup" Bottom Dialog Option
 * @param onRenameDeviceOptionClicked Lambda that performs a specific action when the User clicks the "Rename" Bottom Dialog Option
 * @param onRenameDeviceCancelled Lambda that performs a specific action when cancelling the Rename Device action
 * @param onRenameDeviceSuccessful Lambda that performs a specific action when the Rename Device action is successful
 * @param onRenameDeviceSuccessfulSnackbarShown Lambda that performs a specific action when the Rename Device success Snackbar has been displayed
 * @param onBackPressHandled Lambda that performs a specific action when the Composable handles the Back Press
 * @param onFeatureExited Lambda that performs a specific action when the Device Center is exited
 * @param paddingValues Padding from parent scaffold
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeviceCenterScreenM3(
    uiState: DeviceCenterUiState,
    onDeviceClicked: (DeviceUINode) -> Unit,
    onDeviceMenuClicked: (DeviceUINode) -> Unit,
    onBackupFolderClicked: (BackupDeviceFolderUINode) -> Unit,
    onNonBackupFolderClicked: (NonBackupDeviceFolderUINode) -> Unit,
    onCameraUploadsClicked: () -> Unit,
    onInfoOptionClicked: (DeviceCenterUINode) -> Unit,
    onAddNewSyncOptionClicked: () -> Unit,
    onAddBackupOptionClicked: () -> Unit,
    onRenameDeviceOptionClicked: (DeviceUINode) -> Unit,
    onRenameDeviceCancelled: () -> Unit,
    onRenameDeviceSuccessful: () -> Unit,
    onRenameDeviceSuccessfulSnackbarShown: () -> Unit,
    onBackPressHandled: () -> Unit,
    onFeatureExited: () -> Unit,
    paddingValues: PaddingValues = PaddingValues(),
) {
    val context = LocalContext.current
    val selectedDevice = uiState.selectedDevice
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val snackbarHostState = LocalSnackBarHostState.current ?: SnackbarHostState()

    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
    )

    LaunchedEffect(key1 = selectedDevice == null) {
        if (modalSheetState.isVisible) {
            modalSheetState.hide()
        }
    }

    EventEffect(
        event = uiState.exitFeature,
        onConsumed = onFeatureExited,
        action = { onBackPressedDispatcher?.onBackPressed() },
    )

    EventEffect(
        event = uiState.renameDeviceSuccess,
        onConsumed = onRenameDeviceSuccessfulSnackbarShown,
        action = {
            snackbarHostState.showAutoDurationSnackbar(
                context.resources.getString(
                    R.string.device_center_snackbar_message_rename_device_successful
                )
            )
        },
    )

    val keyboardController = LocalSoftwareKeyboardController.current

    // Handle the Back Press if the Bottom Sheet is visible or User is in Folder View
    BackHandler(enabled = modalSheetState.isVisible || selectedDevice != null) {
        if (modalSheetState.isVisible) {
            coroutineScope.launch { modalSheetState.hide() }
        } else {
            onBackPressHandled()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .consumeWindowInsets(paddingValues)
    ) {
        when {
            !uiState.isNetworkConnected -> {
                DeviceCenterNoNetworkState()
            }

            !uiState.isInitialLoadingFinished -> {
                DeviceCenterLoadingScreen()
            }

            uiState.filteredUiItems?.isEmpty() == true -> {
                DeviceCenterNoItemsFound()
            }

            else -> {
                DeviceCenterContent(
                    itemsToDisplay = uiState.filteredUiItems ?: uiState.itemsToDisplay,
                    onDeviceClicked = onDeviceClicked,
                    onDeviceMenuClicked = { deviceNode ->
                        keyboardController?.hide()
                        onDeviceMenuClicked(deviceNode)
                        if (!modalSheetState.isVisible) {
                            coroutineScope.launch { modalSheetState.show() }
                        }
                    },
                    onBackupFolderClicked = onBackupFolderClicked,
                    onNonBackupFolderClicked = onNonBackupFolderClicked,
                    onInfoClicked = onInfoOptionClicked,
                )
            }
        }

        // Rename Device Dialog
        uiState.deviceToRename?.let { nonNullDevice ->
            RenameDeviceDialog(
                deviceId = nonNullDevice.id,
                oldDeviceName = nonNullDevice.name,
                existingDeviceNames = uiState.devices.map { it.name },
                onRenameSuccessful = onRenameDeviceSuccessful,
                onRenameCancelled = onRenameDeviceCancelled,
            )
        }
    }

    // Modal Bottom Sheet for Device Options
    if (modalSheetState.isVisible) {
        uiState.menuClickedDevice?.let { device ->
            MegaModalBottomSheet(
                onDismissRequest = {
                    coroutineScope.launch { modalSheetState.hide() }
                },
                sheetState = modalSheetState,
                bottomSheetBackground = MegaModalBottomSheetBackground.Surface1
            ) {
                DeviceBottomSheetBody(
                    device = device,
                    isCameraUploadsEnabled = uiState.isCameraUploadsEnabled,
                    onCameraUploadsClicked = onCameraUploadsClicked,
                    onRenameDeviceClicked = onRenameDeviceOptionClicked,
                    onInfoClicked = onInfoOptionClicked,
                    onAddNewSyncClicked = { onAddNewSyncOptionClicked() },
                    onAddBackupClicked = { onAddBackupOptionClicked() },
                    onBottomSheetDismissed = {
                        coroutineScope.launch { modalSheetState.hide() }
                    },
                )
            }
        }
    }
}


/**
 * A Preview Composable which shows the No network connectivity state
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterM3NoNetworkStatePreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterScreenM3(
            uiState = DeviceCenterUiState(isInitialLoadingFinished = true),
            onDeviceClicked = {},
            onDeviceMenuClicked = {},
            onBackupFolderClicked = {},
            onNonBackupFolderClicked = {},
            onCameraUploadsClicked = {},
            onInfoOptionClicked = {},
            onAddNewSyncOptionClicked = {},
            onAddBackupOptionClicked = {},
            onRenameDeviceOptionClicked = {},
            onRenameDeviceCancelled = {},
            onRenameDeviceSuccessful = {},
            onRenameDeviceSuccessfulSnackbarShown = {},
            onBackPressHandled = {},
            onFeatureExited = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun DeviceCenterM3NoItemsFoundPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterScreenM3(
            uiState = DeviceCenterUiState(
                isInitialLoadingFinished = true,
                searchQuery = "testing",
                filteredUiItems = emptyList(),
                isNetworkConnected = true
            ),
            onDeviceClicked = {},
            onDeviceMenuClicked = {},
            onBackupFolderClicked = {},
            onNonBackupFolderClicked = {},
            onCameraUploadsClicked = {},
            onInfoOptionClicked = {},
            onAddNewSyncOptionClicked = {},
            onAddBackupOptionClicked = {},
            onRenameDeviceOptionClicked = {},
            onRenameDeviceCancelled = {},
            onRenameDeviceSuccessful = {},
            onRenameDeviceSuccessfulSnackbarShown = {},
            onBackPressHandled = {},
            onFeatureExited = {},
        )
    }
}

/**
 * A Preview Composable that shows the Loading Screen
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterM3InInitialLoadingPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterScreenM3(
            uiState = DeviceCenterUiState(isNetworkConnected = true),
            onDeviceClicked = {},
            onDeviceMenuClicked = {},
            onBackupFolderClicked = {},
            onNonBackupFolderClicked = {},
            onCameraUploadsClicked = {},
            onInfoOptionClicked = {},
            onAddNewSyncOptionClicked = {},
            onAddBackupOptionClicked = {},
            onRenameDeviceOptionClicked = {},
            onRenameDeviceCancelled = {},
            onRenameDeviceSuccessful = {},
            onRenameDeviceSuccessfulSnackbarShown = {},
            onBackPressHandled = {},
            onFeatureExited = {},
        )
    }
}

/**
 * A Preview Composable that shows the Device Center in Device View
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterM3InDeviceViewPreview() {
    val uiState = DeviceCenterUiState(
        devices = listOf(
            ownDeviceUINode,
            otherDeviceUINodeOne,
            otherDeviceUINodeTwo,
            otherDeviceUINodeThree,
        ),
        isInitialLoadingFinished = true,
        isNetworkConnected = true,
    )
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterScreenM3(
            uiState = uiState,
            onDeviceClicked = {},
            onDeviceMenuClicked = {},
            onBackupFolderClicked = {},
            onNonBackupFolderClicked = {},
            onCameraUploadsClicked = {},
            onInfoOptionClicked = {},
            onAddNewSyncOptionClicked = {},
            onAddBackupOptionClicked = {},
            onRenameDeviceOptionClicked = {},
            onRenameDeviceCancelled = {},
            onRenameDeviceSuccessful = {},
            onRenameDeviceSuccessfulSnackbarShown = {},
            onBackPressHandled = {},
            onFeatureExited = {},
        )
    }
}

/**
 * A Preview Composable that shows the Device Center in Folder View (when the user selects a Device)
 * and there is nothing setup yet (empty state)
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterM3InFolderViewEmptyStatePreview() {
    val uiState = DeviceCenterUiState(
        devices = listOf(ownDeviceUINode),
        isInitialLoadingFinished = true,
        selectedDevice = ownDeviceUINode,
        isNetworkConnected = true,
    )
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterScreenM3(
            uiState = uiState,
            onDeviceClicked = {},
            onDeviceMenuClicked = {},
            onBackupFolderClicked = {},
            onNonBackupFolderClicked = {},
            onCameraUploadsClicked = {},
            onInfoOptionClicked = {},
            onAddNewSyncOptionClicked = {},
            onAddBackupOptionClicked = {},
            onRenameDeviceOptionClicked = {},
            onRenameDeviceCancelled = {},
            onRenameDeviceSuccessful = {},
            onRenameDeviceSuccessfulSnackbarShown = {},
            onBackPressHandled = {},
            onFeatureExited = {},
        )
    }
}

/**
 * A Preview Composable that shows the Device Center in Folder View (when the user selects a Device)
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterM3InFolderViewPreview() {
    val uiState = DeviceCenterUiState(
        devices = listOf(ownDeviceUINodeTwo),
        isInitialLoadingFinished = true,
        selectedDevice = ownDeviceUINodeTwo,
        isNetworkConnected = true,
    )
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterScreenM3(
            uiState = uiState,
            onDeviceClicked = {},
            onDeviceMenuClicked = {},
            onBackupFolderClicked = {},
            onNonBackupFolderClicked = {},
            onCameraUploadsClicked = {},
            onInfoOptionClicked = {},
            onAddNewSyncOptionClicked = {},
            onAddBackupOptionClicked = {},
            onRenameDeviceOptionClicked = {},
            onRenameDeviceCancelled = {},
            onRenameDeviceSuccessful = {},
            onRenameDeviceSuccessfulSnackbarShown = {},
            onBackPressHandled = {},
            onFeatureExited = {},
        )
    }
}
