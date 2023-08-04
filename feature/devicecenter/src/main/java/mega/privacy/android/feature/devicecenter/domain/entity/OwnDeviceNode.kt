package mega.privacy.android.feature.devicecenter.domain.entity

/**
 * A domain data class representing the User's Backup Device currently being used
 *
 * @property id The Current Device ID
 * @property name The Current Device Name
 * @property status The Current Device Status
 * @property folders The list of Backup Folders linked to that Device
 */
data class OwnDeviceNode(
    override val id: String,
    override val name: String,
    override val status: DeviceCenterNodeStatus,
    override val folders: List<DeviceFolderNode>,
) : DeviceNode