package mega.privacy.android.feature.devicecenter.ui.model

/**
 * A UI interface representing any of the Backup Devices linked to the User
 *
 * @property folders The list of Folders linked to that Device as [DeviceFolderUINode] objects
 */
interface DeviceUINode : DeviceCenterUINode {
    val folders: List<DeviceFolderUINode>
}