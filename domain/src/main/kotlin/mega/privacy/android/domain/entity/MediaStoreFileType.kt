package mega.privacy.android.domain.entity

/**
 * Type of media store file used to map android URIs
 */
enum class MediaStoreFileType {
    /**
     * type images internal
     */
    IMAGES_INTERNAL,

    /**
     * type images external
     */
    IMAGES_EXTERNAL,

    /**
     * type video internal
     */
    VIDEO_INTERNAL,

    /**
     * type video external
     */
    VIDEO_EXTERNAL;


    /**
     * Check if the MediaStoreFileType is of image type
     *
     * @return true if the MediaStoreFileType is of image type
     */
    fun isImageFileType(): Boolean = when (this) {
        IMAGES_INTERNAL, IMAGES_EXTERNAL -> true
        else -> false
    }
}
