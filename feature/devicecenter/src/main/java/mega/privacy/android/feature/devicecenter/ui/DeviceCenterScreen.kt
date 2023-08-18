package mega.privacy.android.feature.devicecenter.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.core.ui.controls.lists.MenuActionHeader
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.feature.devicecenter.R
import mega.privacy.android.feature.devicecenter.ui.lists.DeviceCenterListViewItem
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterUINode
import mega.privacy.android.feature.devicecenter.ui.model.OtherDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.OwnDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceIconType
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus

/**
 * Test tags for the Device Center Screen
 */
internal const val DEVICE_CENTER_SCREEN_TAG = "device_center_screen:box"
internal const val DEVICE_CENTER_THIS_DEVICE_HEADER =
    "device_center_content:menu_action_header_this_device"
internal const val DEVICE_CENTER_OTHER_DEVICES_HEADER =
    "device_center_content:menu_action_header_other_devices"
internal const val DEVICE_CENTER_LIST_VIEW = "device_center_content:lazy_column_display_items"

/**
 * A [Composable] that serves as the main View for the Device Center
 *
 * @param deviceCenterViewModel The View Model to execute business logic
 */
@Composable
internal fun DeviceCenterScreen(
    deviceCenterViewModel: DeviceCenterViewModel = hiltViewModel(),
) {
    val uiState by deviceCenterViewModel.state.collectAsStateWithLifecycle()
    Box(
        modifier = Modifier
            .testTag(DEVICE_CENTER_SCREEN_TAG)
            .fillMaxSize(),
    )
    DeviceCenterContent(uiState.nodes)
}

/**
 * A [Composable] that displays the User's Backup information
 *
 * @param backupDevices The list of Backup Devices
 */
@Composable
private fun DeviceCenterContent(backupDevices: List<DeviceCenterUINode>) {
    if (backupDevices.isNotEmpty()) {
        val currentlyUsedDevices = backupDevices.filterIsInstance<OwnDeviceUINode>()
        val otherDevices = backupDevices.filterIsInstance<OtherDeviceUINode>()

        LazyColumn(
            modifier = Modifier.testTag(DEVICE_CENTER_LIST_VIEW),
            state = LazyListState(),
        ) {
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
                        onMenuClick = {},
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
                        onMenuClick = {},
                    )
                }
            }
        }
    }
}

/**
 * A Preview Composable that only displays content from the "This device" section
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterContentWithOwnDeviceSectionOnly() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterContent(listOf(ownDeviceUINode))
    }
}

/**
 * A Preview Composable that only displays content from the "Other devices" section
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterContentWithOtherDevicesSectionOnly() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterContent(listOf(otherDeviceUINodeOne))
    }
}

/**
 * A Preview Composable that displays content from both "This device" and "Other devices" sections
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterContentWithBothDeviceSections() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterContent(
            listOf(
                ownDeviceUINode,
                otherDeviceUINodeOne,
                otherDeviceUINodeTwo,
                otherDeviceUINodeThree,
            )
        )
    }
}

private val ownDeviceUINode = OwnDeviceUINode(
    id = "1234-5678",
    name = "User's Pixel 6",
    icon = DeviceIconType.Android,
    status = DeviceCenterUINodeStatus.NoCameraUploads,
    folders = emptyList(),
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