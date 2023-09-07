package mega.privacy.android.feature.devicecenter.domain.repository

import mega.privacy.android.feature.devicecenter.domain.entity.BackupInfo
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceNode

/**
 * Repository class that provides several functions for Device Center Use Cases
 */
interface DeviceCenterRepository {

    /**
     * Retrieves all of the User's Backup information
     *
     * @return A list of [BackupInfo] objects
     */
    suspend fun getBackupInfo(): List<BackupInfo>

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

    /**
     * Retrieves the Device ID of the current Device
     *
     * @return the Device ID
     */
    suspend fun getDeviceId(): String?

    /**
     * Retrieves User information on the list of backed up Devices, represented as a [Map].
     * Each [Map] entry represents a Key-Value Pair of a Device ID and Device Name, respectively
     *
     * @return A [Map] whose Key-Value Pair consists of the Device ID and Device Name
     */
    suspend fun getDeviceIdAndNameMap(): Map<String, String>


    /**
     * Get the Device Name from Server
     *
     * @param deviceId The Device ID identifying the Device
     * @return [String] Device Name
     */
    suspend fun getDeviceName(deviceId: String): String?

    /**
     * Renames a Device
     *
     * @param deviceId The Device ID identifying the Device to be renamed
     * @param deviceName The new Device Name
     */
    suspend fun renameDevice(deviceId: String, deviceName: String)
}
