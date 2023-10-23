package mega.privacy.android.feature.devicecenter.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.privacy.android.core.ui.controls.appbar.AppBarType
import mega.privacy.android.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.core.ui.controls.lists.MenuActionHeader
import mega.privacy.android.core.ui.controls.snackbars.MegaSnackbar
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.feature.devicecenter.R
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.DeviceCenterBottomSheet
import mega.privacy.android.feature.devicecenter.ui.lists.DeviceCenterListViewItem
import mega.privacy.android.feature.devicecenter.ui.lists.loading.DeviceCenterLoadingScreen
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterState
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterUINode
import mega.privacy.android.feature.devicecenter.ui.model.DeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.DeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.NonBackupDeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.OtherDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.OwnDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceIconType
import mega.privacy.android.feature.devicecenter.ui.model.icon.FolderIconType
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import mega.privacy.android.feature.devicecenter.ui.renamedevice.RenameDeviceDialog

/**
 * Test tags for the Device Center Screen
 */
internal const val DEVICE_CENTER_TOOLBAR = "device_center_screen:mega_app_bar"
internal const val DEVICE_CENTER_THIS_DEVICE_HEADER =
    "device_center_content:menu_action_header_this_device"
internal const val DEVICE_CENTER_OTHER_DEVICES_HEADER =
    "device_center_content:menu_action_header_other_devices"

/**
 * A [Composable] that serves as the main View for the Device Center
 *
 * @param uiState The UI State
 * @param snackbarHostState The [SnackbarHostState]
 * @param onDeviceClicked Lambda that performs a specific action when a Device is clicked
 * @param onNodeMenuIconClicked Lambda that performs a specific action when the Node Menu Icon is
 * clicked
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
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun DeviceCenterScreen(
    uiState: DeviceCenterState,
    snackbarHostState: SnackbarHostState,
    onDeviceClicked: (DeviceUINode) -> Unit,
    onNodeMenuIconClicked: (DeviceCenterUINode) -> Unit,
    onCameraUploadsClicked: () -> Unit,
    onRenameDeviceOptionClicked: (DeviceUINode) -> Unit,
    onRenameDeviceCancelled: () -> Unit,
    onRenameDeviceSuccessful: () -> Unit,
    onRenameDeviceSuccessfulSnackbarShown: () -> Unit,
    onBackPressHandled: () -> Unit,
    onFeatureExited: () -> Unit,
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
    Scaffold(
        topBar = {
            MegaAppBar(
                modifier = Modifier.testTag(DEVICE_CENTER_TOOLBAR),
                appBarType = AppBarType.BACK_NAVIGATION,
                title = selectedDevice?.name
                    ?: stringResource(R.string.device_center_top_app_bar_title),
                subtitle = null,
                elevation = 0.dp,
                onNavigationPressed = {
                    if (modalSheetState.isVisible) {
                        coroutineScope.launch { modalSheetState.hide() }
                    } else {
                        onBackPressHandled()
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                MegaSnackbar(snackbarData = snackbarData)
            }
        },
        content = { paddingValues ->
            if (!uiState.isInitialLoadingFinished) {
                DeviceCenterLoadingScreen()
            } else {
                DeviceCenterContent(
                    itemsToDisplay = uiState.itemsToDisplay,
                    onDeviceClicked = onDeviceClicked,
                    onNodeMenuIconClicked = { menuClickedNode ->
                        onNodeMenuIconClicked(menuClickedNode)
                        if (!modalSheetState.isVisible) {
                            coroutineScope.launch { modalSheetState.show() }
                        }
                    },
                    modifier = Modifier.padding(paddingValues),
                )
            }
            DeviceCenterBottomSheet(
                coroutineScope = coroutineScope,
                modalSheetState = modalSheetState,
                selectedNode = uiState.menuIconClickedNode ?: return@Scaffold,
                isCameraUploadsEnabled = uiState.isCameraUploadsEnabled,
                onCameraUploadsClicked = onCameraUploadsClicked,
                onRenameDeviceClicked = onRenameDeviceOptionClicked,
                onShowInBackupsClicked = {},
                onShowInCloudDriveClicked = {},
                onInfoClicked = {},
            )
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

/**
 * A [Composable] that displays the User's Backup information
 *
 * @param itemsToDisplay The list of Backup Devices / Device Folders to be displayed
 * @param onDeviceClicked Lambda that performs a specific action when a Device is clicked
 * @param onNodeMenuIconClicked Lambda that performs a specific action when the Node Menu Icon is clicked
 * @param modifier The Modifier object
 */
@Composable
private fun DeviceCenterContent(
    itemsToDisplay: List<DeviceCenterUINode>,
    onDeviceClicked: (DeviceUINode) -> Unit,
    onNodeMenuIconClicked: (DeviceCenterUINode) -> Unit,
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
            // User is in Device Folder View
            if (deviceFolders.isNotEmpty()) {
                items(count = deviceFolders.size, key = {
                    deviceFolders[it].id
                }) { itemIndex ->
                    DeviceCenterListViewItem(
                        uiNode = deviceFolders[itemIndex],
                        onMenuClicked = { node -> onNodeMenuIconClicked(node) },
                    )
                }
                // User is in Device View
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
                            onMenuClicked = { node -> onNodeMenuIconClicked(node) },
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
                            onMenuClicked = { node -> onNodeMenuIconClicked(node) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * A Preview Composable that shows the Loading Screen
 */
@CombinedThemePreviews
@Composable
private fun PreviewDeviceCenterInInitialLoading() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterScreen(
            uiState = DeviceCenterState(),
            snackbarHostState = SnackbarHostState(),
            onDeviceClicked = {},
            onNodeMenuIconClicked = {},
            onCameraUploadsClicked = {},
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
private fun PreviewDeviceCenterInDeviceView() {
    val uiState = DeviceCenterState(
        devices = listOf(
            ownDeviceUINode,
            otherDeviceUINodeOne,
            otherDeviceUINodeTwo,
            otherDeviceUINodeThree,
        ),
        isInitialLoadingFinished = true,
    )
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterScreen(
            uiState = uiState,
            snackbarHostState = SnackbarHostState(),
            onDeviceClicked = {},
            onNodeMenuIconClicked = {},
            onCameraUploadsClicked = {},
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
private fun PreviewDeviceCenterInFolderView() {
    val uiState = DeviceCenterState(
        devices = listOf(ownDeviceUINodeTwo),
        isInitialLoadingFinished = true,
        selectedDevice = ownDeviceUINodeTwo,
    )
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterScreen(
            uiState = uiState,
            snackbarHostState = SnackbarHostState(),
            onDeviceClicked = {},
            onNodeMenuIconClicked = {},
            onCameraUploadsClicked = {},
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
 * A Preview Composable that only displays content from the "This device" section
 */
@CombinedThemePreviews
@Composable
private fun PreviewDeviceCenterContentWithOwnDeviceSectionOnly() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterContent(
            itemsToDisplay = listOf(ownDeviceUINode),
            onDeviceClicked = {},
            onNodeMenuIconClicked = {},
        )
    }
}

/**
 * A Preview Composable that only displays content from the "Other devices" section
 */
@CombinedThemePreviews
@Composable
private fun PreviewDeviceCenterContentWithOtherDevicesSectionOnly() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterContent(
            itemsToDisplay = listOf(otherDeviceUINodeOne),
            onDeviceClicked = {},
            onNodeMenuIconClicked = {},
        )
    }
}

/**
 * A Preview Composable that displays content from both "This device" and "Other devices" sections
 */
@CombinedThemePreviews
@Composable
private fun PreviewDeviceCenterContentWithBothDeviceSections() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterContent(
            itemsToDisplay = listOf(
                ownDeviceUINode,
                otherDeviceUINodeOne,
                otherDeviceUINodeTwo,
                otherDeviceUINodeThree,
            ),
            onDeviceClicked = {},
            onNodeMenuIconClicked = {},
        )
    }
}

private val ownDeviceFolderUINode = NonBackupDeviceFolderUINode(
    id = "ABCD-EFGH",
    name = "Camera uploads",
    icon = FolderIconType.CameraUploads,
    status = DeviceCenterUINodeStatus.UpToDate
)

private val ownDeviceFolderUINodeTwo = NonBackupDeviceFolderUINode(
    id = "IJKL-MNOP",
    name = "Media uploads",
    icon = FolderIconType.CameraUploads,
    status = DeviceCenterUINodeStatus.UpToDate,
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