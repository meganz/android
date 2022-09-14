package mega.privacy.android.domain.entity

/**
 * Sort Order
 */
enum class SortOrder(
    /**
     * sort order value
     */
    val value: Int,
) {
    /**
     * Order NONE refers to MegaApiJava.ORDER_NONE
     */
    ORDER_NONE(0),

    /**
     * Order ORDER_DEFAULT_ASC refers to MegaApiJava.ORDER_DEFAULT_ASC
     */
    ORDER_DEFAULT_ASC(1),

    /**
     * Order ORDER_DEFAULT_DESC refers to MegaApiJava.ORDER_DEFAULT_DESC
     */
    ORDER_DEFAULT_DESC(2),

    /**
     * Order ORDER_SIZE_ASC refers to MegaApiJava.ORDER_SIZE_ASC
     */
    ORDER_SIZE_ASC(3),

    /**
     * Order ORDER_SIZE_DESC refers to MegaApiJava.ORDER_SIZE_DESC
     */
    ORDER_SIZE_DESC(4),

    /**
     * Order ORDER_CREATION_ASC refers to MegaApiJava.ORDER_CREATION_ASC
     */
    ORDER_CREATION_ASC(5),

    /**
     * Order ORDER_CREATION_DESC refers to MegaApiJava.ORDER_CREATION_DESC
     */
    ORDER_CREATION_DESC(6),

    /**
     * Order ORDER_MODIFICATION_ASC refers to MegaApiJava.ORDER_MODIFICATION_ASC
     */
    ORDER_MODIFICATION_ASC(7),

    /**
     * Order ORDER_MODIFICATION_DESC refers to MegaApiJava.ORDER_MODIFICATION_DESC
     */
    ORDER_MODIFICATION_DESC(8),

    /**
     * Order ORDER_ALPHABETICAL_ASC refers to MegaApiJava.ORDER_ALPHABETICAL_ASC
     */
    ORDER_ALPHABETICAL_ASC(9),

    /**
     * Order ORDER_ALPHABETICAL_DESC refers to MegaApiJava.ORDER_ALPHABETICAL_DESC
     */
    ORDER_ALPHABETICAL_DESC(10),

    /**
     * Order ORDER_PHOTO_ASC refers to MegaApiJava.ORDER_PHOTO_ASC
     */
    ORDER_PHOTO_ASC(11),

    /**
     * Order ORDER_PHOTO_DESC refers to MegaApiJava.ORDER_PHOTO_DESC
     */
    ORDER_PHOTO_DESC(12),

    /**
     * Order ORDER_VIDEO_ASC refers to MegaApiJava.ORDER_VIDEO_ASC
     */
    ORDER_VIDEO_ASC(13),

    /**
     * Order ORDER_VIDEO_DESC refers to MegaApiJava.ORDER_VIDEO_DESC
     */
    ORDER_VIDEO_DESC(14),

    /**
     * Order ORDER_LINK_CREATION_ASC refers to MegaApiJava.ORDER_LINK_CREATION_ASC
     */
    ORDER_LINK_CREATION_ASC(15),

    /**
     * Order ORDER_LINK_CREATION_DESC refers to MegaApiJava.ORDER_LINK_CREATION_DESC
     */
    ORDER_LINK_CREATION_DESC(16),

    /**
     * Order ORDER_LABEL_ASC refers to MegaApiJava.ORDER_LABEL_ASC
     */
    ORDER_LABEL_ASC(17),

    /**
     * Order ORDER_LABEL_DESC refers to MegaApiJava.ORDER_LABEL_DESC
     */
    ORDER_LABEL_DESC(18),

    /**
     * Order ORDER_FAV_ASC refers to MegaApiJava.ORDER_FAV_ASC
     */
    ORDER_FAV_ASC(19),

    /**
     * Order ORDER_FAV_DESC refers to MegaApiJava.ORDER_FAV_DESC
     */
    ORDER_FAV_DESC(20)
}
