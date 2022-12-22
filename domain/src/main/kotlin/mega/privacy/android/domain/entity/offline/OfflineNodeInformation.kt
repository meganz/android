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
    val incomingHandle: String,
) : OfflineNodeInformation