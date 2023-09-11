package mega.privacy.android.feature.devicecenter.domain.entity

/**
 * A domain data class representing the User's other Backup Devices currently not in use
 *
 * @property id The Other Device ID
 * @property name The Other Device Name
 * @property status The Other Device Status
 * @property folders The list of Folders linked to that Device
 */
data class OtherDeviceNode(
    override val id: String,
    override val name: String,
    override val status: DeviceCenterNodeStatus,
    override val folders: List<DeviceFolderNode>,
) : DeviceNode
