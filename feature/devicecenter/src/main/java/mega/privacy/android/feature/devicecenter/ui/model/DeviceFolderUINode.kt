package mega.privacy.android.feature.devicecenter.ui.model

import androidx.annotation.DrawableRes
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus

/**
 * A UI data class representing a Backup Folder of a Backup Device
 *
 * @property id The Device Folder ID
 * @property name The Device Folder Name
 * @property icon The Device Folder Icon as a [DrawableRes]
 * @property status The Device Folder Status from [DeviceCenterUINodeStatus]
 */
data class DeviceFolderUINode(
    override val id: String,
    override val name: String,
    @DrawableRes override val icon: Int,
    override val status: DeviceCenterUINodeStatus,
) : DeviceCenterUINode
