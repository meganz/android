package mega.privacy.android.feature.devicecenter.domain.entity

/**
 * A domain interface representing any of the Backup Devices linked to the User
 *
 * @property id The Device ID
 * @property name The Device Name
 * @property status The Device Status
 * @property folders The list of Folders linked to that Device
 */
interface DeviceNode : DeviceCenterNode {
    override val id: String
    override val name: String
    val status: DeviceStatus
    val folders: List<DeviceFolderNode>
}