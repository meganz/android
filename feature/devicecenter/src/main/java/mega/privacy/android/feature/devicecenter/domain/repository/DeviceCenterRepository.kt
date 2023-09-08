package mega.privacy.android.feature.devicecenter.domain.repository

import mega.privacy.android.domain.entity.backup.BackupInfo
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceNode

/**
 * Repository class that provides several functions specific to Device Center
 */
interface DeviceCenterRepository {

    /**
     * Retrieves all of the User's Backup Devices
     *
     * @param backupInfoList A list of [BackupInfo] objects
     * @param currentDeviceId The Device ID of the Current Device being used
     * @param deviceIdAndNameMap A [Map] whose Key-Value Pair consists of the Device ID and Device Name
     * @param isCameraUploadsEnabled true if Camera Uploads is enabled, and false if otherwise
     *
     * @return A list of [DeviceNode] objects
     */
    suspend fun getDevices(
        backupInfoList: List<BackupInfo>,
        currentDeviceId: String,
        deviceIdAndNameMap: Map<String, String>,
        isCameraUploadsEnabled: Boolean,
    ): List<DeviceNode>
}
