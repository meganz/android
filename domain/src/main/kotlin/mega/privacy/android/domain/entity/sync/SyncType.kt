package mega.privacy.android.domain.entity.sync

/**
 * Enum class representing the different Sync Types
 *
 * All enum names are exactly the same as what was specified in the documentation of
 * MegaBackupInfo.type in megaapi.h for consistency
 */
enum class SyncType {
    /**
     * Represents an invalid Sync, equivalent to INVALID = -1. This is also returned if no
     * matching value for MegaBackupInfo.type is found
     */
    INVALID,

    /**
     * Represents a Two-Way Sync, equivalent to TWO_WAY = 0 in the SDK
     */
    TWO_WAY,

    /**
     * Represents a Sync for Uploads only, equivalent to UP_SYNC = 1 in the SDK
     */
    UP_SYNC,

    /**
     * Represents a Sync for Downloads only, equivalent to DOWN_SYNC = 2 in the SDK
     */
    DOWN_SYNC,

    /**
     * Represents the Sync for the Primary Folder in Camera Uploads, equivalent to CAMERA_UPLOAD = 3
     * in the SDK
     */
    CAMERA_UPLOAD,

    /**
     * Represents the Sync for the Secondary Folder (Media Uploads) in Camera Uploads, equivalent
     * to MEDIA_UPLOAD = 4 in the SDK
     */
    MEDIA_UPLOAD,

    /**
     * Represents a Backup Sync, equivalent to BACKUP_UPLOAD = 5 in the SDK
     */
    BACKUP_UPLOAD,
}