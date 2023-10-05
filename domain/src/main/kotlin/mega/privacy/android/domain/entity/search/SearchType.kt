package mega.privacy.android.domain.entity.search

/**
 * Enum class containing all search types available
 */
enum class SearchType {

    /**
     * When searching the cloud drive
     */
    CLOUD_DRIVE,

    /**
     * When searching the shared links
     */
    LINKS,

    /**
     * When searching inside rubbish bin
     */
    RUBBISH_BIN,

    /**
     * When searching inside back ups
     */
    BACKUPS,

    /**
     * When searching inside outgoing shares
     */
    OUTGOING_SHARES,

    /**
     * When searching inside incoming shares
     */
    INCOMING_SHARES,

    /**
     * When searching other tabs
     */
    OTHER,
}