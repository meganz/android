package mega.privacy.android.feature.devicecenter.data.entity

/**
 * Enum class representing the different Sync Types mapped from [nz.mega.sdk.MegaBackupInfo.type]
 */
enum class SyncType {
    /**
     * Represents an Invalid Sync Type
     */
    INVALID,

    /**
     * Represents a Two-Way Sync Type
     */
    TWO_WAY_SYNC,

    /**
     * Represents a Sync Type for Uploads only
     */
    UP_SYNC,

    /**
     * Represents a Sync Type for Downloads only
     */
    DOWN_SYNC,

    /**
     * Represents the Sync Type for the Primary Folder in Camera Uploads
     * in the SDK
     */
    CAMERA_UPLOADS,

    /**
     * Represents the Sync Type for the Secondary Folder (Media Uploads) in Camera Uploads
     */
    MEDIA_UPLOADS,
}