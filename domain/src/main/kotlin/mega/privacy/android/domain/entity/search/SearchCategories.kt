package mega.privacy.android.domain.entity.search

/**
 * Enum class representing search category
 *
 */
enum class SearchCategory {
    /**
     * ALL
     *
     * all will be selected by default and shows all items based on search keyword
     */
    ALL,

    /**
     * IMAGES
     *
     * shows only images which is matching the search keyword
     */
    IMAGES,

    /**
     * Documents
     *
     * shows only documents which is matching the search keyword
     */
    ALL_DOCUMENTS,

    /**
     * AUDIO
     *
     * shows only audios which is matching the search keyword
     */
    AUDIO,

    /**
     * VIDEO
     *
     * shows only videos which is matching the search keyword
     */
    VIDEO,
}
