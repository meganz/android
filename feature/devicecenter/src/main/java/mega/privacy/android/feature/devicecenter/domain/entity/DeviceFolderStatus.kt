package mega.privacy.android.feature.devicecenter.domain.entity

import mega.privacy.android.domain.entity.sync.SyncError

/**
 * Sealed class representing the Status of each Sync/Backup Folder of a Device in Device Center
 *
 * @property priority When determining the Device Status to be displayed from the list of Backup
 * Folders, this decides what Status to be displayed. The higher the number, the more
 * that Status is prioritized
 */
sealed class DeviceFolderStatus(val priority: Int) {

    /**
     * The default value assigned when prioritizing what Device Status should be displayed and none
     * is found
     */
    data object Unknown : DeviceFolderStatus(0)

    /**
     * The Device is up to date
     */
    data object UpToDate : DeviceFolderStatus(1)

    /**
     * The Device Folder is updating
     *
     * @property progress The progress value
     */
    data class Updating(val progress: Int) : DeviceFolderStatus(2)

    /**
     * The Device Folder is paused (not applicable for CU or MU folders)
     */
    data object Paused : DeviceFolderStatus(3)

    /**
     * The Device Folder is disabled (applicable only for CU and MU folders)
     */
    data object Disabled : DeviceFolderStatus(3)

    /**
     * The Device Folder has errors
     *
     * @property errorSubState The corresponding Error Sub State
     */
    data class Error(val errorSubState: SyncError?) : DeviceFolderStatus(4)

    /**
     * The Device Folder is inactive
     */
    data object Inactive : DeviceFolderStatus(5)
}
