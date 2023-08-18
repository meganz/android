package mega.privacy.android.feature.devicecenter.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterState
import mega.privacy.android.feature.devicecenter.ui.model.OtherDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.OwnDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceIconType
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

/**
 * Test class for [DeviceCenterScreen]
 */
@RunWith(AndroidJUnit4::class)
internal class DeviceCenterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val deviceCenterViewModel = mock<DeviceCenterViewModel>()

    @Test
    fun `test that the device center screen is shown`() {
        val uiState = DeviceCenterState(nodes = emptyList())
        deviceCenterViewModel.stub { on { state }.thenReturn(MutableStateFlow(uiState)) }
        composeTestRule.setContent { DeviceCenterScreen(deviceCenterViewModel) }

        composeTestRule.onNodeWithTag(DEVICE_CENTER_SCREEN_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that no device section is displayed if the user backup information is empty`() {
        val uiState = DeviceCenterState(nodes = emptyList())
        deviceCenterViewModel.stub { on { state }.thenReturn(MutableStateFlow(uiState)) }
        composeTestRule.setContent { DeviceCenterScreen(deviceCenterViewModel) }

        composeTestRule.onNodeWithTag(DEVICE_CENTER_LIST_VIEW).assertDoesNotExist()
        composeTestRule.onNodeWithTag(DEVICE_CENTER_THIS_DEVICE_HEADER).assertDoesNotExist()
        composeTestRule.onNodeWithTag(DEVICE_CENTER_OTHER_DEVICES_HEADER).assertDoesNotExist()
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
        val uiState = DeviceCenterState(nodes = listOf(ownDeviceUINode))
        deviceCenterViewModel.stub { on { state }.thenReturn(MutableStateFlow(uiState)) }
        composeTestRule.setContent { DeviceCenterScreen(deviceCenterViewModel) }

        composeTestRule.onNodeWithTag(DEVICE_CENTER_LIST_VIEW).assertIsDisplayed()
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
        val uiState = DeviceCenterState(nodes = listOf(otherDeviceUINode))
        deviceCenterViewModel.stub { on { state }.thenReturn(MutableStateFlow(uiState)) }
        composeTestRule.setContent { DeviceCenterScreen(deviceCenterViewModel) }

        composeTestRule.onNodeWithTag(DEVICE_CENTER_LIST_VIEW).assertIsDisplayed()
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
        val uiState = DeviceCenterState(nodes = listOf(ownDeviceUINode, otherDeviceUINode))
        deviceCenterViewModel.stub { on { state }.thenReturn(MutableStateFlow(uiState)) }
        composeTestRule.setContent { DeviceCenterScreen(deviceCenterViewModel) }

        composeTestRule.onNodeWithTag(DEVICE_CENTER_LIST_VIEW).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DEVICE_CENTER_THIS_DEVICE_HEADER).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DEVICE_CENTER_OTHER_DEVICES_HEADER).assertIsDisplayed()
    }
}