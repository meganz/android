package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.DeviceType
import mega.privacy.android.domain.entity.node.FolderNode

/**
 * Get device type for the selected folder
 */
fun interface GetDeviceType {
    /**
     * Invoke
     *
     * @param folder
     * @return the device type or unknown if none matches
     */
    suspend operator fun invoke(folder: FolderNode): DeviceType
}