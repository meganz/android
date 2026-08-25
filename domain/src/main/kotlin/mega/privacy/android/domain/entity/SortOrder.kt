package mega.privacy.android.domain.entity

/**
 * Enum class for Sort Order
 */
enum class SortOrder {
    /**
     * Order NONE refers to MegaApiJava.ORDER_NONE
     */
    ORDER_NONE,

    /**
     * Order ORDER_DEFAULT_ASC refers to MegaApiJava.ORDER_DEFAULT_ASC
     */
    ORDER_DEFAULT_ASC,

    /**
     * Order ORDER_DEFAULT_DESC refers to MegaApiJava.ORDER_DEFAULT_DESC
     */
    ORDER_DEFAULT_DESC,

    /**
     * Order ORDER_SIZE_ASC refers to MegaApiJava.ORDER_SIZE_ASC
     */
    ORDER_SIZE_ASC,

    /**
     * Order ORDER_SIZE_DESC refers to MegaApiJava.ORDER_SIZE_DESC
     */
    ORDER_SIZE_DESC,

    /**
     * Order ORDER_CREATION_ASC refers to MegaApiJava.ORDER_CREATION_ASC
     */
    ORDER_CREATION_ASC,

    /**
     * Order ORDER_CREATION_DESC refers to MegaApiJava.ORDER_CREATION_DESC
     */
    ORDER_CREATION_DESC,

    /**
     * Order ORDER_MODIFICATION_ASC refers to MegaApiJava.ORDER_MODIFICATION_ASC
     */
    ORDER_MODIFICATION_ASC,

    /**
     * Order ORDER_MODIFICATION_DESC refers to MegaApiJava.ORDER_MODIFICATION_DESC
     */
    ORDER_MODIFICATION_DESC,

    /**
     * Order ORDER_LINK_CREATION_ASC refers to MegaApiJava.ORDER_LINK_CREATION_ASC
     */
    ORDER_LINK_CREATION_ASC,

    /**
     * Order ORDER_LINK_CREATION_DESC refers to MegaApiJava.ORDER_LINK_CREATION_DESC
     */
    ORDER_LINK_CREATION_DESC,

    /**
     * Order ORDER_LABEL_ASC refers to MegaApiJava.ORDER_LABEL_ASC
     */
    ORDER_LABEL_ASC,

    /**
     * Order ORDER_LABEL_DESC refers to MegaApiJava.ORDER_LABEL_DESC
     */
    ORDER_LABEL_DESC,

    /**
     * Order ORDER_FAV_ASC refers to MegaApiJava.ORDER_FAV_ASC
     */
    ORDER_FAV_ASC,

    /**
     * Order ORDER_FAV_DESC refers to MegaApiJava.ORDER_FAV_DESC
     */
    ORDER_FAV_DESC,

    /**
     * Order ORDER_MEDIATS_ASC refers to MegaApiJava.ORDER_MEDIATS_ASC
     *
     * Sorts by media capture timestamp, oldest first. Only meaningful for media nodes — the
     * timestamp is 0 for everything else.
     */
    ORDER_MEDIATS_ASC,

    /**
     * Order ORDER_MEDIATS_DESC refers to MegaApiJava.ORDER_MEDIATS_DESC
     *
     * Sorts by media capture timestamp, newest first. Only meaningful for media nodes — the
     * timestamp is 0 for everything else.
     */
    ORDER_MEDIATS_DESC
}
