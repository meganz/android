package mega.privacy.android.domain.entity.node

/**
 * Enum class containing all Node Source types available
 */
enum class NodeSourceType {

    /**
     * When node source is the home page
     */
    HOME,

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
     * When node source is inside Favourites section
     */
    FAVOURITES,

    /**
     * When node source is inside Docs section
     */
    DOCUMENTS,

    /**
     * When node source is inside Audio section
     */
    AUDIO,

    /**
     * When node source is other tabs
     */
    OTHER,

    /**
     * When node source is offline
     */
    OFFLINE,

    /**
     * When node source is Videos tab
     */
    VIDEOS,

    /**
     * When node source is Search results
     */
    SEARCH,

    /**
     * When node source is Video Playlists
     */
    VIDEO_PLAYLISTS,

    /**
     * When node source is Recents Bucket
     */
    RECENTS_BUCKET,
}

/**
 * Checks if the [NodeSourceType] represents a shared item.
 *
 * Shared items include incoming shares, outgoing shares, and links.
 *
 * @return true if the node source is INCOMING_SHARES, OUTGOING_SHARES, or LINKS; false otherwise.
 */
fun NodeSourceType.isSharedSource(): Boolean = when (this) {
    NodeSourceType.INCOMING_SHARES, NodeSourceType.OUTGOING_SHARES, NodeSourceType.LINKS -> true
    else -> false
}
