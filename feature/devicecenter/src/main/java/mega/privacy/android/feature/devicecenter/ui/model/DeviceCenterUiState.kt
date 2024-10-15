package mega.privacy.android.feature.devicecenter.ui.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState

/**
 * Data class representing the state of the Device Center Screen
 *
 * @property devices The list of [DeviceCenterUINode] objects that are Devices
 * @property isCameraUploadsEnabled true if Camera Uploads is enabled, and false if otherwise
 * @property isInitialLoadingFinished true if the call to retrieve the User's Backup Information is
 * finished for the first time, and false if otherwise
 * @property selectedDevice The Device selected by the User. It is null when the User is in Device View
 * @property menuClickedDevice The Device whose Context Menu is selected
 * @property deviceToRename The Device to be renamed
 * @property itemsToDisplay The list of [DeviceCenterUINode] objects shown in the UI. The list of
 * Devices are shown if [selectedDevice] is null. Otherwise, the list of Device Folders of
 * [selectedDevice] are shown
 * @property exitFeature State Event that will cause the User to leave Device Center if it is triggered
 * @property renameDeviceSuccess State Event which notifies that renaming the Device is successful
 * @property isNetworkConnected True if has network connectivity or False otherwise
 * @property searchQuery The search query
 * @property filteredUiItems The list of [DeviceCenterUINode] objects that are filtered based on the search query
 * @property searchWidgetState The state of the search widget
 * @property infoSelectedItem The item selected to show its Info
 * @property isFreeAccount True if is a Free account or False otherwise
 * @property enabledFlags Enabled flags
 * @property isSyncFeatureEnabled True if the Sync feature is enabled or False otherwise
 */
data class DeviceCenterUiState(
    val devices: List<DeviceCenterUINode> = emptyList(),
    val isCameraUploadsEnabled: Boolean = false,
    val isInitialLoadingFinished: Boolean = false,
    val selectedDevice: DeviceUINode? = null,
    val menuClickedDevice: DeviceUINode? = null,
    val deviceToRename: DeviceUINode? = null,
    val exitFeature: StateEvent = consumed,
    val renameDeviceSuccess: StateEvent = consumed,
    val isNetworkConnected: Boolean = false,
    val searchQuery: String = "",
    val filteredUiItems: List<DeviceCenterUINode>? = null,
    val searchWidgetState: SearchWidgetState = SearchWidgetState.COLLAPSED,
    val infoSelectedItem: DeviceCenterUINode? = null,
    val isFreeAccount: Boolean = true,
    val isSyncFeatureEnabled: Boolean = false,
    val enabledFlags: Set<Feature> = emptySet(),
) {
    val itemsToDisplay: List<DeviceCenterUINode> = selectedDevice?.folders ?: devices
}