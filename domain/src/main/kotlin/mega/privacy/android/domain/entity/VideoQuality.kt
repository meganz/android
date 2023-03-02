package mega.privacy.android.domain.entity

/**
 * Enum class that specifies all available Video Quality options for Camera Uploads
 *
 * @property value [Int] representation of the enum
 */
enum class VideoQuality(val value: Int) {
    /**
     * Uploaded Videos will be in Low Quality
     */
    LOW(0),

    /**
     * Uploaded Videos will be in Medium Quality
     */
    MEDIUM(1),

    /**
     * Uploaded Videos will be in High Quality
     */
    HIGH(2),

    /**
     * Uploaded Videos will be in Original Quality
     */
    ORIGINAL(3),
}
