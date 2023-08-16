package mega.privacy.android.feature.devicecenter.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.privacy.android.core.ui.controls.lists.NodeListViewItem
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.feature.devicecenter.R
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
 * @param icon The entry Icon
 * @param name The entry Name
 * @param status The [DeviceCenterUINodeStatus]
 * @param onMenuClick Lambda that performs a specific action when the Menu icon is clicked
 */
@Composable
internal fun DeviceCenterListViewItem(
    @DrawableRes icon: Int,
    name: String,
    status: DeviceCenterUINodeStatus,
    onMenuClick: () -> Unit,
) {
    NodeListViewItem(
        modifier = Modifier.testTag(DEVICE_CENTER_LIST_VIEW_ITEM_TAG),
        isSelected = false,
        folderInfo = getStatusText(status),
        icon = icon,
        fileSize = null,
        modifiedDate = null,
        name = name,
        infoColor = getStatusColor(status),
        infoIcon = status.icon,
        infoIconTint = getStatusColor(status),
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
    if (uiNodeStatus is DeviceCenterUINodeStatus.UpdatingWithPercentage) {
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

@CombinedThemePreviews
@Composable
private fun EntryWithDifferentStatusPreview(
    @PreviewParameter(DeviceCenterUINodeStatusProvider::class) status: DeviceCenterUINodeStatus,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterListViewItem(
            icon = R.drawable.ic_device_android,
            name = "Android Device",
            status = status,
            onMenuClick = {},
        )
    }
}

/**
 * Provider class that enumerates the list of [DeviceCenterUINodeStatus] objects to be shown in
 * the Preview
 */
private class DeviceCenterUINodeStatusProvider :
    PreviewParameterProvider<DeviceCenterUINodeStatus> {
    override val values = listOf(
        DeviceCenterUINodeStatus.UpToDate,
        DeviceCenterUINodeStatus.Initialising,
        DeviceCenterUINodeStatus.Scanning,
        DeviceCenterUINodeStatus.Updating,
        DeviceCenterUINodeStatus.UpdatingWithPercentage(50),
        DeviceCenterUINodeStatus.NoCameraUploads,
        DeviceCenterUINodeStatus.Disabled,
        DeviceCenterUINodeStatus.Offline,
        DeviceCenterUINodeStatus.Paused,
        DeviceCenterUINodeStatus.BackupStopped,
        DeviceCenterUINodeStatus.OutOfQuota,
        DeviceCenterUINodeStatus.Error,
        DeviceCenterUINodeStatus.Blocked,
    ).asSequence()
}
