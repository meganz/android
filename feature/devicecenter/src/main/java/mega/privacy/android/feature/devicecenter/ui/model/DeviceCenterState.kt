package mega.privacy.android.feature.devicecenter.ui.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed

/**
 * Data class representing the state of the Device Center Screen
 *
 * @property devices The list of [DeviceCenterUINode] objects that are Devices
 * @property isCameraUploadsEnabled true if Camera Uploads is enabled, and false if otherwise
 * @property isInitialLoadingFinished true if the call to retrieve the User's Backup Information is
 * finished for the first time, and false if otherwise
 * @property selectedDevice The Device selected by the User. It is null when the User is in Device View
 * @property menuIconClickedNode The Node that was Menu-selected by the User
 * @property deviceToRename The Device to be renamed by the User
 * @property itemsToDisplay The list of [DeviceCenterUINode] objects shown in the UI. The list of
 * Devices are shown if [selectedDevice] is null. Otherwise, the list of Device Folders of
 * [selectedDevice] are shown
 * @property exitFeature State Event that will cause the User to leave Device Center if it is triggered
 * @property renameDeviceSuccess State Event which notifies that renaming the Device is successful
 */
data class DeviceCenterState(
    val devices: List<DeviceCenterUINode> = emptyList(),
    val isCameraUploadsEnabled: Boolean = false,
    val isInitialLoadingFinished: Boolean = false,
    val selectedDevice: DeviceUINode? = null,
    val menuIconClickedNode: DeviceCenterUINode? = null,
    val deviceToRename: DeviceUINode? = null,
    val exitFeature: StateEvent = consumed,
    val renameDeviceSuccess: StateEvent = consumed,
) {
    val itemsToDisplay: List<DeviceCenterUINode> = selectedDevice?.folders ?: devices
}