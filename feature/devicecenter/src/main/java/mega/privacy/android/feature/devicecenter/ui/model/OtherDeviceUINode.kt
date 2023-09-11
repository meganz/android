package mega.privacy.android.feature.devicecenter.ui.model

import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceCenterUINodeIcon
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus

/**
 * A UI data class representing the User's other Backup Devices currently not in use
 *
 * @property id The Other Device ID
 * @property name The Other Device Name
 * @property icon The Other Device Icon from [DeviceCenterUINodeIcon]
 * @property status The Other Device Status from [DeviceCenterUINodeStatus]
 * @property folders The list of Folders linked to that Device as [DeviceFolderUINode] objects
 */
data class OtherDeviceUINode(
    override val id: String,
    override val name: String,
    override val icon: DeviceCenterUINodeIcon,
    override val status: DeviceCenterUINodeStatus,
    override val folders: List<DeviceFolderUINode>,
) : DeviceUINode