package mega.privacy.android.feature.devicecenter.ui.model

import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceCenterUINodeIcon
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus

/**
 * A UI data class representing a Backup Folder of a Device
 *
 * One Scenario that this Folder can be created is through MEGAsync, wherein the User selects a
 * Folder to automatically back up content
 *
 * @property id The Backup Folder ID
 * @property name The Backup Folder Name
 * @property icon The Backup Folder Icon
 * @property status The Backup Folder Status
 * @property rootHandle The Backup Folder Root Handle
 */
data class BackupDeviceFolderUINode(
    override val id: String,
    override val name: String,
    override val icon: DeviceCenterUINodeIcon,
    override val status: DeviceCenterUINodeStatus,
    val rootHandle: Long,
) : DeviceFolderUINode
