package mega.privacy.android.feature.devicecenter.ui.bottomsheet

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.body.BOTTOM_SHEET_BODY_OTHER_DEVICE
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.body.BOTTOM_SHEET_BODY_OWN_DEVICE
import mega.privacy.android.feature.devicecenter.ui.model.OtherDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.OwnDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceIconType
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class for [DeviceBottomSheetBody]
 */
@RunWith(AndroidJUnit4::class)
internal class DeviceBottomSheetBodyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the bottom sheet is displayed for a device under the own device category`() {
        val ownDeviceUINode = OwnDeviceUINode(
            id = "1234-5678",
            name = "Test Device",
            icon = DeviceIconType.Android,
            status = DeviceCenterUINodeStatus.UpToDate,
            folders = emptyList(),
        )
        composeTestRule.setContent {
            DeviceBottomSheetBody(
                device = ownDeviceUINode,
                isCameraUploadsEnabled = true,
                onRenameDeviceClicked = {},
                onInfoClicked = {},
                onAddNewSyncClicked = {},
                onAddBackupClicked = {},
                onBottomSheetDismissed = {},
                isFreeAccount = false,
                isBackupForAndroidEnabled = true,
                isAndroidSyncFeatureEnabled = true
            )
        }
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_CONTAINER)
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_HEADER)
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_BODY_OWN_DEVICE).assertIsDisplayed()
    }

    @Test
    fun `test that the bottom sheet is displayed for a device under the other devices category`() {
        val otherDeviceUINode = OtherDeviceUINode(
            id = "9012-3456",
            name = "Test Device 2",
            icon = DeviceIconType.IOS,
            status = DeviceCenterUINodeStatus.Blocked(specificErrorMessage = null),
            folders = emptyList(),
        )
        composeTestRule.setContent {
            DeviceBottomSheetBody(
                device = otherDeviceUINode,
                isCameraUploadsEnabled = true,
                onRenameDeviceClicked = {},
                onInfoClicked = {},
                onAddNewSyncClicked = {},
                onAddBackupClicked = {},
                onBottomSheetDismissed = {},
                isFreeAccount = false,
                isBackupForAndroidEnabled = true,
                isAndroidSyncFeatureEnabled = true
            )
        }
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_CONTAINER)
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_HEADER)
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_BODY_OTHER_DEVICE).assertIsDisplayed()
    }
}