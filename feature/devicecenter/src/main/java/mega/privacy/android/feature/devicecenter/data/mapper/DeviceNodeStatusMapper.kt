package mega.privacy.android.feature.devicecenter.data.mapper

import mega.privacy.android.feature.devicecenter.domain.entity.DeviceCenterNodeStatus
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceFolderNode
import javax.inject.Inject

/**
 * Mapper that returns the appropriate [DeviceCenterNodeStatus] for a Backup Device
 *
 * The order for detecting the appropriate Device Status is:
 *
 * 1. No Camera uploads (only applicable for the User's Current Device)
 * 2. Syncing
 * 3. Scanning
 * 4. Initializing
 * 5. Paused
 * 6. Overquota
 * 7. Blocked
 * 8. Up to Date
 * 9. Offline
 * 10. Disabled
 * 11. Stopped
 * 12. Unknown (if no matching Device Status)
 */
internal class DeviceNodeStatusMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param folders the list of [DeviceFolderNode] objects of a Device
     * @param isCameraUploadsEnabled true if Camera Uploads is enabled on the User's Current Device,
     * and false if otherwise
     * @param isCurrentDevice true if the Device is the User's Current Device, and false if otherwise
     *
     * @return the appropriate [DeviceCenterNodeStatus] for the Device
     */
    operator fun invoke(
        folders: List<DeviceFolderNode>,
        isCameraUploadsEnabled: Boolean,
        isCurrentDevice: Boolean,
    ) = if (isCurrentDevice && isCameraUploadsEnabled.not()) {
        DeviceCenterNodeStatus.NoCameraUploads
    } else {
        folders.calculateDeviceStatus()
    }

    /**
     * Retrieves the Device Status based on the highest priority found from the Device's Backup
     * Folders
     *
     * This is called on either condition:
     *
     * 1. When the Device is a Current Device and Camera Uploads is enabled, or
     * 2. When the Device is an Other Device
     *
     * @return The appropriate [DeviceCenterNodeStatus]
     */
    private fun List<DeviceFolderNode>.calculateDeviceStatus(): DeviceCenterNodeStatus =
        when (this.maxOfOrNull { folder -> folder.status.priority }) {
            // Syncing Devices do not need to display the syncing progress in the UI
            10 -> DeviceCenterNodeStatus.Syncing(0)
            9 -> DeviceCenterNodeStatus.Scanning
            8 -> DeviceCenterNodeStatus.Initializing
            7 -> DeviceCenterNodeStatus.Paused
            // Blocked and Overquota Devices do not need to display the error sub state in the UI
            6 -> DeviceCenterNodeStatus.Overquota(null)
            5 -> DeviceCenterNodeStatus.Blocked(null)
            4 -> DeviceCenterNodeStatus.UpToDate
            3 -> DeviceCenterNodeStatus.Offline
            2 -> DeviceCenterNodeStatus.Disabled
            1 -> DeviceCenterNodeStatus.Stopped
            else -> DeviceCenterNodeStatus.Unknown
        }
}