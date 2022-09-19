package mega.privacy.android.app.presentation.photos.timeline.model

/**
 * Filter enum class for Timeline
 */
enum class FilterMediaType {
    /**
     * All Photos from CD + CU + MU
     */
    ALL_MEDIA,


    /**
     * All Photos from CD
     */
    IMAGES,

    /**
     * All videos from CD + CU + MU
     */
    VIDEOS;

    companion object {
        /**
         * The default selected media type
         */
        val DEFAULT = ALL_MEDIA
    }
}