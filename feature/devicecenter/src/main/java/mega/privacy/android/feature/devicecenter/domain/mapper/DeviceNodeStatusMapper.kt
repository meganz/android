package mega.privacy.android.feature.devicecenter.domain.mapper

import mega.privacy.android.feature.devicecenter.domain.entity.DeviceCenterNodeStatus
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceFolderNode
import javax.inject.Inject

/**
 * Given a list of [DeviceFolderNode] objects, this Mapper that returns a specific Device Status
 * based on a given priority number
 */
class DeviceNodeStatusMapper @Inject constructor() {
    /**
     * Invocation function
     *
     * The order for detecting the appropriate Device Status is:
     * 1. Syncing
     * 2. Scanning
     * 3. Initializing
     * 4. Paused
     * 5. Overquota
     * 6. Blocked
     * 7. Up to Date
     * 8. Offline
     * 9. Disabled
     * 10. Stopped
     *
     * If there is no matching Device Status, [DeviceCenterNodeStatus.Unknown] is returned
     *
     * @param folders the list of [DeviceFolderNode] objects of a Device
     * @return the appropriate [DeviceCenterNodeStatus] for the Device
     */
    operator fun invoke(folders: List<DeviceFolderNode>) =
        when (folders.maxOfOrNull { it.status.priority }) {
            // Devices do not need to display the syncing progress in the UI
            10 -> DeviceCenterNodeStatus.Syncing(0)
            9 -> DeviceCenterNodeStatus.Scanning
            8 -> DeviceCenterNodeStatus.Initializing
            7 -> DeviceCenterNodeStatus.Paused
            6 -> DeviceCenterNodeStatus.Overquota
            // Devices do not need to display the error sub state in the UI
            5 -> DeviceCenterNodeStatus.Blocked(null)
            4 -> DeviceCenterNodeStatus.UpToDate
            3 -> DeviceCenterNodeStatus.Offline
            2 -> DeviceCenterNodeStatus.Disabled
            1 -> DeviceCenterNodeStatus.Stopped
            else -> DeviceCenterNodeStatus.Unknown
        }
}