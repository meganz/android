package mega.privacy.android.feature.devicecenter.ui.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import mega.privacy.android.icon.pack.R
import mega.privacy.android.shared.sync.ui.SyncEmptyState

/**
 * A [androidx.compose.runtime.Composable] which displays a No network connectivity state
 */
@Composable
internal fun DeviceCenterNoNetworkState() {
    SyncEmptyState(
        iconId = R.drawable.ic_no_cloud,
        iconSize = 144.dp,
        iconDescription = "No network connectivity state",
        textId = mega.privacy.android.feature.devicecenter.R.string.device_center_no_network_state,
        testTag = DEVICE_CENTER_NO_NETWORK_STATE
    )
}

internal const val DEVICE_CENTER_NO_NETWORK_STATE = "device_center_content:no_network_state"