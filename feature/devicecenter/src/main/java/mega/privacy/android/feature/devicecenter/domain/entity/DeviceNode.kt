package mega.privacy.android.feature.devicecenter.domain.entity

/**
 * A domain interface representing any of the Backup Devices linked to the User
 *
 * @property folders The list of Backup Folders linked to that Device
 */
interface DeviceNode : DeviceCenterNode {
    val folders: List<DeviceFolderNode>
}