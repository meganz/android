package mega.privacy.android.feature.devicecenter.domain.entity

/**
 * A domain data class representing a Backup Folder of a Backup Device
 *
 * @property id The Device Folder ID
 * @property name The Device Folder Name
 * @property status The Device Folder Status
 * @property type The Device Folder Type
 */
data class DeviceFolderNode(
    override val id: String,
    override val name: String,
    override val status: DeviceCenterNodeStatus,
    val type: BackupInfoType,
) : DeviceCenterNode