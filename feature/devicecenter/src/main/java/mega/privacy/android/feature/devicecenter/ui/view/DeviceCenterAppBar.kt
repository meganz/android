package mega.privacy.android.feature.devicecenter.ui.view

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.feature.devicecenter.R
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterUiState
import mega.privacy.android.feature.devicecenter.ui.model.DeviceMenuAction
import mega.privacy.android.feature.devicecenter.ui.model.DeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.OwnDeviceUINode
import mega.privacy.android.legacy.core.ui.controls.appbar.LegacySearchAppBar
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar

@Composable
internal fun DeviceCenterAppBar(
    uiState: DeviceCenterUiState,
    selectedDevice: DeviceUINode?,
    onNavigateUp: () -> Unit,
    onActionPressed: ((MenuAction) -> Unit)?,
    onSearchQueryChanged: (query: String) -> Unit,
    onSearchCloseClicked: () -> Unit,
    onSearchClicked: () -> Unit,
) {
    if (!uiState.isInitialLoadingFinished || !uiState.isNetworkConnected) {
        MegaAppBar(
            modifier = Modifier.Companion.testTag(DEVICE_CENTER_TOOLBAR),
            appBarType = AppBarType.BACK_NAVIGATION,
            title = selectedDevice?.name
                ?: stringResource(R.string.device_center_top_app_bar_title),
            elevation = 0.dp,
            onNavigationPressed = onNavigateUp,
            onActionPressed = onActionPressed,
            windowInsets = WindowInsets(0.dp),
        )
    } else {
        LegacySearchAppBar(
            searchWidgetState = uiState.searchWidgetState,
            typedSearch = uiState.searchQuery,
            onSearchTextChange = { onSearchQueryChanged(it) },
            onCloseClicked = {
                onSearchCloseClicked()
            },
            onBackPressed = onNavigateUp,
            onSearchClicked = { onSearchClicked() },
            elevation = false,
            title = selectedDevice?.name
                ?: stringResource(R.string.device_center_top_app_bar_title),
            hintId = if (uiState.itemsToDisplay.any { it is DeviceUINode }) {
                R.string.device_center_top_app_bar_search_devices_hint
            } else {
                R.string.device_center_top_app_bar_search_syncs_hint
            },
            onActionPressed = onActionPressed,
            actions = selectedDevice?.let {
                val list = mutableListOf<MenuAction>(DeviceMenuAction.Rename)

                when (uiState.selectedDevice) {
                    is OwnDeviceUINode -> {
                        if (uiState.isCameraUploadsEnabled) {
                            list.add(DeviceMenuAction.Info)
                        }
                        list.add(DeviceMenuAction.CameraUploads)
                    }

                    else -> list.add(DeviceMenuAction.Info)
                }

                return@let list
            },
            isHideAfterSearch = true,
            windowInsets = WindowInsets(0.dp),
            modifier = Modifier.Companion.testTag(DEVICE_CENTER_TOOLBAR),
        )
    }
}

internal const val DEVICE_CENTER_TOOLBAR = "device_center_screen:mega_app_bar"
