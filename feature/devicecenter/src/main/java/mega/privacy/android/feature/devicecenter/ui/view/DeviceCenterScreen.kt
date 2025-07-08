package mega.privacy.android.feature.devicecenter.ui.view

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.privacy.android.feature.devicecenter.R
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.DeviceBottomSheetBody
import mega.privacy.android.feature.devicecenter.ui.lists.loading.DeviceCenterLoadingScreen
import mega.privacy.android.feature.devicecenter.ui.model.BackupDeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterUINode
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterUiState
import mega.privacy.android.feature.devicecenter.ui.model.DeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.NonBackupDeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.renamedevice.RenameDeviceDialog
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar

/**
 * A [androidx.compose.runtime.Composable] that serves as the main View for the Device Center
 *
 * @param uiState The UI State
 * @param snackbarHostState The [androidx.compose.material.SnackbarHostState]
 * @param onDeviceClicked Lambda that performs a specific action when a Device is clicked
 * @param onDeviceMenuClicked Lambda that performs a specific action when a Device's Menu Icon is clicked
 * @param onBackupFolderClicked Lambda that performs a specific action when a Backup Folder is clicked
 * @param onNonBackupFolderClicked Lambda that performs a specific action when a Non Backup Folder is clicked
 * @param onInfoOptionClicked Lambda that performs a specific action when the User clicks the "Info" Bottom Dialog Option
 * @param onAddNewSyncOptionClicked Lambda that performs a specific action when the User clicks the "Add new sync" Bottom Dialog Option
 * @param onRenameDeviceOptionClicked Lambda that performs a specific action when the User clicks the "Rename" Bottom Dialog Option
 * @param onRenameDeviceCancelled Lambda that performs a specific action when cancelling the Rename Device action
 * @param onRenameDeviceSuccessful Lambda that performs a specific action when the Rename Device action is successful
 * @param onRenameDeviceSuccessfulSnackbarShown Lambda that performs a specific action when the Rename Device success Snackbar has been displayed
 * @param onBackPressHandled Lambda that performs a specific action when the Composable handles the Back Press
 * @param onFeatureExited Lambda that performs a specific action when the Device Center is exited
 * @param onActionPressed Action for each available option of the app bar menu
 */
@Composable
internal fun DeviceCenterScreen(
    uiState: DeviceCenterUiState,
    snackbarHostState: SnackbarHostState,
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
    onSearchQueryChanged: (query: String) -> Unit,
    onSearchCloseClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    onActionPressed: ((MenuAction) -> Unit)? = null,
) {
    val context = LocalContext.current
    val selectedDevice = uiState.selectedDevice
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = false,
    )

    LaunchedEffect(key1 = selectedDevice == null) {
        coroutineScope.launch { modalSheetState.hide() }
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
    // Handle the Back Press if the Bottom Dialog is visible and the User is in Folder View
    BackHandler(enabled = modalSheetState.isVisible || selectedDevice != null) {
        if (modalSheetState.isVisible) {
            coroutineScope.launch { modalSheetState.hide() }
        } else {
            onBackPressHandled()
        }
    }
    BottomSheet(
        modalSheetState = modalSheetState,
        bottomInsetPadding = false,
        expandedRoundedCorners = true,
        sheetBody = {
            DeviceBottomSheetBody(
                device = uiState.menuClickedDevice ?: return@BottomSheet,
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
        },
        content = {
            MegaScaffold(
                scaffoldState = rememberScaffoldState(snackbarHostState = snackbarHostState),
                shouldAddSnackBarPadding = false,
                contentWindowInsets = WindowInsets(0.dp),
                topBar = {
                    DeviceCenterAppBar(
                        uiState,
                        selectedDevice,
                        modalSheetState,
                        coroutineScope,
                        onBackPressHandled,
                        onActionPressed,
                        onSearchQueryChanged,
                        onSearchCloseClicked,
                        onSearchClicked
                    )
                },
                content = { paddingValues ->
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
                                onDeviceClicked = { deviceUiNode ->
                                    onDeviceClicked(deviceUiNode)
                                },
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
                                modifier = Modifier.Companion
                                    .background(MaterialTheme.colors.background)
                                    .consumeWindowInsets(paddingValues)
                            )
                        }
                    }

                    uiState.deviceToRename?.let { nonNullDevice ->
                        RenameDeviceDialog(
                            deviceId = nonNullDevice.id,
                            oldDeviceName = nonNullDevice.name,
                            existingDeviceNames = uiState.devices.map { it.name },
                            onRenameSuccessful = onRenameDeviceSuccessful,
                            onRenameCancelled = onRenameDeviceCancelled,
                        )
                    }
                },
            )
        }
    )
}


/**
 * A Preview Composable which shows the No network connectivity state
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterNoNetworkStatePreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterScreen(
            uiState = DeviceCenterUiState(isInitialLoadingFinished = true),
            snackbarHostState = SnackbarHostState(),
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
            onSearchQueryChanged = {},
            onSearchCloseClicked = {},
            onSearchClicked = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun DeviceCenterNoItemsFoundPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterScreen(
            uiState = DeviceCenterUiState(
                isInitialLoadingFinished = true,
                searchQuery = "testing",
                filteredUiItems = emptyList(),
                searchWidgetState = SearchWidgetState.EXPANDED,
                isNetworkConnected = true
            ),
            snackbarHostState = SnackbarHostState(),
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
            onSearchQueryChanged = {},
            onSearchCloseClicked = {},
            onSearchClicked = {},
        )
    }
}

/**
 * A Preview Composable that shows the Loading Screen
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterInInitialLoadingPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterScreen(
            uiState = DeviceCenterUiState(isNetworkConnected = true),
            snackbarHostState = SnackbarHostState(),
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
            onSearchQueryChanged = {},
            onSearchCloseClicked = {},
            onSearchClicked = {},
        )
    }
}

/**
 * A Preview Composable that shows the Device Center in Device View
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterInDeviceViewPreview() {
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
        DeviceCenterScreen(
            uiState = uiState,
            snackbarHostState = SnackbarHostState(),
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
            onSearchQueryChanged = {},
            onSearchCloseClicked = {},
            onSearchClicked = {},
        )
    }
}

/**
 * A Preview Composable that shows the Device Center in Folder View (when the user selects a Device)
 * and there i nothing setup yet (empty state)
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterInFolderViewEmptyStatePreview() {
    val uiState = DeviceCenterUiState(
        devices = listOf(ownDeviceUINode),
        isInitialLoadingFinished = true,
        selectedDevice = ownDeviceUINode,
        isNetworkConnected = true,
    )
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterScreen(
            uiState = uiState,
            snackbarHostState = SnackbarHostState(),
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
            onSearchQueryChanged = {},
            onSearchCloseClicked = {},
            onSearchClicked = {},
        )
    }
}

/**
 * A Preview Composable that shows the Device Center in Folder View (when the user selects a Device)
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterInFolderViewPreview() {
    val uiState = DeviceCenterUiState(
        devices = listOf(ownDeviceUINodeTwo),
        isInitialLoadingFinished = true,
        selectedDevice = ownDeviceUINodeTwo,
        isNetworkConnected = true,
    )
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterScreen(
            uiState = uiState,
            snackbarHostState = SnackbarHostState(),
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
            onSearchQueryChanged = {},
            onSearchCloseClicked = {},
            onSearchClicked = {},
        )
    }
}