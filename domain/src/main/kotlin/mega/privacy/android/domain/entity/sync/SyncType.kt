package mega.privacy.android.domain.entity.sync

/**
 * Enum class representing the different Sync Types mapped from
 * nz.mega.sdk.MegaSync.SyncType
 */
enum class SyncType {
    /**
     * Represents an Unknown Sync Type
     */
    TYPE_UNKNOWN,

    /**
     * Represents a Two-way Sync Type
     */
    TYPE_TWOWAY,

    /**
     * Represents a Backup (one-way) Sync Type
     */
    TYPE_BACKUP,

    /**
     * Represents a Camera Uploads Sync Type
     */
    TYPE_CAMERA_UPLOADS,

    /**
     * Represents a Media Uploads Sync Type
     */
    TYPE_MEDIA_UPLOADS,
}