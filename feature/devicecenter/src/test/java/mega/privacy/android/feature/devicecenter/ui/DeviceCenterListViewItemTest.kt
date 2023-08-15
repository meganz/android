package mega.privacy.android.feature.devicecenter.ui

import mega.privacy.android.feature.devicecenter.R as DeviceCenterR
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.feature.devicecenter.ui.entity.status.DeviceCenterUINodeStatus
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
                icon = DeviceCenterR.drawable.ic_device_android,
                name = "Android Device",
                status = DeviceCenterUINodeStatus.UpToDate,
                onMenuClick = {},
            )
        }
        composeTestRule.onNodeWithTag(DEVICE_CENTER_LIST_VIEW_ITEM_TAG).assertIsDisplayed()
    }
}