package mega.privacy.android.feature.devicecenter.ui.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import mega.privacy.android.core.ui.controls.lists.StatusListViewItem
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.feature.devicecenter.R
import mega.privacy.android.feature.devicecenter.ui.model.BackupDeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterUINode
import mega.privacy.android.feature.devicecenter.ui.model.DeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.NonBackupDeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.OwnDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceCenterUINodeIcon
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceIconType
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import mega.privacy.android.legacy.core.ui.controls.divider.CustomDivider
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Test tag for the Device Center List View Item
 */
internal const val DEVICE_CENTER_LIST_VIEW_ITEM_TAG =
    "device_center_list_view_item:node_list_view_item"

internal const val DEVICE_CENTER_LIST_VIEW_ITEM_DIVIDER_TAG =
    "device_center_list_view_item:custom_divider"

/**
 * A Composable Class that represents a Device / Folder entry in the Device Center.
 * Each entry also shows a [CustomDivider]
 *
 * @param uiNode The [DeviceCenterUINode] to be displayed
 * @param onDeviceClicked Lambda that performs a specific action when a Device is clicked
 * @param onDeviceMenuClicked Lambda that performs a specific action when a Device's Menu Icon is
 * clicked
 * @param onBackupFolderClicked Lambda that performs a specific action when a Backup Folder is clicked
 * @param onNonBackupFolderClicked Lambda that performs a specific action when a Non Backup Folder
 * is clicked
 */
@Composable
internal fun DeviceCenterListViewItem(
    uiNode: DeviceCenterUINode,
    onDeviceClicked: (DeviceUINode) -> Unit = {},
    onDeviceMenuClicked: (DeviceUINode) -> Unit = {},
    onBackupFolderClicked: (BackupDeviceFolderUINode) -> Unit = {},
    onNonBackupFolderClicked: (NonBackupDeviceFolderUINode) -> Unit = {},
    onInfoClicked: (DeviceCenterUINode) -> Unit = {},
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        val (item, divider) = createRefs()

        StatusListViewItem(
            icon = uiNode.icon.iconRes,
            name = uiNode.name.ifBlank {
                when (uiNode) {
                    is DeviceUINode -> stringResource(R.string.device_center_list_view_item_title_unknown_device)
                    is NonBackupDeviceFolderUINode -> uiNode.localFolderPath
                    else -> ""
                }
            },
            statusText = getStatusText(uiNode.status),
            modifier = Modifier
                .testTag(DEVICE_CENTER_LIST_VIEW_ITEM_TAG)
                .constrainAs(item) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .clickable {
                    when (uiNode) {
                        is DeviceUINode -> onDeviceClicked(uiNode)
                        is BackupDeviceFolderUINode -> onBackupFolderClicked(uiNode)
                        is NonBackupDeviceFolderUINode -> onNonBackupFolderClicked(uiNode)
                    }
                },
            applySecondaryColorIconTint = uiNode.icon.applySecondaryColorTint,
            statusIcon = uiNode.status.icon,
            statusColor = uiNode.status.color,
            onMoreClicked = if (uiNode is DeviceUINode) { ->
                onDeviceMenuClicked(uiNode)
            } else {
                null
            },
            onInfoClicked = if (uiNode is DeviceUINode) {
                null
            } else { ->
                onInfoClicked(uiNode)
            },
        )
        CustomDivider(
            modifier = Modifier
                .testTag(DEVICE_CENTER_LIST_VIEW_ITEM_DIVIDER_TAG)
                .constrainAs(divider) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                },
            withStartPadding = true,
        )
    }
}

/**
 * Retrieves the Status Text to be displayed in the Body Section of [DeviceCenterListViewItem]
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
 * A Preview Composable that displays [DeviceCenterListViewItem] for a Device
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterListViewItemDevicePreview(
    @PreviewParameter(DeviceCenterUINodeDeviceIconProvider::class) icon: DeviceCenterUINodeIcon,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterListViewItem(
            uiNode = OwnDeviceUINode(
                id = "1234-5678",
                name = "Device Name",
                icon = icon,
                status = DeviceCenterUINodeStatus.UpToDate,
                folders = emptyList(),
            ),
        )
    }
}

/**
 * A Preview Composable that displays [DeviceCenterListViewItem] for a Folder Connection
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterListViewItemDeviceFolderPreview(
    @PreviewParameter(DeviceCenterUINodeFolderIconProvider::class) icon: DeviceCenterUINodeIcon,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterListViewItem(
            uiNode = BackupDeviceFolderUINode(
                id = "1234-5678",
                name = "Connection Folder Name",
                icon = icon,
                status = DeviceCenterUINodeStatus.UpToDate,
                rootHandle = 1234L,
            ),
        )
    }
}


/**
 * A Preview Composable that displays [DeviceCenterListViewItem] for a Device
 * with a default Title if the Device has no name
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterListViewItemDeviceWithEmptyTitlePreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterListViewItem(
            uiNode = OwnDeviceUINode(
                id = "1234-5678",
                name = "",
                icon = DeviceIconType.Android,
                status = DeviceCenterUINodeStatus.UpToDate,
                folders = emptyList(),
            ),
        )
    }
}
