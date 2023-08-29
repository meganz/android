package mega.privacy.android.feature.devicecenter.domain.entity

/**
 * Enum class representing the different Backup States mapped from [nz.mega.sdk.MegaBackupInfo.state]
 */
enum class BackupInfoState {

    /**
     * Represents an uninitialized State
     */
    NOT_INITIALIZED,

    /**
     * Represents an Active State
     */
    ACTIVE,

    /**
     * Represents a Failed State
     */
    FAILED,

    /**
     * Represents a Temporarily Disabled State
     */
    TEMPORARY_DISABLED,

    /**
     * Represents a Disabled Uploads State
     */
    DISABLED,

    /**
     * Represents a Pause Uploads State
     */
    PAUSE_UP,

    /**
     * Represents a Pause Downloads State
     */
    PAUSE_DOWN,

    /**
     * Represents a State in which both Uploads and Downloads are paused
     */
    PAUSE_FULL,

    /**
     * Represents a Deleted State
     */
    DELETED,
}