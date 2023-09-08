package mega.privacy.android.feature.devicecenter.domain.entity

import mega.privacy.android.domain.entity.backup.BackupInfoSubState

/**
 * Sealed class representing the Status of each Device Center Node
 *
 * @property priority When determining the Device Status to be displayed from the list of Backup
 * Folders, this decides what Status to be displayed. The higher the number, the more
 * that Status is prioritized
 */
sealed class DeviceCenterNodeStatus(val priority: Int) {

    /**
     * The default value assigned when prioritizing what Device Status should be displayed and none
     * is found
     */
    object Unknown : DeviceCenterNodeStatus(0)

    /**
     * The Device is Stopped
     */
    object Stopped : DeviceCenterNodeStatus(1)

    /**
     * The Device is Disabled by the User
     */
    object Disabled : DeviceCenterNodeStatus(2)

    /**
     * The Device is Offline
     */
    object Offline : DeviceCenterNodeStatus(3)

    /**
     * The Device is Up to Date
     */
    object UpToDate : DeviceCenterNodeStatus(4)

    /**
     * The Device is Blocked
     *
     * @property errorSubState The corresponding Error Sub State
     */
    data class Blocked(val errorSubState: BackupInfoSubState?) : DeviceCenterNodeStatus(5)

    /**
     * The Device is Overquota
     */
    object Overquota : DeviceCenterNodeStatus(6)

    /**
     * The Device is Paused
     */
    object Paused : DeviceCenterNodeStatus(7)

    /**
     * The Device is Initializing
     */
    object Initializing : DeviceCenterNodeStatus(8)

    /**
     * The Device is Scanning
     */
    object Scanning : DeviceCenterNodeStatus(9)

    /**
     * The Device is Syncing
     *
     * @property progress The progress value
     */
    data class Syncing(val progress: Int) : DeviceCenterNodeStatus(10)

    /**
     * The Device is found to have its Camera Uploads disabled. This is only applicable for the
     * User's Current Device, and has the highest priority amongst all Device Statuses
     */
    object NoCameraUploads : DeviceCenterNodeStatus(11)
}
