package mega.privacy.android.feature.devicecenter.ui.mapper

import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceFolderNode
import mega.privacy.android.feature.devicecenter.ui.model.BackupDeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.DeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.NonBackupDeviceFolderUINode
import javax.inject.Inject

/**
 * UI Mapper class that converts a list of [DeviceFolderNode] objects into a list of
 * [DeviceFolderUINode] objects
 *
 * @property deviceCenterUINodeStatusMapper [DeviceCenterUINodeStatusMapper]
 * @property deviceFolderUINodeIconMapper [DeviceFolderUINodeIconMapper]
 */
internal class DeviceFolderUINodeListMapper @Inject constructor(
    private val deviceCenterUINodeStatusMapper: DeviceCenterUINodeStatusMapper,
    private val deviceFolderUINodeIconMapper: DeviceFolderUINodeIconMapper,
) {

    /**
     * Invocation function
     *
     * @param folders a list of [DeviceFolderNode] objects
     * @return a list of [DeviceFolderUINode] objects
     */
    operator fun invoke(folders: List<DeviceFolderNode>): List<DeviceFolderUINode> =
        folders.map { folder ->
            if (folder.type == BackupInfoType.BACKUP_UPLOAD) {
                BackupDeviceFolderUINode(
                    id = folder.id,
                    name = folder.name,
                    icon = deviceFolderUINodeIconMapper(folder.type),
                    status = deviceCenterUINodeStatusMapper(
                        isDevice = false,
                        status = folder.status,
                    ),
                    rootHandle = folder.rootHandle,
                )
            } else {
                NonBackupDeviceFolderUINode(
                    id = folder.id,
                    name = folder.name,
                    icon = deviceFolderUINodeIconMapper(folder.type),
                    status = deviceCenterUINodeStatusMapper(
                        isDevice = false,
                        status = folder.status,
                    ),
                    rootHandle = folder.rootHandle,
                )
            }
        }
}