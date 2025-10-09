package mega.privacy.android.feature.devicecenter.ui.mapper

import mega.privacy.android.feature.devicecenter.domain.entity.DeviceNode
import mega.privacy.android.feature.devicecenter.domain.entity.OwnDeviceNode
import mega.privacy.android.feature.devicecenter.ui.model.OtherDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.OwnDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceIconType
import javax.inject.Inject

/**
 * UI Mapper class that converts a list of [DeviceNode] objects into a list of Device UI Node objects
 *
 * @property deviceCenterUINodeStatusMapper [DeviceCenterUINodeStatusMapper]
 * @property deviceFolderUINodeListMapper [DeviceFolderUINodeListMapper]
 * @property deviceUINodeIconMapper [DeviceUINodeIconMapper]
 */
internal class DeviceUINodeListMapper @Inject constructor(
    private val deviceCenterUINodeStatusMapper: DeviceCenterUINodeStatusMapper,
    private val deviceFolderUINodeListMapper: DeviceFolderUINodeListMapper,
    private val deviceUINodeIconMapper: DeviceUINodeIconMapper,
) {

    /**
     * Invocation function
     *
     * @param deviceNodes a list of [DeviceNode] objects
     *
     * @return a list of Device UI Node objects
     */
    operator fun invoke(
        deviceNodes: List<DeviceNode>,
    ) = deviceNodes.map { deviceNode ->
        if (deviceNode is OwnDeviceNode) {
            OwnDeviceUINode(
                id = deviceNode.id,
                name = deviceNode.name,
                icon = DeviceIconType.Android,
                status = deviceCenterUINodeStatusMapper(deviceNode.status),
                folders = deviceFolderUINodeListMapper(
                    folders = deviceNode.folders,
                ),
            )
        } else {
            OtherDeviceUINode(
                id = deviceNode.id,
                name = deviceNode.name,
                icon = deviceUINodeIconMapper(deviceNode.folders),
                status = deviceCenterUINodeStatusMapper(deviceNode.status),
                folders = deviceFolderUINodeListMapper(
                    folders = deviceNode.folders,
                ),
            )
        }
    }
}
