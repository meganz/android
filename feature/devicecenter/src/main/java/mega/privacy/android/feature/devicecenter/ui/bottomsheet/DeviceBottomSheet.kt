package mega.privacy.android.feature.devicecenter.ui.bottomsheet


import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.feature.devicecenter.R
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.body.OtherDeviceBottomSheetBody
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.body.OwnDeviceBottomSheetBody
import mega.privacy.android.feature.devicecenter.ui.model.DeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.OtherDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.OwnDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceCenterUINodeIcon
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceIconType
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import mega.privacy.android.legacy.core.ui.controls.lists.MenuActionNodeHeaderWithBody
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Test Tags for the Device Bottom Sheet
 */
internal const val BOTTOM_SHEET_CONTAINER =
    "device_bottom_sheet:menu_action_bottom_sheet_container"
internal const val BOTTOM_SHEET_HEADER =
    "device_bottom_sheet:menu_action_node_header_with_body_node_information"

/**
 * A [Composable] Bottom Sheet shown when clicking a Device's Context Menu Icon, showing all
 * available Options
 *
 * @param coroutineScope The [CoroutineScope] used to hide the Bottom Sheet
 * @param modalSheetState The Bottom Sheet State
 * @param device The selected [DeviceUINode]
 * @param isCameraUploadsEnabled true if Camera Uploads is Enabled, and false if otherwise
 * @param onCameraUploadsClicked Lambda that is executed when the "Camera uploads" Tile is selected
 * @param onRenameDeviceClicked Lambda that is executed when the "Rename" Tile is selected
 * @param onInfoClicked Lambda that is executed when the "Info" Tile is selected
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun DeviceBottomSheet(
    coroutineScope: CoroutineScope,
    modalSheetState: ModalBottomSheetState,
    device: DeviceUINode,
    isCameraUploadsEnabled: Boolean,
    onCameraUploadsClicked: () -> Unit,
    onRenameDeviceClicked: (DeviceUINode) -> Unit,
    onInfoClicked: () -> Unit,
) {
    BottomSheet(
        modifier = Modifier.testTag(BOTTOM_SHEET_CONTAINER),
        modalSheetState = modalSheetState,
        sheetHeader = {
            MenuActionNodeHeaderWithBody(
                modifier = Modifier.testTag(BOTTOM_SHEET_HEADER),
                title = getTitleText(device.name),
                body = getStatusText(device.status),
                nodeIcon = device.icon.iconRes,
                bodyIcon = device.status.icon,
                bodyColor = getStatusColor(device.status),
                bodyIconColor = getStatusColor(device.status),
                nodeIconColor = getNodeIconColor(device.icon),
            )
        },
        sheetBody = {
            // Display the Options depending on the Device type
            when (device) {
                is OwnDeviceUINode -> {
                    OwnDeviceBottomSheetBody(
                        isCameraUploadsEnabled = isCameraUploadsEnabled,
                        onCameraUploadsClicked = {
                            coroutineScope.launch { modalSheetState.hide() }
                            onCameraUploadsClicked.invoke()
                        },
                        onRenameDeviceClicked = {
                            coroutineScope.launch { modalSheetState.hide() }
                            onRenameDeviceClicked(device)
                        },
                        onInfoClicked = {
                            coroutineScope.launch { modalSheetState.hide() }
                            onInfoClicked.invoke()
                        },
                    )
                }

                is OtherDeviceUINode -> {
                    OtherDeviceBottomSheetBody(
                        onRenameDeviceClicked = {
                            coroutineScope.launch { modalSheetState.hide() }
                            onRenameDeviceClicked(device)
                        },
                        onInfoClicked = {
                            coroutineScope.launch { modalSheetState.hide() }
                            onInfoClicked.invoke()
                        },
                    )
                }
            }
        },
    )
}

/**
 * Retrieves the Title Text to be displayed in [DeviceBottomSheet]
 *
 * @param uiNodeName The Node name
 * @return The corresponding Title Text
 */
@Composable
private fun getTitleText(uiNodeName: String) = uiNodeName.ifBlank {
    stringResource(R.string.device_center_list_view_item_title_unknown_device)
}

/**
 * Retrieves the Status Text to be displayed in [DeviceBottomSheet]
 *
 * @param uiNodeStatus The [DeviceCenterUINodeStatus]
 * @return The corresponding Status Text
 */
@Composable
private fun getStatusText(uiNodeStatus: DeviceCenterUINodeStatus) =
    if (uiNodeStatus is DeviceCenterUINodeStatus.SyncingWithPercentage) {
        // Apply String Formatting for this UI Status
        stringResource(uiNodeStatus.name, uiNodeStatus.progress)
    } else {
        stringResource(uiNodeStatus.name)
    }

/**
 * Retrieves the Status Color to be applied in the Status Text and Icon of [DeviceBottomSheet]
 *
 * @param uiNodeStatus The [DeviceCenterUINodeStatus]
 * @return The corresponding Status Color
 */
@Composable
private fun getStatusColor(uiNodeStatus: DeviceCenterUINodeStatus) =
    uiNodeStatus.color ?: MaterialTheme.colors.textColorSecondary

/**
 * Retrieves the Color to be applied in the Node Icon of [DeviceBottomSheet]
 *
 * @param uiNodeIcon The [DeviceCenterUINodeIcon]
 * @return The corresponding Node Icon Color
 */
@Composable
private fun getNodeIconColor(uiNodeIcon: DeviceCenterUINodeIcon) =
    if (uiNodeIcon.applySecondaryColorTint) {
        MaterialTheme.colors.textColorSecondary
    } else {
        null
    }

/**
 * A Preview Composable that displays the Device Bottom Sheet with its Options
 */
@OptIn(ExperimentalMaterialApi::class)
@CombinedThemePreviews
@Composable
private fun PreviewDeviceBottomSheet(
    @PreviewParameter(DeviceBottomSheetPreviewProvider::class) device: DeviceUINode,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        DeviceBottomSheet(
            coroutineScope = rememberCoroutineScope(),
            modalSheetState = ModalBottomSheetState(
                initialValue = ModalBottomSheetValue.Expanded,
                isSkipHalfExpanded = false,
                density = LocalDensity.current,
            ),
            device = device,
            isCameraUploadsEnabled = true,
            onCameraUploadsClicked = {},
            onRenameDeviceClicked = {},
            onInfoClicked = {},
        )
    }
}

/**
 * A class that provides Preview Parameters for the [DeviceBottomSheet]
 */
private class DeviceBottomSheetPreviewProvider : PreviewParameterProvider<DeviceUINode> {
    override val values: Sequence<DeviceUINode>
        get() = sequenceOf(
            OwnDeviceUINode(
                id = "1234-5678",
                name = "Own Device",
                icon = DeviceIconType.Android,
                status = DeviceCenterUINodeStatus.UpToDate,
                folders = emptyList(),
            ),
            OtherDeviceUINode(
                id = "9012-3456",
                name = "Other Device",
                icon = DeviceIconType.IOS,
                status = DeviceCenterUINodeStatus.Offline,
                folders = emptyList(),
            )
        )
}