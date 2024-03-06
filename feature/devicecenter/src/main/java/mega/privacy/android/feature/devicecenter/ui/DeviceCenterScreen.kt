package mega.privacy.android.feature.devicecenter.ui

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.core.ui.controls.appbar.AppBarType
import mega.privacy.android.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.core.ui.controls.snackbars.MegaSnackbar
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.feature.devicecenter.R
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.DeviceBottomSheetBody
import mega.privacy.android.feature.devicecenter.ui.lists.DeviceCenterListViewItem
import mega.privacy.android.feature.devicecenter.ui.lists.loading.DeviceCenterLoadingScreen
import mega.privacy.android.feature.devicecenter.ui.model.BackupDeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterState
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterUINode
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
import mega.privacy.android.shared.theme.MegaAppTheme

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

/**
 * A [Composable] that serves as the main View for the Device Center
 *
 * @param uiState The UI State
 * @param snackbarHostState The [SnackbarHostState]
 * @param onDeviceClicked Lambda that performs a specific action when a Device is clicked
 * @param onDeviceMenuClicked Lambda that performs a specific action when a Device's Menu Icon is
 * clicked
 * @param onBackupFolderClicked Lambda that performs a specific action when a Backup Folder is clicked
 * @param onBackupFolderMenuClicked Lambda that performs a specific action when a Backup Folder's Menu
 * Icon is clicked
 * @param onNonBackupFolderClicked Lambda that performs a specific action when a Non Backup Folder
 * is clicked
 * @param onNonBackupFolderMenuClicked Lambda that performs a specific action when a Non-Backup
 * Folder's Menu Icon is clicked
 * @param onCameraUploadsClicked Lambda that performs a specific action when the User clicks the
 * "Camera uploads" Bottom Dialog Option
 * @param onRenameDeviceOptionClicked Lambda that performs a specific action when the User clicks
 * the "Rename" Bottom Dialog Option
 * @param onRenameDeviceCancelled Lambda that performs a specific action when cancelling the Rename
 * Device action
 * @param onRenameDeviceSuccessful Lambda that performs a specific action when the Rename Device
 * action is successful
 * @param onRenameDeviceSuccessfulSnackbarShown Lambda that performs a specific action when the
 * Rename Device success Snackbar has been displayed
 * @param onBackPressHandled Lambda that performs a specific action when the Composable handles the
 * Back Press
 * @param onFeatureExited Lambda that performs a specific action when the Device Center is exited
 * @param onActionPressed Action for each available option of the app bar menu
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun DeviceCenterScreen(
    uiState: DeviceCenterState,
    snackbarHostState: SnackbarHostState,
    onDeviceClicked: (DeviceUINode) -> Unit,
    onDeviceMenuClicked: (DeviceUINode) -> Unit,
    onBackupFolderClicked: (BackupDeviceFolderUINode) -> Unit,
    onBackupFolderMenuClicked: (BackupDeviceFolderUINode) -> Unit,
    onNonBackupFolderClicked: (NonBackupDeviceFolderUINode) -> Unit,
    onNonBackupFolderMenuClicked: (NonBackupDeviceFolderUINode) -> Unit,
    onCameraUploadsClicked: () -> Unit,
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
                onInfoClicked = {},
                onBottomSheetDismissed = {
                    coroutineScope.launch { modalSheetState.hide() }
                }
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
                                    onDeviceMenuClicked(deviceNode)
                                    if (!modalSheetState.isVisible) {
                                        coroutineScope.launch { modalSheetState.show() }
                                    }
                                },
                                onBackupFolderClicked = onBackupFolderClicked,
                                onBackupFolderMenuClicked = onBackupFolderMenuClicked,
                                onNonBackupFolderClicked = onNonBackupFolderClicked,
                                onNonBackupFolderMenuClicked = onNonBackupFolderMenuClicked,
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
        })
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun DeviceCenterAppBar(
    uiState: DeviceCenterState,
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
//                        if (uiState.isCameraUploadsEnabled) {
//                            list.add(DeviceMenuAction.Info)
//                        }
                        list.add(DeviceMenuAction.CameraUploads)
                    }

//                    else -> list.add(DeviceMenuAction.Info)
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
    DeviceCenterEmptyState(
        iconId = R.drawable.ic_device_center_no_network_state,
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
    DeviceCenterEmptyState(
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
    DeviceCenterEmptyState(
        iconId = iconPackR.drawable.ic_search_02,
        iconSize = 128.dp,
        iconDescription = "No results found for search",
        textId = R.string.device_center_empty_screen_no_results,
        testTag = DEVICE_CENTER_NO_ITEMS_FOUND_STATE
    )
}

/**
 * A [Composable] which displays an empty state
 *
 * @param iconId            [DrawableRes] ID to query the image file from.
 * @param iconSize          Size of the icon square in [Dp].
 * @param iconDescription   [String] used by accessibility services to describe what this image represents.
 * @param textId            [StringRes] ID of the text to display.
 * @param testTag           Tag to allow modified element to be found in tests.
 */
@Composable
private fun DeviceCenterEmptyState(
    @DrawableRes iconId: Int,
    iconSize: Dp,
    iconDescription: String,
    @StringRes textId: Int,
    testTag: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = iconId),
            contentDescription = iconDescription,
            modifier = Modifier
                .size(size = iconSize)
                .padding(bottom = 8.dp)
        )
        MegaText(
            text = stringResource(id = textId),
            textColor = TextColor.Primary,
            style = MaterialTheme.typography.subtitle2,
        )
    }
}

/**
 * A [Composable] that displays the User's Backup information
 *
 * @param itemsToDisplay The list of Backup Devices / Device Folders to be displayed
 * @param onDeviceClicked Lambda that performs a specific action when a Device is clicked
 * @param onDeviceMenuClicked Lambda that performs a specific action when a Device's Menu Icon is
 * clicked
 * @param onBackupFolderClicked Lambda that performs a specific action when a Backup Folder is clicked
 * @param onBackupFolderMenuClicked Lambda that performs a specific action when a Backup Folder's
 * Menu Icon is clicked
 * @param onNonBackupFolderMenuClicked Lambda that performs a specific action when a Non-Backup Folder's
 * Menu Icon is clicked
 * @param modifier The Modifier object
 */
@Composable
private fun DeviceCenterContent(
    itemsToDisplay: List<DeviceCenterUINode>,
    onDeviceClicked: (DeviceUINode) -> Unit,
    onDeviceMenuClicked: (DeviceUINode) -> Unit,
    onBackupFolderClicked: (BackupDeviceFolderUINode) -> Unit,
    onBackupFolderMenuClicked: (BackupDeviceFolderUINode) -> Unit,
    onNonBackupFolderClicked: (NonBackupDeviceFolderUINode) -> Unit,
    onNonBackupFolderMenuClicked: (NonBackupDeviceFolderUINode) -> Unit,
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
                        onBackupFolderMenuClicked = onBackupFolderMenuClicked,
                        onNonBackupFolderClicked = onNonBackupFolderClicked,
                        onNonBackupFolderMenuClicked = onNonBackupFolderMenuClicked,
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
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterScreen(
            uiState = DeviceCenterState(isInitialLoadingFinished = true),
            snackbarHostState = SnackbarHostState(),
            onDeviceClicked = {},
            onDeviceMenuClicked = {},
            onBackupFolderClicked = {},
            onBackupFolderMenuClicked = {},
            onNonBackupFolderClicked = {},
            onNonBackupFolderMenuClicked = {},
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
        )
    }
}

@CombinedThemePreviews
@Composable
private fun DeviceCenterNoItemsFoundPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterScreen(
            uiState = DeviceCenterState(
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
            onBackupFolderMenuClicked = {},
            onNonBackupFolderClicked = {},
            onNonBackupFolderMenuClicked = {},
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
        )
    }
}

/**
 * A Preview Composable that shows the Loading Screen
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterInInitialLoadingPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterScreen(
            uiState = DeviceCenterState(isNetworkConnected = true),
            snackbarHostState = SnackbarHostState(),
            onDeviceClicked = {},
            onDeviceMenuClicked = {},
            onBackupFolderClicked = {},
            onBackupFolderMenuClicked = {},
            onNonBackupFolderClicked = {},
            onNonBackupFolderMenuClicked = {},
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
        )
    }
}

/**
 * A Preview Composable that shows the Device Center in Device View
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterInDeviceViewPreview() {
    val uiState = DeviceCenterState(
        devices = listOf(
            ownDeviceUINode,
            otherDeviceUINodeOne,
            otherDeviceUINodeTwo,
            otherDeviceUINodeThree,
        ),
        isInitialLoadingFinished = true,
        isNetworkConnected = true,
    )
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterScreen(
            uiState = uiState,
            snackbarHostState = SnackbarHostState(),
            onDeviceClicked = {},
            onDeviceMenuClicked = {},
            onBackupFolderClicked = {},
            onBackupFolderMenuClicked = {},
            onNonBackupFolderClicked = {},
            onNonBackupFolderMenuClicked = {},
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
    val uiState = DeviceCenterState(
        devices = listOf(ownDeviceUINode),
        isInitialLoadingFinished = true,
        selectedDevice = ownDeviceUINode,
        isNetworkConnected = true,
    )
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterScreen(
            uiState = uiState,
            snackbarHostState = SnackbarHostState(),
            onDeviceClicked = {},
            onDeviceMenuClicked = {},
            onBackupFolderClicked = {},
            onBackupFolderMenuClicked = {},
            onNonBackupFolderClicked = {},
            onNonBackupFolderMenuClicked = {},
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
        )
    }
}

/**
 * A Preview Composable that shows the Device Center in Folder View (when the user selects a Device)
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterInFolderViewPreview() {
    val uiState = DeviceCenterState(
        devices = listOf(ownDeviceUINodeTwo),
        isInitialLoadingFinished = true,
        selectedDevice = ownDeviceUINodeTwo,
        isNetworkConnected = true,
    )
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterScreen(
            uiState = uiState,
            snackbarHostState = SnackbarHostState(),
            onDeviceClicked = {},
            onDeviceMenuClicked = {},
            onBackupFolderClicked = {},
            onBackupFolderMenuClicked = {},
            onNonBackupFolderClicked = {},
            onNonBackupFolderMenuClicked = {},
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
        )
    }
}

/**
 * A Preview Composable that only displays content from the "This device" section
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterContentWithOwnDeviceSectionOnlyPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterContent(
            itemsToDisplay = listOf(ownDeviceUINode),
            onDeviceClicked = {},
            onDeviceMenuClicked = {},
            onBackupFolderClicked = {},
            onBackupFolderMenuClicked = {},
            onNonBackupFolderClicked = {},
            onNonBackupFolderMenuClicked = {},
        )
    }
}

/**
 * A Preview Composable that only displays content from the "Other devices" section
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterContentWithOtherDevicesSectionOnlyPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterContent(
            itemsToDisplay = listOf(otherDeviceUINodeOne),
            onDeviceClicked = {},
            onDeviceMenuClicked = {},
            onBackupFolderClicked = {},
            onBackupFolderMenuClicked = {},
            onNonBackupFolderClicked = {},
            onNonBackupFolderMenuClicked = {},
        )
    }
}

/**
 * A Preview Composable that displays content from both "This device" and "Other devices" sections
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterContentWithBothDeviceSectionsPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
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
            onBackupFolderMenuClicked = {},
            onNonBackupFolderClicked = {},
            onNonBackupFolderMenuClicked = {},
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