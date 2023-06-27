package mega.privacy.android.domain.entity.chat

/**
 * Enum class for storing the different types of [ContainsMeta]
 */
enum class ContainsMetaType {

    /**
     * Unknown type of meta contained.
     */
    INVALID,

    /**
     * Rich-preview type for meta contained
     */
    RICH_PREVIEW,

    /**
     * ChatGeolocation type for meta contained.
     */
    GEOLOCATION,

    /**
     * Giphy type for meta contained.
     */
    GIPHY
}