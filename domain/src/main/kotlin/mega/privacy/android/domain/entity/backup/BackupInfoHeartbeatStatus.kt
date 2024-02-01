package mega.privacy.android.domain.entity.backup

/**
 * Enum class representing the different Backup Heartbeat Statuses mapped from
 * nz.mega.sdk.MegaBackupInfo.status
 */
enum class BackupInfoHeartbeatStatus {

    /**
     * Represents an Uninitialized Heartbeat Status
     */
    NOT_INITIALIZED,

    /**
     * Represents an Up to Date Heartbeat Status
     */
    UPTODATE,

    /**
     * Represents a Syncing Heartbeat Status
     */
    SYNCING,

    /**
     * Represents a Pending Heartbeat Status
     */
    PENDING,

    /**
     * Represents an Inactive Heartbeat Status
     */
    INACTIVE,

    /**
     * Represents an Unknown Heartbeat Status
     */
    UNKNOWN,

    /**
     * Represents a Stalled Heartbeat Status, which is only applied to Syncs
     */
    STALLED,
}