package mega.privacy.android.feature.devicecenter.data.mapper

import mega.privacy.android.feature.devicecenter.domain.entity.DeviceFolderNode
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceStatus
import javax.inject.Inject

/**
 * Mapper that returns the appropriate [DeviceStatus] for a Device
 *
 * The order for detecting the appropriate Device Status is:
 *
 * 1. Nothing set up (only applicable for the User's Current Device)
 * 2. Inactive
 * 3. Attention needed
 * 4. Updating
 * 5. Up to date
 * 6. Unknown (if no matching Device Status)
 */
internal class DeviceNodeStatusMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param folders the list of [DeviceFolderNode] objects of a Device
     * @param isCurrentDevice true if the Device is the User's Current Device, and false if otherwise
     *
     * @return the appropriate [DeviceStatus] for the Device
     */
    operator fun invoke(
        folders: List<DeviceFolderNode>,
        isCurrentDevice: Boolean,
    ) =
        if (isCurrentDevice && folders.isEmpty()) {
            DeviceStatus.NothingSetUp
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
     * @return The appropriate [DeviceStatus]
     */
    private fun List<DeviceFolderNode>.calculateDeviceStatus(): DeviceStatus =
        when (this.maxOfOrNull { folder -> folder.status.priority }) {
            5 -> DeviceStatus.Inactive
            3, 4 -> DeviceStatus.AttentionNeeded
            2 -> DeviceStatus.Updating
            1 -> DeviceStatus.UpToDate
            else -> DeviceStatus.Unknown
        }
}