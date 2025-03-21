package mega.privacy.android.feature.devicecenter.ui.lists

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.feature.devicecenter.ui.model.OwnDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceIconType
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import mega.privacy.android.shared.resources.R
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * A Preview Composable that displays all possible Statuses
 *
 * @param status The [DeviceCenterUINodeStatus] generated by the [DeviceCenterUINodeStatusProvider]
 */
@CombinedThemePreviews
@Composable
private fun PreviewDeviceCenterUINodeStatus(
    @PreviewParameter(DeviceCenterUINodeStatusProvider::class) status: DeviceCenterUINodeStatus,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterListViewItem(
            uiNode = OwnDeviceUINode(
                id = "1234-5678",
                name = "Device Name",
                icon = DeviceIconType.Android,
                status = status,
                folders = emptyList(),
            ),
        )
    }
}

/**
 * A [PreviewParameterProvider] class that provides the list of Statuses to be displayed in the
 * Composable preview
 */
private class DeviceCenterUINodeStatusProvider :
    PreviewParameterProvider<DeviceCenterUINodeStatus> {
    override val values = listOf(
        DeviceCenterUINodeStatus.Unknown,
        DeviceCenterUINodeStatus.UpToDate,
        DeviceCenterUINodeStatus.Initializing,
        DeviceCenterUINodeStatus.Scanning,
        DeviceCenterUINodeStatus.Syncing,
        DeviceCenterUINodeStatus.SyncingWithPercentage(50),
        DeviceCenterUINodeStatus.NothingSetUp,
        DeviceCenterUINodeStatus.Disabled,
        DeviceCenterUINodeStatus.Offline,
        DeviceCenterUINodeStatus.Paused,
        DeviceCenterUINodeStatus.Stopped,
        DeviceCenterUINodeStatus.Overquota(specificErrorMessage = R.string.general_sync_storage_overquota),
        DeviceCenterUINodeStatus.Error(specificErrorMessage = R.string.general_sync_put_nodes_error),
        DeviceCenterUINodeStatus.Blocked(specificErrorMessage = R.string.general_sync_account_blocked),
    ).asSequence()
}