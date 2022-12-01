package mega.privacy.android.app.presentation.photos.model

/**
 * Sort enum class for Timeline
 */
enum class Sort {
    /**
     * Sort by newest first
     */
    NEWEST,

    /**
     * Sort by oldest first
     */
    OLDEST,

    /**
     * Sort by photos with newest first
     */
    PHOTOS,

    /**
     * Sort by videos with newest first
     */
    VIDEOS;

    companion object {
        /**
         * The default selected media type
         */
        val DEFAULT = NEWEST
    }
}