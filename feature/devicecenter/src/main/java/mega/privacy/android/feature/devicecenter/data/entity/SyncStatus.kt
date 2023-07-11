package mega.privacy.android.feature.devicecenter.data.entity

/**
 * Enum class representing the different Sync Statuses mapped from
 * [nz.mega.sdk.MegaBackupInfo.status]
 */
enum class SyncStatus {

    /**
     * Represents an Uninitialized Sync Status
     */
    NOT_INITIALIZED,

    /**
     * Represents an Up to Date Sync Status
     */
    UPTODATE,

    /**
     * Represents a Syncing Sync Status
     */
    SYNCING,

    /**
     * Represents a Pending Sync Status
     */
    PENDING,

    /**
     * Represents an Inactive Sync Status
     */
    INACTIVE,

    /**
     * Represents an Unknown Sync Status
     */
    UNKNOWN,
}