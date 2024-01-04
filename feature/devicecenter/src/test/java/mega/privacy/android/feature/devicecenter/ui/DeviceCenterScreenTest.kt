package mega.privacy.android.feature.devicecenter.ui

import androidx.compose.material.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.BOTTOM_SHEET_CONTAINER
import mega.privacy.android.feature.devicecenter.ui.lists.loading.DEVICE_CENTER_LOADING_SCREEN
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterState
import mega.privacy.android.feature.devicecenter.ui.model.NonBackupDeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.OtherDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.OwnDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceIconType
import mega.privacy.android.feature.devicecenter.ui.model.icon.FolderIconType
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import mega.privacy.android.feature.devicecenter.ui.renamedevice.RENAME_DEVICE_DIALOG_TAG
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
        val uiState = DeviceCenterState(
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
            )
        }
        composeTestRule.onNodeWithTag(DEVICE_CENTER_TOOLBAR).assertIsDisplayed()
    }

    @Test
    fun `test that nothing is displayed if the user backup information is empty`() {
        val uiState = DeviceCenterState(isInitialLoadingFinished = true)
        composeTestRule.setContent {
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
        val uiState = DeviceCenterState(
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
            )
        }
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_CONTAINER).assertIsDisplayed()
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
        val uiState = DeviceCenterState(
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
        val uiState = DeviceCenterState(
            devices = listOf(otherDeviceUINode),
            isInitialLoadingFinished = true,
        )
        composeTestRule.setContent {
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
        val uiState = DeviceCenterState(
            devices = listOf(ownDeviceUINode, otherDeviceUINode),
            isInitialLoadingFinished = true,
        )
        composeTestRule.setContent {
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
        )
        val ownDeviceUINode = OwnDeviceUINode(
            id = "1234-5678",
            name = "Own Device",
            icon = DeviceIconType.Android,
            status = DeviceCenterUINodeStatus.UpToDate,
            folders = listOf(ownDeviceFolderUINode),
        )
        val uiState = DeviceCenterState(
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
            )
        }
        composeTestRule.onNodeWithTag(DEVICE_CENTER_THIS_DEVICE_HEADER).assertDoesNotExist()
        composeTestRule.onNodeWithTag(DEVICE_CENTER_OTHER_DEVICES_HEADER).assertDoesNotExist()
    }

    @Test
    fun `test that the loading screen is shown`() {
        composeTestRule.setContent {
            DeviceCenterScreen(
                uiState = DeviceCenterState(),
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
            )
        }
        composeTestRule.onNodeWithTag(DEVICE_CENTER_LOADING_SCREEN).assertIsDisplayed()
    }
}