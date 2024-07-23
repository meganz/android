package mega.privacy.android.feature.devicecenter.data.mapper

import mega.privacy.android.domain.entity.backup.BackupInfoType
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
 * 8. Error
 * 9. Stalled
 * 10. Up to Date
 * 11. Offline
 * 12. Disabled
 * 13. Stopped
 * 14. Unknown (if no matching Device Status)
 */
internal class DeviceNodeStatusMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param folders the list of [DeviceFolderNode] objects of a Device
     * @param isCurrentDevice true if the Device is the User's Current Device, and false if otherwise
     *
     * @return the appropriate [DeviceCenterNodeStatus] for the Device
     */
    operator fun invoke(
        folders: List<DeviceFolderNode>,
        isCurrentDevice: Boolean,
    ) =
        if (isCurrentDevice && folders.isEmpty()) {
            DeviceCenterNodeStatus.NothingSetUp
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
        when (this.filter { it.type != BackupInfoType.CAMERA_UPLOADS && it.type != BackupInfoType.MEDIA_UPLOADS }
            .maxOfOrNull { folder -> folder.status.priority }) {
            // Syncing Devices do not need to display the syncing progress in the UI
            12 -> DeviceCenterNodeStatus.Syncing(progress = 0)
            11 -> DeviceCenterNodeStatus.Scanning
            10 -> DeviceCenterNodeStatus.Initializing
            9 -> DeviceCenterNodeStatus.Paused
            // Blocked, Overquota and Error Devices do not need to display the error sub state in the UI
            8 -> DeviceCenterNodeStatus.Overquota(errorSubState = null)
            7 -> DeviceCenterNodeStatus.Blocked(errorSubState = null)
            6 -> DeviceCenterNodeStatus.Error(errorSubState = null)
            5 -> DeviceCenterNodeStatus.Stalled
            4 -> DeviceCenterNodeStatus.UpToDate
            3 -> DeviceCenterNodeStatus.Offline
            2 -> DeviceCenterNodeStatus.Disabled
            1 -> DeviceCenterNodeStatus.Stopped
            else -> DeviceCenterNodeStatus.Unknown
        }
}