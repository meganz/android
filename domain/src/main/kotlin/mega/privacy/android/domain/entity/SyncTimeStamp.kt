package mega.privacy.android.domain.entity

/**
 * All different synchronization timestamps
 */
enum class SyncTimeStamp {
    /**
     * only primary photos
     */
    PRIMARY_PHOTO,

    /**
     * primary videos
     */
    PRIMARY_VIDEO,

    /**
     * only secondary photos
     */
    SECONDARY_PHOTO,

    /**
     * secondary videos
     */
    SECONDARY_VIDEO
}
