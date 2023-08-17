package mega.privacy.android.feature.devicecenter.ui.mapper

import mega.privacy.android.feature.devicecenter.domain.entity.DeviceFolderNode
import mega.privacy.android.feature.devicecenter.ui.model.DeviceFolderUINode
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
    operator fun invoke(folders: List<DeviceFolderNode>) = folders.map { folder ->
        DeviceFolderUINode(
            id = folder.id,
            name = folder.name,
            icon = deviceFolderUINodeIconMapper(folder.type),
            status = deviceCenterUINodeStatusMapper(folder.status),
        )
    }
}