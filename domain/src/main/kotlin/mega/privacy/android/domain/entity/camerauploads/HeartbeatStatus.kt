package mega.privacy.android.domain.entity.camerauploads

/**
 * Enum class that provides different types of Heartbeat Statuses
 *
 * @param value The value of the Heartbeat Status to be sent to the API
 */
enum class HeartbeatStatus(val value: Int) {

    /**
     * Up to Date Heartbeat that is sent when the local and remote
     */
    UP_TO_DATE(1),

    /**
     * Syncing Heartbeat, sent when the Transfers are in progress
     */
    SYNCING(2),

    /**
     * Pending Heartbeat, sent when scanning local folders
     */
    PENDING(3),

    /**
     * Inactive Heartbeat, sent when the sync is not active
     */
    INACTIVE(4),

    /**
     * Unknown Heartbeat, sent when a Backup is enabled (CU/MU)
     */
    UNKNOWN(5),
}