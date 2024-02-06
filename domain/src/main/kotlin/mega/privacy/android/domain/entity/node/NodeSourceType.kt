package mega.privacy.android.domain.entity.node

/**
 * Enum class containing all Node Source types available
 */
enum class NodeSourceType {

    /**
     * When node source is the cloud drive
     */
    CLOUD_DRIVE,

    /**
     * When node source is the shared links
     */
    LINKS,

    /**
     * When node source is inside rubbish bin
     */
    RUBBISH_BIN,

    /**
     * When node source is inside back ups
     */
    BACKUPS,

    /**
     * When node source is inside outgoing shares
     */
    OUTGOING_SHARES,

    /**
     * When node source is inside incoming shares
     */
    INCOMING_SHARES,

    /**
     * When node source is other tabs
     */
    OTHER,
}