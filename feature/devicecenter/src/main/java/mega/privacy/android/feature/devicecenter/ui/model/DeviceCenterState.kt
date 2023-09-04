package mega.privacy.android.feature.devicecenter.ui.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed

/**
 * Data class representing the state of the Device Center Screen
 *
 * @property devices The list of [DeviceCenterUINode] objects that are Devices
 * @property selectedDevice The Device selected by the User. It is null when the User is in Device View
 * @property itemsToDisplay The list of [DeviceCenterUINode] objects shown in the UI. The list of
 * Devices are shown if [selectedDevice] is null. Otherwise, the list of Device Folders of
 * [selectedDevice] are shown
 * @property exitFeature State Event that will cause the User to leave Device Center if it is triggered
 */
data class DeviceCenterState(
    val devices: List<DeviceCenterUINode> = emptyList(),
    val selectedDevice: DeviceUINode? = null,
    val exitFeature: StateEvent = consumed,
) {
    val itemsToDisplay: List<DeviceCenterUINode> = selectedDevice?.folders ?: devices
}