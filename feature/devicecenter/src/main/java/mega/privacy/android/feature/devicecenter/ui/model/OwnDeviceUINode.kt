package mega.privacy.android.feature.devicecenter.ui.model

import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceCenterUINodeIcon
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus

/**
 * A UI data class representing the User's Backup Device currently being used
 *
 * @property id The Current Device ID
 * @property name The Current Device Name
 * @property icon The Current Device Icon from [DeviceCenterUINodeIcon]
 * @property status The Current Device Status from [DeviceCenterUINodeStatus]
 * @property folders The list of Folders linked to that Device as [DeviceFolderUINode] objects
 */
data class OwnDeviceUINode(
    override val id: String,
    override val name: String,
    override val icon: DeviceCenterUINodeIcon,
    override val status: DeviceCenterUINodeStatus,
    override val folders: List<DeviceFolderUINode>,
) : DeviceUINode
