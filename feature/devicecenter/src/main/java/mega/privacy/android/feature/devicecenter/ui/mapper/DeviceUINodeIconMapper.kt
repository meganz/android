package mega.privacy.android.feature.devicecenter.ui.mapper

import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.backup.BackupInfoUserAgent
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceFolderNode
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceIconType
import javax.inject.Inject

/**
 * UI Mapper class that retrieves the appropriate [DeviceIconType] from the User Agents of a Device
 * Node's Backup Folders
 *
 * The order for determining the Device Icon is as follows:
 * 1. Windows
 * 2. Linux
 * 3. Mac
 * 4. Android
 * 5. iPhone
 *
 * If there are no matching User Agents, the Mobile Device icon is returned if the Device is a
 * Mobile Device. Otherwise, the default PC icon is returned
 */
internal class DeviceUINodeIconMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param deviceFolders A list of [DeviceFolderNode] objects from a Device Node
     * @return The correct [DeviceIconType]
     */
    operator fun invoke(deviceFolders: List<DeviceFolderNode>) =
        deviceFolders.firstOrNull { it.userAgent != BackupInfoUserAgent.UNKNOWN }?.let { folder ->
            when {
                folder.userAgent == BackupInfoUserAgent.WINDOWS -> DeviceIconType.Windows
                folder.userAgent == BackupInfoUserAgent.LINUX -> DeviceIconType.Linux
                folder.userAgent == BackupInfoUserAgent.MAC -> DeviceIconType.Mac
                folder.userAgent == BackupInfoUserAgent.ANDROID -> DeviceIconType.Android
                folder.userAgent == BackupInfoUserAgent.IPHONE -> DeviceIconType.IOS
                folder.type in listOf(
                    BackupInfoType.CAMERA_UPLOADS,
                    BackupInfoType.MEDIA_UPLOADS,
                ) -> DeviceIconType.Mobile

                else -> DeviceIconType.PC
            }
        } ?: DeviceIconType.PC
}