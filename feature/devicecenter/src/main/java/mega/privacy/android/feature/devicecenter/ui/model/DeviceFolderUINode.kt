package mega.privacy.android.feature.devicecenter.ui.model

/**
 * A UI interface representing a Folder linked to a Device
 *
 * @property rootHandle The Backup Folder Root Handle
 */
interface DeviceFolderUINode : DeviceCenterUINode {
    val rootHandle: Long
}