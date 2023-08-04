package mega.privacy.android.feature.devicecenter.domain.entity

import mega.privacy.android.feature.devicecenter.data.entity.BackupInfoSubState

/**
 * Sealed class representing the Status of each Device Center Node
 *
 * @property priority When determining the Device Status to be displayed from the list of Backup
 * Folders, this decides what Status to be displayed. The higher the number, the more
 * that Status is prioritized
 */
sealed class DeviceCenterNodeStatus(val priority: Int) {

    /**
     * The default value assigned when prioritizing what Status should be displayed and none
     * is found
     */
    object Unknown : DeviceCenterNodeStatus(0)

    /**
     * The Node is Stopped
     */
    object Stopped : DeviceCenterNodeStatus(1)

    /**
     * The Node is Disabled by the User
     */
    object Disabled : DeviceCenterNodeStatus(2)

    /**
     * The Node is Offline
     */
    object Offline : DeviceCenterNodeStatus(3)

    /**
     * The Node is Up to Date
     */
    object UpToDate : DeviceCenterNodeStatus(4)

    /**
     * The Node is Blocked
     *
     * @property errorSubState The corresponding Error Sub State
     */
    data class Blocked(val errorSubState: BackupInfoSubState?) : DeviceCenterNodeStatus(5)

    /**
     * The Node is Overquota
     */
    object Overquota : DeviceCenterNodeStatus(6)

    /**
     * The Node is Paused
     */
    object Paused : DeviceCenterNodeStatus(7)

    /**
     * The Node is Initializing
     */
    object Initializing : DeviceCenterNodeStatus(8)

    /**
     * The Node is Scanning
     */
    object Scanning : DeviceCenterNodeStatus(9)

    /**
     * The Node is Syncing
     *
     * @property progress The progress value
     */
    data class Syncing(val progress: Int) : DeviceCenterNodeStatus(10)
}
