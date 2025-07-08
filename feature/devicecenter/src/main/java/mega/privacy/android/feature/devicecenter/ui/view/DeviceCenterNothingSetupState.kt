package mega.privacy.android.feature.devicecenter.ui.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import mega.privacy.android.icon.pack.R
import mega.privacy.android.shared.sync.ui.SyncEmptyState

/**
 * A [androidx.compose.runtime.Composable] which displays a Nothing setup state
 */
@Composable
internal fun DeviceCenterNothingSetupState() {
    SyncEmptyState(
        iconId = R.drawable.ic_folder_sync,
        iconSize = 128.dp,
        iconDescription = "No setup state",
        textId = mega.privacy.android.feature.devicecenter.R.string.device_center_nothing_setup_state,
        testTag = DEVICE_CENTER_NOTHING_SETUP_STATE
    )
}

internal const val DEVICE_CENTER_NOTHING_SETUP_STATE = "device_center_content:nothing_setup_state"