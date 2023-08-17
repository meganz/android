package mega.privacy.android.feature.devicecenter.ui.lists

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.core.ui.controls.lists.NodeListViewItem
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterUINode
import mega.privacy.android.feature.devicecenter.ui.model.OwnDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceIconType
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import java.io.File

/**
 * Test tag for the Device Center List View Item
 */
internal const val DEVICE_CENTER_LIST_VIEW_ITEM_TAG =
    "device_center_list_view_item:node_list_view_item"

/**
 * A Composable Class extending [NodeListViewItem] that represents a Device / Device Folder entry
 * in the Device Center
 *
 * @param uiNode The [DeviceCenterUINode] to be displayed
 * @param onMenuClick Lambda that performs a specific action when the Menu icon is clicked
 */
@Composable
internal fun DeviceCenterListViewItem(
    uiNode: DeviceCenterUINode,
    onMenuClick: () -> Unit,
) {
    NodeListViewItem(
        modifier = Modifier.testTag(DEVICE_CENTER_LIST_VIEW_ITEM_TAG),
        isSelected = false,
        folderInfo = getStatusText(uiNode.status),
        icon = uiNode.icon.iconRes,
        applySecondaryColorIconTint = uiNode.icon.applySecondaryColorTint,
        fileSize = null,
        modifiedDate = null,
        name = uiNode.name,
        infoColor = getStatusColor(uiNode.status),
        infoIcon = uiNode.status.icon,
        infoIconTint = getStatusColor(uiNode.status),
        showMenuButton = true,
        isTakenDown = false,
        isFavourite = false,
        isSharedWithPublicLink = false,
        imageState = remember { mutableStateOf(null as File?) },
        onClick = onMenuClick,
    )
}

/**
 * Retrieves the Status Text to be displayed in the Body Section of [NodeListViewItem]
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
 * Retrieves the Status Color to be applied in the Body Section of [NodeListViewItem]
 *
 * @param uiNodeStatus The [DeviceCenterUINodeStatus]
 * @return The corresponding Status Color
 */
@Composable
private fun getStatusColor(uiNodeStatus: DeviceCenterUINodeStatus) =
    uiNodeStatus.color ?: MaterialTheme.colors.textColorSecondary

/**
 * A Preview Composable that displays [DeviceCenterListViewItem]
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterListViewItemPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterListViewItem(
            uiNode = OwnDeviceUINode(
                id = "1234-5678",
                name = "Backup Name",
                icon = DeviceIconType.Android,
                status = DeviceCenterUINodeStatus.UpToDate,
                folders = emptyList(),
            ),
            onMenuClick = {},
        )
    }
}
