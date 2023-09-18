package mega.privacy.android.feature.devicecenter.ui.bottomsheet

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.body.BOTTOM_SHEET_BODY_BACKUP_FOLDER
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.body.BOTTOM_SHEET_BODY_NON_BACKUP_FOLDER
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.body.BOTTOM_SHEET_BODY_OTHER_DEVICE
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.body.BOTTOM_SHEET_BODY_OWN_DEVICE
import mega.privacy.android.feature.devicecenter.ui.model.BackupDeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.NonBackupDeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.OtherDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.OwnDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceIconType
import mega.privacy.android.feature.devicecenter.ui.model.icon.FolderIconType
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class for [DeviceCenterBottomSheet]
 */
@OptIn(ExperimentalMaterialApi::class)
@RunWith(AndroidJUnit4::class)
internal class DeviceCenterBottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the bottom sheet is a displayed for a device under the own device category`() {
        val ownDeviceUINode = OwnDeviceUINode(
            id = "1234-5678",
            name = "Test Device",
            icon = DeviceIconType.Android,
            status = DeviceCenterUINodeStatus.UpToDate,
            folders = emptyList(),
        )
        composeTestRule.setContent {
            DeviceCenterBottomSheet(
                coroutineScope = rememberCoroutineScope(),
                modalSheetState = ModalBottomSheetState(
                    initialValue = ModalBottomSheetValue.Expanded,
                    isSkipHalfExpanded = false,
                ),
                selectedNode = ownDeviceUINode,
                isCameraUploadsEnabled = true,
                onCameraUploadsClicked = {},
                onRenameDeviceClicked = {},
                onShowInBackupsClicked = {},
                onShowInCloudDriveClicked = {},
                onInfoClicked = {},
            )
        }
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_CONTAINER)
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_HEADER)
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_BODY_OWN_DEVICE).assertIsDisplayed()
    }

    @Test
    fun `test that the bottom sheet is a displayed for a device under the other devices category`() {
        val otherDeviceUINode = OtherDeviceUINode(
            id = "9012-3456",
            name = "Test Device 2",
            icon = DeviceIconType.IOS,
            status = DeviceCenterUINodeStatus.Blocked,
            folders = emptyList(),
        )
        composeTestRule.setContent {
            DeviceCenterBottomSheet(
                coroutineScope = rememberCoroutineScope(),
                modalSheetState = ModalBottomSheetState(
                    initialValue = ModalBottomSheetValue.Expanded,
                    isSkipHalfExpanded = false,
                ),
                selectedNode = otherDeviceUINode,
                isCameraUploadsEnabled = true,
                onCameraUploadsClicked = {},
                onRenameDeviceClicked = {},
                onShowInBackupsClicked = {},
                onShowInCloudDriveClicked = {},
                onInfoClicked = {},
            )
        }
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_CONTAINER)
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_HEADER)
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_BODY_OTHER_DEVICE).assertIsDisplayed()
    }

    @Test
    fun `test that the bottom sheet is a displayed for a backup folder`() {
        val backupDeviceFolderUINode = BackupDeviceFolderUINode(
            id = "ABCD-EFGH",
            name = "PC Backup Folder",
            icon = FolderIconType.Backup,
            status = DeviceCenterUINodeStatus.UpToDate,
        )
        composeTestRule.setContent {
            DeviceCenterBottomSheet(
                coroutineScope = rememberCoroutineScope(),
                modalSheetState = ModalBottomSheetState(
                    initialValue = ModalBottomSheetValue.Expanded,
                    isSkipHalfExpanded = false,
                ),
                selectedNode = backupDeviceFolderUINode,
                isCameraUploadsEnabled = true,
                onCameraUploadsClicked = {},
                onRenameDeviceClicked = {},
                onShowInBackupsClicked = {},
                onShowInCloudDriveClicked = {},
                onInfoClicked = {},
            )
        }
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_CONTAINER)
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_HEADER)
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_BODY_BACKUP_FOLDER).assertIsDisplayed()
    }

    @Test
    fun `test that the bottom sheet is a displayed for a non backup folder`() {
        val nonBackupDeviceFolderUINode = NonBackupDeviceFolderUINode(
            id = "IJKL-MNOP",
            name = "Camera Uploads Primary Folder",
            icon = FolderIconType.CameraUploads,
            status = DeviceCenterUINodeStatus.UpToDate,
        )
        composeTestRule.setContent {
            DeviceCenterBottomSheet(
                coroutineScope = rememberCoroutineScope(),
                modalSheetState = ModalBottomSheetState(
                    initialValue = ModalBottomSheetValue.Expanded,
                    isSkipHalfExpanded = false,
                ),
                selectedNode = nonBackupDeviceFolderUINode,
                isCameraUploadsEnabled = true,
                onCameraUploadsClicked = {},
                onRenameDeviceClicked = {},
                onShowInBackupsClicked = {},
                onShowInCloudDriveClicked = {},
                onInfoClicked = {},
            )
        }
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_CONTAINER)
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_HEADER)
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_BODY_NON_BACKUP_FOLDER).assertIsDisplayed()
    }
}