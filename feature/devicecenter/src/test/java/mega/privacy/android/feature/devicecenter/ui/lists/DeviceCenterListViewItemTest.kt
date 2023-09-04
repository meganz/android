package mega.privacy.android.feature.devicecenter.ui.lists

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.feature.devicecenter.ui.model.OwnDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceIconType
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class for [DeviceCenterListViewItem]
 */
@RunWith(AndroidJUnit4::class)
internal class DeviceCenterListViewItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the device center list item is displayed`() {
        composeTestRule.setContent {
            DeviceCenterListViewItem(
                uiNode = OwnDeviceUINode(
                    id = "1234-5678",
                    name = "Android Device",
                    icon = DeviceIconType.Android,
                    status = DeviceCenterUINodeStatus.UpToDate,
                    folders = emptyList(),
                ),
                onDeviceClicked = {},
                onMenuClicked = {},
            )
        }
        composeTestRule.onNodeWithTag(DEVICE_CENTER_LIST_VIEW_ITEM_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DEVICE_CENTER_LIST_VIEW_ITEM_DIVIDER_TAG).assertIsDisplayed()
    }
}