package mega.privacy.android.feature.devicecenter.ui.bottomsheet


import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.status.getStatusIconColor
import mega.privacy.android.shared.original.core.ui.controls.status.getStatusTextColor
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorSecondary
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
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

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
 * @param device The selected [DeviceUINode]
 * @param isCameraUploadsEnabled true if Camera Uploads is Enabled, and false if otherwise
 * @param onRenameDeviceClicked Lambda that is executed when the "Rename" Tile is selected
 * @param onInfoClicked Lambda that is executed when the "Info" Tile is selected
 * @param onAddNewSyncClicked Lambda that is executed when the "Add new sync" Tile is selected
 * @param onAddBackupClicked Lambda that is executed when the "Add backup" Tile is selected
 * @param onBottomSheetDismissed Lambda that is executed when the bottom sheet is dismissed
 * @param isFreeAccount True if is a Free account or False otherwise
 * @param isAndroidSyncFeatureEnabled True if the Android Sync feature is enabled or False otherwise
 */
@Composable
internal fun DeviceBottomSheetBody(
    device: DeviceUINode,
    isCameraUploadsEnabled: Boolean,
    onRenameDeviceClicked: (DeviceUINode) -> Unit,
    onInfoClicked: (DeviceUINode) -> Unit,
    onAddNewSyncClicked: (DeviceUINode) -> Unit,
    onAddBackupClicked: (DeviceUINode) -> Unit,
    onBottomSheetDismissed: () -> Unit,
    isFreeAccount: Boolean,
    isBackupForAndroidEnabled: Boolean,
    isAndroidSyncFeatureEnabled: Boolean,
) {
    Column(Modifier.testTag(BOTTOM_SHEET_CONTAINER)) {
        MenuActionNodeHeaderWithBody(
            modifier = Modifier.testTag(BOTTOM_SHEET_HEADER),
            title = getTitleText(device.name),
            body = getStatusText(device.status),
            nodeIcon = device.icon.iconRes,
            bodyIcon = device.status.icon,
            bodyColor = device.status.color.getStatusTextColor(),
            bodyIconColor = device.status.color.getStatusIconColor(),
            nodeIconColor = getNodeIconColor(device.icon),
        )
        MegaDivider(dividerType = DividerType.SmallStartPadding)
        // Display the Options depending on the Device type
        when (device) {
            is OwnDeviceUINode -> {
                OwnDeviceBottomSheetBody(
                    isCameraUploadsEnabled = isCameraUploadsEnabled,
                    hasSyncedFolders = device.folders.isNotEmpty(),
                    onRenameDeviceClicked = {
                        onBottomSheetDismissed()
                        onRenameDeviceClicked(device)
                    },
                    onInfoClicked = {
                        onBottomSheetDismissed()
                        onInfoClicked(device)
                    },
                    onAddNewSyncClicked = {
                        onBottomSheetDismissed()
                        onAddNewSyncClicked(device)
                    },
                    onAddBackupClicked = {
                        onBottomSheetDismissed()
                        onAddBackupClicked(device)
                    },
                    isFreeAccount = isFreeAccount,
                    isBackupForAndroidEnabled = isBackupForAndroidEnabled,
                    isAndroidSyncFeatureEnabled = isAndroidSyncFeatureEnabled
                )
            }

            is OtherDeviceUINode -> {
                OtherDeviceBottomSheetBody(
                    onRenameDeviceClicked = {
                        onBottomSheetDismissed()
                        onRenameDeviceClicked(device)
                    },
                    onInfoClicked = {
                        onBottomSheetDismissed()
                        onInfoClicked(device)
                    },
                )
            }
        }
    }
}

/**
 * Retrieves the Title Text to be displayed in [DeviceBottomSheetBody]
 *
 * @param uiNodeName The Node name
 * @return The corresponding Title Text
 */
@Composable
private fun getTitleText(uiNodeName: String) = uiNodeName.ifBlank {
    stringResource(R.string.device_center_list_view_item_title_unknown_device)
}

/**
 * Retrieves the Status Text to be displayed in [DeviceBottomSheetBody]
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
 * Retrieves the Color to be applied in the Node Icon of [DeviceBottomSheetBody]
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
@CombinedThemePreviews
@Composable
private fun DeviceBottomSheetBodyPreview(
    @PreviewParameter(DeviceBottomSheetBodyPreviewProvider::class) device: DeviceUINode,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        DeviceBottomSheetBody(
            device = device,
            isCameraUploadsEnabled = true,
            onRenameDeviceClicked = {},
            onInfoClicked = {},
            onAddNewSyncClicked = {},
            onAddBackupClicked = {},
            onBottomSheetDismissed = {},
            isFreeAccount = true,
            isBackupForAndroidEnabled = true,
            isAndroidSyncFeatureEnabled = true
        )
    }
}

/**
 * A class that provides Preview Parameters for the [DeviceBottomSheetBody]
 */
private class DeviceBottomSheetBodyPreviewProvider : PreviewParameterProvider<DeviceUINode> {
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