package mega.privacy.android.feature.devicecenter.ui.lists

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.feature.devicecenter.R
import mega.privacy.android.feature.devicecenter.ui.model.BackupDeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterUINode
import mega.privacy.android.feature.devicecenter.ui.model.DeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.NonBackupDeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.OwnDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceIconType
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import mega.privacy.android.legacy.core.ui.controls.divider.CustomDivider
import mega.privacy.android.legacy.core.ui.controls.lists.NodeListViewItem
import mega.privacy.android.shared.theme.MegaAppTheme
import java.io.File

/**
 * Test tag for the Device Center List View Item
 */
internal const val DEVICE_CENTER_LIST_VIEW_ITEM_TAG =
    "device_center_list_view_item:node_list_view_item"

internal const val DEVICE_CENTER_LIST_VIEW_ITEM_DIVIDER_TAG =
    "device_center_list_view_item:custom_divider"

/**
 * A Composable Class extending [NodeListViewItem] that represents a Device / Device Folder entry
 * in the Device Center. Each entry also shows a [CustomDivider]
 *
 * @param uiNode The [DeviceCenterUINode] to be displayed
 * @param onDeviceClicked Lambda that performs a specific action when a Device is clicked
 * @param onDeviceMenuClicked Lambda that performs a specific action when a Device's Menu Icon is
 * clicked
 * @param onBackupFolderClicked Lambda that performs a specific action when a Backup Folder is clicked
 * @param onBackupFolderMenuClicked Lambda that performs a specific action when a Backup Folder's
 * Menu Icon is clicked
 * @param onNonBackupFolderMenuClicked Lambda that performs a specific action when a Non Backup Folder's
 * Menu icon is clicked
 */
@Composable
internal fun DeviceCenterListViewItem(
    uiNode: DeviceCenterUINode,
    onDeviceClicked: (DeviceUINode) -> Unit = {},
    onDeviceMenuClicked: (DeviceUINode) -> Unit = {},
    onBackupFolderClicked: (BackupDeviceFolderUINode) -> Unit = {},
    onBackupFolderMenuClicked: (BackupDeviceFolderUINode) -> Unit = {},
    onNonBackupFolderMenuClicked: (NonBackupDeviceFolderUINode) -> Unit = {},
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        val (nodeListViewItem, divider) = createRefs()

        NodeListViewItem(
            modifier = Modifier
                .testTag(DEVICE_CENTER_LIST_VIEW_ITEM_TAG)
                .constrainAs(nodeListViewItem) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            isSelected = false,
            folderInfo = getStatusText(uiNode.status),
            icon = uiNode.icon.iconRes,
            applySecondaryColorIconTint = uiNode.icon.applySecondaryColorTint,
            fileSize = null,
            modifiedDate = null,
            name = uiNode.name.ifBlank { stringResource(R.string.device_center_list_view_item_title_unknown_device) },
            infoColor = getStatusColor(uiNode.status),
            infoIcon = uiNode.status.icon,
            infoIconTint = getStatusColor(uiNode.status),
            showMenuButton = true,
            isTakenDown = false,
            isFavourite = false,
            isSharedWithPublicLink = false,
            imageState = remember { mutableStateOf(null as File?) },
            onClick = {
                when (uiNode) {
                    is DeviceUINode -> onDeviceClicked(uiNode)
                    is BackupDeviceFolderUINode -> onBackupFolderClicked(uiNode)
                }
            },
            onMenuClick = {
                when (uiNode) {
                    is DeviceUINode -> onDeviceMenuClicked(uiNode)
                    is BackupDeviceFolderUINode -> onBackupFolderMenuClicked(uiNode)
                    is NonBackupDeviceFolderUINode -> onNonBackupFolderMenuClicked(uiNode)
                }
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
private fun PreviewDeviceCenterListViewItem() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterListViewItem(
            uiNode = OwnDeviceUINode(
                id = "1234-5678",
                name = "Backup Name",
                icon = DeviceIconType.Android,
                status = DeviceCenterUINodeStatus.UpToDate,
                folders = emptyList(),
            ),
        )
    }
}

/**
 * A Preview Composable that displays [DeviceCenterListViewItem] with a default Title if the Device
 * has no name
 */
@CombinedThemePreviews
@Composable
private fun PreviewDeviceCenterListViewItemWithEmptyTitle() {
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
