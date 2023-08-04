package mega.privacy.android.feature.devicecenter.data.mapper

import mega.privacy.android.feature.devicecenter.data.entity.BackupInfo
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
     * @param currentDeviceId The ID of the User's Current Device
     * @param backupInfoList A list of [BackupInfo] objects
     * @param deviceIdAndNameMap A String map of the User's backed up Device IDs and Device Names.
     * Each Key-Value entry corresponds to the User's Device ID and Device Name
     *
     * @return A list of [DeviceNode] objects
     */
    operator fun invoke(
        currentDeviceId: String,
        backupInfoList: List<BackupInfo>,
        deviceIdAndNameMap: Map<String, String>,
    ): List<DeviceNode> {
        val deviceNodeList = mutableListOf<DeviceNode>()

        // Own Device
        val currentDeviceFolders =
            deviceFolderNodeMapper(backupInfoList.filterBackupInfoByDeviceId(currentDeviceId))
        deviceNodeList.add(
            OwnDeviceNode(
                id = currentDeviceId,
                name = deviceIdAndNameMap[currentDeviceId] ?: "",
                status = deviceNodeStatusMapper(currentDeviceFolders),
                folders = currentDeviceFolders,
            )
        )

        // Other Devices
        val otherDevicesIdAndNameMap =
            deviceIdAndNameMap.filter { (deviceId) -> deviceId != currentDeviceId }
        otherDevicesIdAndNameMap.forEach { (otherDeviceId, otherDeviceName) ->
            val otherDeviceFolders =
                deviceFolderNodeMapper(backupInfoList.filterBackupInfoByDeviceId(otherDeviceId))
            deviceNodeList.add(
                OtherDeviceNode(
                    id = otherDeviceId,
                    name = otherDeviceName,
                    status = deviceNodeStatusMapper(otherDeviceFolders),
                    folders = otherDeviceFolders,
                )
            )
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