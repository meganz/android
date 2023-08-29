package mega.privacy.android.feature.devicecenter.ui.mapper

import mega.privacy.android.feature.devicecenter.domain.entity.BackupInfoType
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceFolderNode
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceCenterUINodeIcon
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceIconType
import javax.inject.Inject

/**
 * UI Mapper class that retrieves the appropriate Device Icon from a Device Node's Backup Folders
 *
 * The Mapper will eventually support more types of Device Icons when the SDK can more accurately
 * specify a parameter for a Device Icon
 */
internal class DeviceUINodeIconMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param deviceFolders A list of [DeviceFolderNode] objects from a Device Node
     * @return The correct Device Icon, represented as a [DeviceCenterUINodeIcon]
     */
    operator fun invoke(deviceFolders: List<DeviceFolderNode>) =
        if (deviceFolders.any { deviceFolderNode ->
                deviceFolderNode.type == BackupInfoType.CAMERA_UPLOADS || deviceFolderNode.type == BackupInfoType.MEDIA_UPLOADS
            }
        ) DeviceIconType.Mobile
        else DeviceIconType.PC
}