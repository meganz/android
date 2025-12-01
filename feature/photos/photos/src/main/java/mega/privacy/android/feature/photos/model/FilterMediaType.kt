package mega.privacy.android.feature.photos.model

import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON

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

        fun FilterMediaType.toMediaTypeValue() = when (this) {
            ALL_MEDIA -> TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_ALL_MEDIA.value
            IMAGES -> TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_IMAGES.value
            VIDEOS -> TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_VIDEOS.value
        }
    }
}