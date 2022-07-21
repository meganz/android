package mega.privacy.android.domain.entity

/**
 * Video quality for camera upload
 */
enum class VideoQuality(
    /**
     * video quality value
     */
    val value: Int,
) {
    /**
     * low quality
     */
    LOW(0),

    /**
     * medium quality
     */
    MEDIUM(1),

    /**
     * high quality
     */
    HIGH(2),

    /**
     * original quality
     */
    ORIGINAL(3),
}
