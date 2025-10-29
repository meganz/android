package mega.privacy.android.feature.devicecenter.ui.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.privacy.android.feature.devicecenter.R
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterUiState
import mega.privacy.android.feature.devicecenter.ui.model.DeviceMenuAction
import mega.privacy.android.feature.devicecenter.ui.model.DeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.OwnDeviceUINode

/**
 * Material 3 App Bar for Device Center
 *
 * @param uiState The UI State
 * @param selectedDevice The currently selected device, if any
 * @param isSearchMode Whether search mode is active
 * @param onBackPressed Lambda for back button press
 * @param onActionPressed Lambda for action menu item clicks
 * @param onSearchQueryChanged Lambda for search query changes
 * @param onSearchModeChanged Lambda for search mode state changes
 */
@Composable
internal fun DeviceCenterAppBarM3(
    uiState: DeviceCenterUiState,
    selectedDevice: DeviceUINode?,
    isSearchMode: Boolean,
    onBackPressed: () -> Unit,
    onActionPressed: (MenuAction) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSearchModeChanged: (Boolean) -> Unit,
) {
    // When info screen is open, use simple AppBar without title or search
    if (uiState.infoSelectedItem != null) {
        MegaTopAppBar(
            navigationType = AppBarNavigationType.Back(
                onNavigationIconClicked = onBackPressed
            ),
            title = "",
        )
    } else {
        // Otherwise, use search-enabled AppBar
        MegaSearchTopAppBar(
            navigationType = AppBarNavigationType.Back(
                onNavigationIconClicked = onBackPressed
            ),
            title = selectedDevice?.name
                ?: stringResource(R.string.device_center_top_app_bar_title),
            query = uiState.searchQuery,
            onQueryChanged = onSearchQueryChanged,
            isSearchingMode = isSearchMode,
            onSearchingModeChanged = onSearchModeChanged,
            searchPlaceholder = if (uiState.itemsToDisplay.any { it is DeviceUINode }) {
                stringResource(R.string.device_center_top_app_bar_search_devices_hint)
            } else {
                stringResource(R.string.device_center_top_app_bar_search_syncs_hint)
            },
            actions = remember(selectedDevice, uiState.isCameraUploadsEnabled, onActionPressed) {
                buildActionsList(
                    selectedDevice,
                    uiState.isCameraUploadsEnabled,
                    onActionPressed
                ) ?: emptyList()
            },
        )
    }
}

/**
 * Builds the list of action menu items based on the selected device type
 *
 * @param selectedDevice The currently selected device
 * @param isCameraUploadsEnabled Whether camera uploads are enabled
 * @param onActionPressed Lambda for action menu item clicks
 * @return List of menu actions or null if no device is selected
 */
private fun buildActionsList(
    selectedDevice: DeviceUINode?,
    isCameraUploadsEnabled: Boolean,
    onActionPressed: (MenuAction) -> Unit,
): List<MenuActionWithClick>? {
    return selectedDevice?.let {
        buildList {
            add(
                MenuActionWithClick(
                    DeviceMenuAction.Rename
                ) { onActionPressed(DeviceMenuAction.Rename) }
            )

            when (selectedDevice) {
                is OwnDeviceUINode -> {
                    if (isCameraUploadsEnabled) {
                        add(MenuActionWithClick(DeviceMenuAction.Info) {
                            onActionPressed(DeviceMenuAction.Info)
                        })
                    }
                    add(MenuActionWithClick(DeviceMenuAction.CameraUploads) {
                        onActionPressed(DeviceMenuAction.CameraUploads)
                    })
                }

                else -> add(MenuActionWithClick(DeviceMenuAction.Info) {
                    onActionPressed(DeviceMenuAction.Info)
                })
            }
        }
    }
}
