package mega.privacy.android.feature.devicecenter.data.entity

/**
 * Enum class representing the different Sync States mapped from [nz.mega.sdk.MegaBackupInfo.state]
 */
enum class SyncState {

    /**
     * Represents an uninitialized Sync State
     */
    NOT_INITIALIZED,

    /**
     * Represents an Active State
     */
    ACTIVE,

    /**
     * Represents a Failed Sync State
     */
    FAILED,

    /**
     * Represents a Temporarily Disabled Sync State
     */
    TEMPORARY_DISABLED,

    /**
     * Represents a Disabled Uploads Sync State
     */
    DISABLED,

    /**
     * Represents a Pause Uploads Sync State
     */
    PAUSE_UP,

    /**
     * Represents a Pause Downloads Sync State
     */
    PAUSE_DOWN,

    /**
     * Represents a Sync State in which both Uploads and Downloads are paused
     */
    PAUSE_FULL,

    /**
     * Represents a Deleted Sync State
     */
    DELETED,
}