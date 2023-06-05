package mega.privacy.android.domain.entity.offline

/**
 * Offline node information
 */
sealed interface OfflineNodeInformation {
    /**
     * Path
     */
    val path: String

    /**
     * Name
     */
    val name: String

    /**
     * Handle
     */
    val handle: String

    /**
     * Is folder
     */
    val isFolder: Boolean
}

/**
 * Other offline node information
 *
 * @property path
 * @property name
 */
data class OtherOfflineNodeInformation(
    override val path: String,
    override val name: String,
    override val handle: String,
    override val isFolder: Boolean,
) : OfflineNodeInformation

/**
 * Inbox offline node information
 *
 * @property path
 * @property name
 */
data class InboxOfflineNodeInformation(
    override val path: String,
    override val name: String,
    override val handle: String,
    override val isFolder: Boolean,
) : OfflineNodeInformation

/**
 * Incoming share offline node information
 *
 * @property path
 * @property name
 * @property incomingHandle
 */
data class IncomingShareOfflineNodeInformation(
    override val path: String,
    override val name: String,
    override val handle: String,
    override val isFolder: Boolean,
    val incomingHandle: String,
) : OfflineNodeInformation