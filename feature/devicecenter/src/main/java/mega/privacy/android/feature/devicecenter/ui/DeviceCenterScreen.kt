package mega.privacy.android.feature.devicecenter.ui

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.shared.sync.R as sharedSyncR
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.feature.devicecenter.R
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.DeviceBottomSheetBody
import mega.privacy.android.feature.devicecenter.ui.lists.DeviceCenterListViewItem
import mega.privacy.android.feature.devicecenter.ui.lists.loading.DeviceCenterLoadingScreen
import mega.privacy.android.feature.devicecenter.ui.model.BackupDeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterUINode
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterUiState
import mega.privacy.android.feature.devicecenter.ui.model.DeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.DeviceMenuAction
import mega.privacy.android.feature.devicecenter.ui.model.DeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.NonBackupDeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.OtherDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.OwnDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceIconType
import mega.privacy.android.feature.devicecenter.ui.model.icon.FolderIconType
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import mega.privacy.android.feature.devicecenter.ui.renamedevice.RenameDeviceDialog
import mega.privacy.android.legacy.core.ui.controls.appbar.LegacySearchAppBar
import mega.privacy.android.legacy.core.ui.controls.lists.MenuActionHeader
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.shared.original.core.ui.controls.snackbars.MegaSnackbar
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.sync.ui.SyncEmptyState
import mega.privacy.mobile.analytics.event.SyncFeatureUpgradeDialogCancelButtonPressedEvent
import mega.privacy.mobile.analytics.event.SyncFeatureUpgradeDialogDisplayedEvent
import mega.privacy.mobile.analytics.event.SyncFeatureUpgradeDialogUpgradeButtonPressedEvent

/**
 * Test tags for the Device Center Screen
 */
internal const val DEVICE_CENTER_TOOLBAR = "device_center_screen:mega_app_bar"
internal const val DEVICE_CENTER_THIS_DEVICE_HEADER =
    "device_center_content:menu_action_header_this_device"
internal const val DEVICE_CENTER_OTHER_DEVICES_HEADER =
    "device_center_content:menu_action_header_other_devices"
internal const val DEVICE_CENTER_NO_NETWORK_STATE = "device_center_content:no_network_state"
internal const val DEVICE_CENTER_NOTHING_SETUP_STATE = "device_center_content:nothing_setup_state"
internal const val DEVICE_CENTER_NO_ITEMS_FOUND_STATE = "device_center_content:no_items_found_state"
internal const val TEST_TAG_DEVICE_CENTER_SCREEN_UPGRADE_DIALOG =
    "device_center_screen:upgrade_dialog"

/**
 * A [Composable] that serves as the main View for the Device Center
 *
 * @param uiState The UI State
 * @param snackbarHostState The [SnackbarHostState]
 * @param onDeviceClicked Lambda that performs a specific action when a Device is clicked
 * @param onDeviceMenuClicked Lambda that performs a specific action when a Device's Menu Icon is clicked
 * @param onBackupFolderClicked Lambda that performs a specific action when a Backup Folder is clicked
 * @param onNonBackupFolderClicked Lambda that performs a specific action when a Non Backup Folder is clicked
 * @param onCameraUploadsClicked Lambda that performs a specific action when the User clicks the "Camera uploads" Bottom Dialog Option
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
@OptIn(ExperimentalMaterialApi::class)
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
    onAddNewSyncOptionClicked: (DeviceUINode) -> Unit,
    onRenameDeviceOptionClicked: (DeviceUINode) -> Unit,
    onRenameDeviceCancelled: () -> Unit,
    onRenameDeviceSuccessful: () -> Unit,
    onRenameDeviceSuccessfulSnackbarShown: () -> Unit,
    onBackPressHandled: () -> Unit,
    onFeatureExited: () -> Unit,
    onSearchQueryChanged: (query: String) -> Unit,
    onSearchCloseClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    onOpenUpgradeAccountClicked: () -> Unit,
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

    var showUpgradeDialog by rememberSaveable { mutableStateOf(false) }

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
            snackbarHostState.showSnackbar(
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
        scrimColor = Color.Black.copy(alpha = 0.32f),
        sheetBody = {
            DeviceBottomSheetBody(
                device = uiState.menuClickedDevice ?: return@BottomSheet,
                isCameraUploadsEnabled = uiState.isCameraUploadsEnabled,
                onCameraUploadsClicked = onCameraUploadsClicked,
                onRenameDeviceClicked = onRenameDeviceOptionClicked,
                onInfoClicked = onInfoOptionClicked,
                onAddNewSyncClicked = {
                    if (uiState.isFreeAccount) {
                        showUpgradeDialog = true
                    } else {
                        onAddNewSyncOptionClicked(uiState.menuClickedDevice)
                    }
                },
                onBottomSheetDismissed = {
                    coroutineScope.launch { modalSheetState.hide() }
                },
                isFreeAccount = uiState.isFreeAccount,
                isSyncFeatureFlagEnabled = uiState.isSyncFeatureFlagEnabled,
            )
        },
        content = {
            Scaffold(
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
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                        MegaSnackbar(snackbarData = snackbarData)
                    }
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
                                modifier = Modifier.padding(paddingValues),
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
            modifier = Modifier.testTag(TEST_TAG_DEVICE_CENTER_SCREEN_UPGRADE_DIALOG)
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun DeviceCenterAppBar(
    uiState: DeviceCenterUiState,
    selectedDevice: DeviceUINode?,
    modalSheetState: ModalBottomSheetState,
    coroutineScope: CoroutineScope,
    onBackPressHandled: () -> Unit,
    onActionPressed: ((MenuAction) -> Unit)?,
    onSearchQueryChanged: (query: String) -> Unit,
    onSearchCloseClicked: () -> Unit,
    onSearchClicked: () -> Unit,
) {
    if (!uiState.isInitialLoadingFinished || !uiState.isNetworkConnected) {
        MegaAppBar(
            modifier = Modifier.testTag(DEVICE_CENTER_TOOLBAR),
            appBarType = AppBarType.BACK_NAVIGATION,
            title = selectedDevice?.name
                ?: stringResource(R.string.device_center_top_app_bar_title),
            elevation = 0.dp,
            onNavigationPressed = {
                if (modalSheetState.isVisible) {
                    coroutineScope.launch { modalSheetState.hide() }
                } else {
                    onBackPressHandled()
                }
            },
            onActionPressed = onActionPressed,
        )
    } else {
        LegacySearchAppBar(
            searchWidgetState = uiState.searchWidgetState,
            typedSearch = uiState.searchQuery,
            onSearchTextChange = { onSearchQueryChanged(it) },
            onCloseClicked = {
                onSearchCloseClicked()
            },
            onBackPressed = {
                if (modalSheetState.isVisible) {
                    coroutineScope.launch { modalSheetState.hide() }
                } else {
                    onBackPressHandled()
                }
            },
            onSearchClicked = { onSearchClicked() },
            elevation = false,
            title = selectedDevice?.name
                ?: stringResource(R.string.device_center_top_app_bar_title),
            hintId = if (uiState.itemsToDisplay.any { it is DeviceUINode }) {
                R.string.device_center_top_app_bar_search_devices_hint
            } else {
                R.string.device_center_top_app_bar_search_syncs_hint
            },
            onActionPressed = onActionPressed,
            actions = selectedDevice?.let {
                val list = mutableListOf<MenuAction>(DeviceMenuAction.Rename)

                when (uiState.selectedDevice) {
                    is OwnDeviceUINode -> {
                        if (uiState.isCameraUploadsEnabled) {
                            list.add(DeviceMenuAction.Info)
                        }
                        if (!uiState.isSyncFeatureFlagEnabled) {
                            list.add(DeviceMenuAction.CameraUploads)
                        }
                    }

                    else -> list.add(DeviceMenuAction.Info)
                }

                return@let list
            },
            modifier = Modifier.testTag(DEVICE_CENTER_TOOLBAR),
        )
    }
}

/**
 * A [Composable] which displays a No network connectivity state
 */
@Composable
private fun DeviceCenterNoNetworkState() {
    SyncEmptyState(
        iconId = sharedSyncR.drawable.ic_no_cloud,
        iconSize = 144.dp,
        iconDescription = "No network connectivity state",
        textId = R.string.device_center_no_network_state,
        testTag = DEVICE_CENTER_NO_NETWORK_STATE
    )
}

/**
 * A [Composable] which displays a Nothing setup state
 */
@Composable
private fun DeviceCenterNothingSetupState() {
    SyncEmptyState(
        iconId = R.drawable.ic_folder_sync_empty,
        iconSize = 128.dp,
        iconDescription = "No setup state",
        textId = R.string.device_center_nothing_setup_state,
        testTag = DEVICE_CENTER_NOTHING_SETUP_STATE
    )
}

/**
 * A [Composable] which displays an Items not found state
 */
@Composable
private fun DeviceCenterNoItemsFound() {
    SyncEmptyState(
        iconId = iconPackR.drawable.ic_search_02,
        iconSize = 128.dp,
        iconDescription = "No results found for search",
        textId = R.string.device_center_empty_screen_no_results,
        testTag = DEVICE_CENTER_NO_ITEMS_FOUND_STATE
    )
}

/**
 * A [Composable] that displays the User's Backup information
 *
 * @param itemsToDisplay The list of Backup Devices / Device Folders to be displayed
 * @param onDeviceClicked Lambda that performs a specific action when a Device is clicked
 * @param onDeviceMenuClicked Lambda that performs a specific action when a Device's Menu Icon is clicked
 * @param onBackupFolderClicked Lambda that performs a specific action when a Backup Folder is clicked
 * @param onInfoClicked Lambda that performs a specific action when the Info option is clicked
 * @param modifier The Modifier object
 */
@Composable
private fun DeviceCenterContent(
    itemsToDisplay: List<DeviceCenterUINode>,
    onDeviceClicked: (DeviceUINode) -> Unit,
    onDeviceMenuClicked: (DeviceUINode) -> Unit,
    onBackupFolderClicked: (BackupDeviceFolderUINode) -> Unit,
    onNonBackupFolderClicked: (NonBackupDeviceFolderUINode) -> Unit,
    onInfoClicked: (DeviceCenterUINode) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (itemsToDisplay.isNotEmpty()) {
        val currentlyUsedDevices = itemsToDisplay.filterIsInstance<OwnDeviceUINode>()
        val otherDevices = itemsToDisplay.filterIsInstance<OtherDeviceUINode>()
        val deviceFolders = itemsToDisplay.filterIsInstance<DeviceFolderUINode>()

        LazyColumn(
            modifier = modifier,
            state = LazyListState(),
        ) {
            // The User's Device Folders are shown
            if (deviceFolders.isNotEmpty()) {
                items(count = deviceFolders.size, key = {
                    deviceFolders[it].id
                }) { itemIndex ->
                    DeviceCenterListViewItem(
                        uiNode = deviceFolders[itemIndex],
                        onBackupFolderClicked = onBackupFolderClicked,
                        onNonBackupFolderClicked = onNonBackupFolderClicked,
                        onInfoClicked = onInfoClicked,
                    )
                }
                // The User's Devices are shown
            } else {
                if (currentlyUsedDevices.isNotEmpty()) {
                    item {
                        MenuActionHeader(
                            modifier = Modifier.testTag(DEVICE_CENTER_THIS_DEVICE_HEADER),
                            text = stringResource(R.string.device_center_list_view_item_header_this_device),
                        )
                    }
                    items(count = currentlyUsedDevices.size, key = {
                        currentlyUsedDevices[it].id
                    }) { itemIndex ->
                        DeviceCenterListViewItem(
                            uiNode = currentlyUsedDevices[itemIndex],
                            onDeviceClicked = onDeviceClicked,
                            onDeviceMenuClicked = onDeviceMenuClicked,
                        )
                    }
                }
                if (otherDevices.isNotEmpty()) {
                    item {
                        MenuActionHeader(
                            modifier = Modifier.testTag(DEVICE_CENTER_OTHER_DEVICES_HEADER),
                            text = stringResource(R.string.device_center_list_view_item_header_other_devices),
                        )
                    }
                    items(count = otherDevices.size, key = {
                        otherDevices[it].id
                    }) { itemIndex ->
                        DeviceCenterListViewItem(
                            uiNode = otherDevices[itemIndex],
                            onDeviceClicked = onDeviceClicked,
                            onDeviceMenuClicked = onDeviceMenuClicked,
                        )
                    }
                }
            }
        }
    } else {
        DeviceCenterNothingSetupState()
    }
}

/**
 * A Preview Composable which shows the No network connectivity state
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterNoNetworkStatePreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterScreen(
            uiState = DeviceCenterUiState(isInitialLoadingFinished = true),
            snackbarHostState = SnackbarHostState(),
            onDeviceClicked = {},
            onDeviceMenuClicked = {},
            onBackupFolderClicked = {},
            onNonBackupFolderClicked = {},
            onInfoOptionClicked = {},
            onAddNewSyncOptionClicked = {},
            onCameraUploadsClicked = {},
            onRenameDeviceOptionClicked = {},
            onRenameDeviceCancelled = {},
            onRenameDeviceSuccessful = {},
            onRenameDeviceSuccessfulSnackbarShown = {},
            onBackPressHandled = {},
            onFeatureExited = {},
            onSearchQueryChanged = {},
            onSearchCloseClicked = {},
            onSearchClicked = {},
            onOpenUpgradeAccountClicked = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun DeviceCenterNoItemsFoundPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
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
            onInfoOptionClicked = {},
            onAddNewSyncOptionClicked = {},
            onCameraUploadsClicked = {},
            onRenameDeviceOptionClicked = {},
            onRenameDeviceCancelled = {},
            onRenameDeviceSuccessful = {},
            onRenameDeviceSuccessfulSnackbarShown = {},
            onBackPressHandled = {},
            onFeatureExited = {},
            onSearchQueryChanged = {},
            onSearchCloseClicked = {},
            onSearchClicked = {},
            onOpenUpgradeAccountClicked = {},
        )
    }
}

/**
 * A Preview Composable that shows the Loading Screen
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterInInitialLoadingPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterScreen(
            uiState = DeviceCenterUiState(isNetworkConnected = true),
            snackbarHostState = SnackbarHostState(),
            onDeviceClicked = {},
            onDeviceMenuClicked = {},
            onBackupFolderClicked = {},
            onNonBackupFolderClicked = {},
            onInfoOptionClicked = {},
            onAddNewSyncOptionClicked = {},
            onCameraUploadsClicked = {},
            onRenameDeviceOptionClicked = {},
            onRenameDeviceCancelled = {},
            onRenameDeviceSuccessful = {},
            onRenameDeviceSuccessfulSnackbarShown = {},
            onBackPressHandled = {},
            onFeatureExited = {},
            onSearchQueryChanged = {},
            onSearchCloseClicked = {},
            onSearchClicked = {},
            onOpenUpgradeAccountClicked = {},
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
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterScreen(
            uiState = uiState,
            snackbarHostState = SnackbarHostState(),
            onDeviceClicked = {},
            onDeviceMenuClicked = {},
            onBackupFolderClicked = {},
            onNonBackupFolderClicked = {},
            onInfoOptionClicked = {},
            onAddNewSyncOptionClicked = {},
            onCameraUploadsClicked = {},
            onRenameDeviceOptionClicked = {},
            onRenameDeviceCancelled = {},
            onRenameDeviceSuccessful = {},
            onRenameDeviceSuccessfulSnackbarShown = {},
            onBackPressHandled = {},
            onFeatureExited = {},
            onSearchQueryChanged = {},
            onSearchCloseClicked = {},
            onSearchClicked = {},
            onOpenUpgradeAccountClicked = {},
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
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterScreen(
            uiState = uiState,
            snackbarHostState = SnackbarHostState(),
            onDeviceClicked = {},
            onDeviceMenuClicked = {},
            onBackupFolderClicked = {},
            onNonBackupFolderClicked = {},
            onInfoOptionClicked = {},
            onAddNewSyncOptionClicked = {},
            onCameraUploadsClicked = {},
            onRenameDeviceOptionClicked = {},
            onRenameDeviceCancelled = {},
            onRenameDeviceSuccessful = {},
            onRenameDeviceSuccessfulSnackbarShown = {},
            onBackPressHandled = {},
            onFeatureExited = {},
            onSearchQueryChanged = {},
            onSearchCloseClicked = {},
            onSearchClicked = {},
            onOpenUpgradeAccountClicked = {},
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
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterScreen(
            uiState = uiState,
            snackbarHostState = SnackbarHostState(),
            onDeviceClicked = {},
            onDeviceMenuClicked = {},
            onBackupFolderClicked = {},
            onNonBackupFolderClicked = {},
            onInfoOptionClicked = {},
            onAddNewSyncOptionClicked = {},
            onCameraUploadsClicked = {},
            onRenameDeviceOptionClicked = {},
            onRenameDeviceCancelled = {},
            onRenameDeviceSuccessful = {},
            onRenameDeviceSuccessfulSnackbarShown = {},
            onBackPressHandled = {},
            onFeatureExited = {},
            onSearchQueryChanged = {},
            onSearchCloseClicked = {},
            onSearchClicked = {},
            onOpenUpgradeAccountClicked = {},
        )
    }
}

/**
 * A Preview Composable that only displays content from the "This device" section
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterContentWithOwnDeviceSectionOnlyPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterContent(
            itemsToDisplay = listOf(ownDeviceUINode),
            onDeviceClicked = {},
            onDeviceMenuClicked = {},
            onBackupFolderClicked = {},
            onNonBackupFolderClicked = {},
            onInfoClicked = {}
        )
    }
}

/**
 * A Preview Composable that only displays content from the "Other devices" section
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterContentWithOtherDevicesSectionOnlyPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterContent(
            itemsToDisplay = listOf(otherDeviceUINodeOne),
            onDeviceClicked = {},
            onDeviceMenuClicked = {},
            onBackupFolderClicked = {},
            onNonBackupFolderClicked = {},
            onInfoClicked = {},
        )
    }
}

/**
 * A Preview Composable that displays content from both "This device" and "Other devices" sections
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterContentWithBothDeviceSectionsPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterContent(
            itemsToDisplay = listOf(
                ownDeviceUINode,
                otherDeviceUINodeOne,
                otherDeviceUINodeTwo,
                otherDeviceUINodeThree,
            ),
            onDeviceClicked = {},
            onDeviceMenuClicked = {},
            onBackupFolderClicked = {},
            onNonBackupFolderClicked = {},
            onInfoClicked = {},
        )
    }
}

private val ownDeviceFolderUINode = NonBackupDeviceFolderUINode(
    id = "ABCD-EFGH",
    name = "Camera uploads",
    icon = FolderIconType.CameraUploads,
    status = DeviceCenterUINodeStatus.UpToDate,
    rootHandle = 789012L,
    localFolderPath = ""
)

private val ownDeviceFolderUINodeTwo = NonBackupDeviceFolderUINode(
    id = "IJKL-MNOP",
    name = "Media uploads",
    icon = FolderIconType.CameraUploads,
    status = DeviceCenterUINodeStatus.UpToDate,
    rootHandle = 789012L,
    localFolderPath = ""
)

private val ownDeviceUINode = OwnDeviceUINode(
    id = "1234-5678",
    name = "User's Pixel 6",
    icon = DeviceIconType.Android,
    status = DeviceCenterUINodeStatus.CameraUploadsDisabled,
    folders = emptyList(),
)

private val ownDeviceUINodeTwo = OwnDeviceUINode(
    id = "9876-5432",
    name = "Samsung Galaxy S23",
    icon = DeviceIconType.Android,
    status = DeviceCenterUINodeStatus.UpToDate,
    folders = listOf(ownDeviceFolderUINode, ownDeviceFolderUINodeTwo),
)

private val otherDeviceUINodeOne = OtherDeviceUINode(
    id = "1A2B-3C4D",
    name = "XXX' HP 360",
    icon = DeviceIconType.Windows,
    status = DeviceCenterUINodeStatus.UpToDate,
    folders = emptyList(),
)
private val otherDeviceUINodeTwo = OtherDeviceUINode(
    id = "5E6F-7G8H",
    name = "HUAWEI P30",
    icon = DeviceIconType.Android,
    status = DeviceCenterUINodeStatus.Offline,
    folders = emptyList(),
)
private val otherDeviceUINodeThree = OtherDeviceUINode(
    id = "9I1J-2K3L",
    name = "Macbook Pro",
    icon = DeviceIconType.Mac,
    status = DeviceCenterUINodeStatus.Offline,
    folders = emptyList(),
)