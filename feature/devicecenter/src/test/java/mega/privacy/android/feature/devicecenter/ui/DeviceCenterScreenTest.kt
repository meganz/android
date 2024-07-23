package mega.privacy.android.feature.devicecenter.ui

import androidx.compose.material.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.BOTTOM_SHEET_CONTAINER
import mega.privacy.android.feature.devicecenter.ui.lists.loading.DEVICE_CENTER_LOADING_SCREEN
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterUiState
import mega.privacy.android.feature.devicecenter.ui.model.NonBackupDeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.OtherDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.OwnDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceIconType
import mega.privacy.android.feature.devicecenter.ui.model.icon.FolderIconType
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import mega.privacy.android.feature.devicecenter.ui.renamedevice.RENAME_DEVICE_DIALOG_TAG
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class for [DeviceCenterScreen]
 */
@RunWith(AndroidJUnit4::class)
internal class DeviceCenterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the top app bar is shown`() {
        val ownDeviceUINode = OwnDeviceUINode(
            id = "1234-5678",
            name = "Own Device",
            icon = DeviceIconType.Android,
            status = DeviceCenterUINodeStatus.UpToDate,
            folders = emptyList(),
        )
        val uiState = DeviceCenterUiState(
            devices = listOf(ownDeviceUINode),
            isInitialLoadingFinished = true,
        )
        composeTestRule.setContent {
            DeviceCenterScreen(
                uiState = uiState,
                snackbarHostState = SnackbarHostState(),
                onDeviceClicked = {},
                onDeviceMenuClicked = {},
                onBackupFolderClicked = {},
                onNonBackupFolderClicked = {},
                onInfoOptionClicked = {},
                onAddNewSyncOptionClicked = {},
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
        composeTestRule.onNodeWithTag(DEVICE_CENTER_TOOLBAR).assertIsDisplayed()
    }

    @Test
    fun `test that nothing is displayed if the user backup information is empty`() {
        val uiState = DeviceCenterUiState(isInitialLoadingFinished = true)
        composeTestRule.setContent {
            DeviceCenterScreen(
                uiState = uiState,
                snackbarHostState = SnackbarHostState(),
                onDeviceClicked = {},
                onDeviceMenuClicked = {},
                onBackupFolderClicked = {},
                onNonBackupFolderClicked = {},
                onInfoOptionClicked = {},
                onAddNewSyncOptionClicked = {},
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
        composeTestRule.onNodeWithTag(DEVICE_CENTER_THIS_DEVICE_HEADER).assertDoesNotExist()
        composeTestRule.onNodeWithTag(DEVICE_CENTER_OTHER_DEVICES_HEADER).assertDoesNotExist()
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_CONTAINER).assertDoesNotExist()
        composeTestRule.onNodeWithTag(RENAME_DEVICE_DIALOG_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the bottom dialog is shown when the menu icon of a node is selected`() {
        val ownDeviceUINode = OwnDeviceUINode(
            id = "1234-5678",
            name = "Own Device",
            icon = DeviceIconType.Android,
            status = DeviceCenterUINodeStatus.UpToDate,
            folders = emptyList(),
        )
        val uiState = DeviceCenterUiState(
            devices = listOf(ownDeviceUINode),
            isInitialLoadingFinished = true,
            menuClickedDevice = ownDeviceUINode,
        )
        composeTestRule.setContent {
            DeviceCenterScreen(
                uiState = uiState,
                snackbarHostState = SnackbarHostState(),
                onDeviceClicked = {},
                onDeviceMenuClicked = {},
                onBackupFolderClicked = {},
                onNonBackupFolderClicked = {},
                onInfoOptionClicked = {},
                onAddNewSyncOptionClicked = {},
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
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_CONTAINER).assertExists()
    }

    @Test
    fun `test that only the own device section is displayed`() {
        val ownDeviceUINode = OwnDeviceUINode(
            id = "1234-5678",
            name = "Own Device",
            icon = DeviceIconType.Android,
            status = DeviceCenterUINodeStatus.UpToDate,
            folders = emptyList(),
        )
        val uiState = DeviceCenterUiState(
            devices = listOf(ownDeviceUINode),
            isInitialLoadingFinished = true,
            isNetworkConnected = true,
        )
        composeTestRule.setContent {
            DeviceCenterScreen(
                uiState = uiState,
                snackbarHostState = SnackbarHostState(),
                onDeviceClicked = {},
                onDeviceMenuClicked = {},
                onBackupFolderClicked = {},
                onNonBackupFolderClicked = {},
                onInfoOptionClicked = {},
                onAddNewSyncOptionClicked = {},
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
        composeTestRule.onNodeWithTag(DEVICE_CENTER_THIS_DEVICE_HEADER).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DEVICE_CENTER_OTHER_DEVICES_HEADER).assertDoesNotExist()
    }

    @Test
    fun `test that only the other devices section is displayed`() {
        val otherDeviceUINode = OtherDeviceUINode(
            id = "1A2B-3C4D",
            name = "Other Device",
            icon = DeviceIconType.PC,
            status = DeviceCenterUINodeStatus.UpToDate,
            folders = emptyList(),
        )
        val uiState = DeviceCenterUiState(
            devices = listOf(otherDeviceUINode),
            isInitialLoadingFinished = true,
            isNetworkConnected = true,
        )
        composeTestRule.setContent {
            DeviceCenterScreen(
                uiState = uiState,
                snackbarHostState = SnackbarHostState(),
                onDeviceClicked = {},
                onDeviceMenuClicked = {},
                onBackupFolderClicked = {},
                onNonBackupFolderClicked = {},
                onInfoOptionClicked = {},
                onAddNewSyncOptionClicked = {},
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
        composeTestRule.onNodeWithTag(DEVICE_CENTER_THIS_DEVICE_HEADER).assertDoesNotExist()
        composeTestRule.onNodeWithTag(DEVICE_CENTER_OTHER_DEVICES_HEADER).assertIsDisplayed()
    }

    @Test
    fun `test that both the own device and other devices sections are displayed`() {
        val ownDeviceUINode = OwnDeviceUINode(
            id = "1234-5678",
            name = "Own Device",
            icon = DeviceIconType.Android,
            status = DeviceCenterUINodeStatus.UpToDate,
            folders = emptyList(),
        )
        val otherDeviceUINode = OtherDeviceUINode(
            id = "1A2B-3C4D",
            name = "Other Device",
            icon = DeviceIconType.PC,
            status = DeviceCenterUINodeStatus.UpToDate,
            folders = emptyList(),
        )
        val uiState = DeviceCenterUiState(
            devices = listOf(ownDeviceUINode, otherDeviceUINode),
            isInitialLoadingFinished = true,
            isNetworkConnected = true,
        )
        composeTestRule.setContent {
            DeviceCenterScreen(
                uiState = uiState,
                snackbarHostState = SnackbarHostState(),
                onDeviceClicked = {},
                onDeviceMenuClicked = {},
                onBackupFolderClicked = {},
                onNonBackupFolderClicked = {},
                onInfoOptionClicked = {},
                onAddNewSyncOptionClicked = {},
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
        composeTestRule.onNodeWithTag(DEVICE_CENTER_THIS_DEVICE_HEADER).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DEVICE_CENTER_OTHER_DEVICES_HEADER).assertIsDisplayed()
    }

    @Test
    fun `test that no device sections are displayed when in folder view`() {
        val ownDeviceFolderUINode = NonBackupDeviceFolderUINode(
            id = "ABCD-EFGH",
            name = "Camera uploads",
            icon = FolderIconType.CameraUploads,
            status = DeviceCenterUINodeStatus.UpToDate,
            rootHandle = 789012L,
            localFolderPath = "storage/emulated/0/DCIM/Camera",
        )
        val ownDeviceUINode = OwnDeviceUINode(
            id = "1234-5678",
            name = "Own Device",
            icon = DeviceIconType.Android,
            status = DeviceCenterUINodeStatus.UpToDate,
            folders = listOf(ownDeviceFolderUINode),
        )
        val uiState = DeviceCenterUiState(
            devices = listOf(ownDeviceUINode),
            isInitialLoadingFinished = true,
            selectedDevice = ownDeviceUINode,
        )
        composeTestRule.setContent {
            DeviceCenterScreen(
                uiState = uiState,
                snackbarHostState = SnackbarHostState(),
                onDeviceClicked = {},
                onDeviceMenuClicked = {},
                onBackupFolderClicked = {},
                onNonBackupFolderClicked = {},
                onInfoOptionClicked = {},
                onAddNewSyncOptionClicked = {},
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
        composeTestRule.onNodeWithTag(DEVICE_CENTER_THIS_DEVICE_HEADER).assertDoesNotExist()
        composeTestRule.onNodeWithTag(DEVICE_CENTER_OTHER_DEVICES_HEADER).assertDoesNotExist()
    }

    @Test
    fun `test that the loading screen is shown`() {
        composeTestRule.setContent {
            DeviceCenterScreen(
                uiState = DeviceCenterUiState(isNetworkConnected = true),
                snackbarHostState = SnackbarHostState(),
                onDeviceClicked = {},
                onDeviceMenuClicked = {},
                onBackupFolderClicked = {},
                onNonBackupFolderClicked = {},
                onInfoOptionClicked = {},
                onAddNewSyncOptionClicked = {},
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
        composeTestRule.onNodeWithTag(DEVICE_CENTER_LOADING_SCREEN).assertIsDisplayed()
    }

    @Test
    fun `test that the no network state is shown`() {
        composeTestRule.setContent {
            DeviceCenterScreen(
                uiState = DeviceCenterUiState(),
                snackbarHostState = SnackbarHostState(),
                onDeviceClicked = {},
                onDeviceMenuClicked = {},
                onBackupFolderClicked = {},
                onNonBackupFolderClicked = {},
                onInfoOptionClicked = {},
                onAddNewSyncOptionClicked = {},
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
        composeTestRule.onNodeWithTag(DEVICE_CENTER_NO_NETWORK_STATE).assertIsDisplayed()
    }

    @Test
    fun `test that the nothing setup state is shown`() {
        val ownDeviceUINode = OwnDeviceUINode(
            id = "1234-5678",
            name = "Own Device",
            icon = DeviceIconType.Android,
            status = DeviceCenterUINodeStatus.CameraUploadsDisabled,
            folders = emptyList(),
        )
        val uiState = DeviceCenterUiState(
            devices = listOf(ownDeviceUINode),
            isCameraUploadsEnabled = false,
            isInitialLoadingFinished = true,
            selectedDevice = ownDeviceUINode,
            isNetworkConnected = true,
        )
        composeTestRule.setContent {
            DeviceCenterScreen(
                uiState = uiState,
                snackbarHostState = SnackbarHostState(),
                onDeviceClicked = {},
                onDeviceMenuClicked = {},
                onBackupFolderClicked = {},
                onNonBackupFolderClicked = {},
                onInfoOptionClicked = {},
                onAddNewSyncOptionClicked = {},
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
        composeTestRule.onNodeWithTag(DEVICE_CENTER_NOTHING_SETUP_STATE).assertIsDisplayed()
    }

    @Test
    fun `test that no results found state is shown`() {
        composeTestRule.setContent {
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
}