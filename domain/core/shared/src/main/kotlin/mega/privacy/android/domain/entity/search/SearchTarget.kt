package mega.privacy.android.domain.entity.search

/**
 * Enum class representing search target
 */
enum class SearchTarget {
    /**
     * INCOMING_SHARE
     */
    INCOMING_SHARE,

    /**
     * OUTGOING_SHARE
     */
    OUTGOING_SHARE,

    /**
     * LINKS_SHARE
     */
    LINKS_SHARE,

    /**
     * CLOUD DRIVE
     * search under Cloud and Vault root nodes
     */
    ROOT_NODES,

    /**
     * ALL searches every where
     */
    ALL
}