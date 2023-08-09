package mega.privacy.android.feature.devicecenter.ui.mapper

import androidx.annotation.DrawableRes
import mega.privacy.android.feature.devicecenter.R
import mega.privacy.android.feature.devicecenter.data.entity.BackupInfoType
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceNode
import javax.inject.Inject

/**
 * UI Mapper class that retrieves the appropriate Device Icon. The Mapper will eventually
 * support more types of Device Icons when the SDK can more accurately specify a parameter for a
 * Device Icon
 */
internal class DeviceUINodeIconMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param deviceNode The Device Node
     * @return The correct Device Icon, represented as an [Int]
     */
    @DrawableRes
    operator fun invoke(deviceNode: DeviceNode) =
        if (deviceNode.folders.any { deviceFolderNode ->
                deviceFolderNode.type == BackupInfoType.CAMERA_UPLOADS || deviceFolderNode.type == BackupInfoType.MEDIA_UPLOADS
            }
        ) R.drawable.ic_device_mobile
        else R.drawable.ic_device_pc
}