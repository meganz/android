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
    val incomingHandle: String,
) : OfflineNodeInformation