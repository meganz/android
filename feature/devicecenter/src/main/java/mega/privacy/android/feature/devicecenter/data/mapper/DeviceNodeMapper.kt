package mega.privacy.android.feature.devicecenter.data.mapper

import mega.privacy.android.domain.entity.backup.BackupInfo
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceNode
import mega.privacy.android.feature.devicecenter.domain.entity.OtherDeviceNode
import mega.privacy.android.feature.devicecenter.domain.entity.OwnDeviceNode
import javax.inject.Inject

/**
 * Mapper class that converts the User's Backup Information into a list of Device Nodes
 *
 * @property deviceFolderNodeMapper [DeviceFolderNodeMapper]
 * @property deviceNodeStatusMapper [DeviceNodeStatusMapper]
 */
internal class DeviceNodeMapper @Inject constructor(
    private val deviceFolderNodeMapper: DeviceFolderNodeMapper,
    private val deviceNodeStatusMapper: DeviceNodeStatusMapper,
) {
    /**
     * Invocation function
     *
     * @param backupInfoList A list of [BackupInfo] objects
     * @param currentDeviceId The ID of the User's Current Device
     * @param deviceIdAndNameMap A String map of the User's backed up Device IDs and Device Names.
     * Each Key-Value entry corresponds to the User's Device ID and Device Name
     * @param isCameraUploadsEnabled true if Camera Uploads is enabled, and false if otherwise
     *
     * @return A list of [DeviceNode] objects
     */
    operator fun invoke(
        backupInfoList: List<BackupInfo>,
        currentDeviceId: String,
        deviceIdAndNameMap: Map<String, String>,
        isCameraUploadsEnabled: Boolean,
    ): List<DeviceNode> {
        val deviceNodeList = mutableListOf<DeviceNode>()

        // Own Device
        val currentDeviceFolders =
            deviceFolderNodeMapper(backupInfoList.filterBackupInfoByDeviceId(currentDeviceId))
        deviceNodeList.add(
            OwnDeviceNode(
                id = currentDeviceId,
                name = deviceIdAndNameMap[currentDeviceId].orEmpty(),
                status = deviceNodeStatusMapper(
                    folders = currentDeviceFolders,
                    isCameraUploadsEnabled = isCameraUploadsEnabled,
                    isCurrentDevice = true,
                ),
                folders = currentDeviceFolders,
            )
        )

        // Other Devices
        deviceIdAndNameMap.filter { (deviceId) -> deviceId != currentDeviceId }
            .forEach { (otherDeviceId, otherDeviceName) ->
                val otherDeviceFolders =
                    deviceFolderNodeMapper(backupInfoList.filterBackupInfoByDeviceId(otherDeviceId))
                if (otherDeviceFolders.isNotEmpty()) {
                    deviceNodeList.add(
                        OtherDeviceNode(
                            id = otherDeviceId,
                            name = otherDeviceName,
                            status = deviceNodeStatusMapper(
                                folders = otherDeviceFolders,
                                isCameraUploadsEnabled = isCameraUploadsEnabled,
                                isCurrentDevice = false,
                            ),
                            folders = otherDeviceFolders,
                        )
                    )
                }
            }

        return deviceNodeList.toList()
    }

    /**
     * Returns a list of [BackupInfo] objects based on the Device ID that was passed
     *
     * @param deviceId The Device ID used to filter the data
     * @return A list of [BackupInfo] objects having the same Device ID
     */
    private fun List<BackupInfo>.filterBackupInfoByDeviceId(deviceId: String) =
        this.filter { backupInfo -> backupInfo.deviceId == deviceId }
}