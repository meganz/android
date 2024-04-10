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
     * includes DOCUMENT, PDF, PRESENTATION, SPREADSHEET
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

    /**
     * PDF
     *
     * shows only pdf files which is matching the search keyword
     */
    PDF,

    /**
     * PRESENTATION
     *
     * shows only presentation files which is matching the search keyword
     */
    PRESENTATION,

    /**
     * SPREADSHEET
     *
     * shows only spreadsheet files which is matching the search keyword
     */
    SPREADSHEET,

    /**
     * FOLDER
     *
     * shows only folders which is matching the search keyword
     */
    FOLDER,

    /**
     * OTHER
     *
     * shows only other files which is matching the search keyword
     */
    OTHER,

    /**
     * DOCUMENTS
     *
     * shows only documents which is matching the search keyword
     */
    DOCUMENTS
}
