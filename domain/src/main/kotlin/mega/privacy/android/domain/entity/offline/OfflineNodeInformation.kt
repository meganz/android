package mega.privacy.android.domain.entity.offline

/**
 * Offline node information
 *
 */
sealed interface OfflineNodeInformation {
    /**
     * Path
     */
    val path: String
}

/**
 * Other offline node information
 *
 * @property path
 */
data class OtherOfflineNodeInformation(
    override val path: String,
) : OfflineNodeInformation

/**
 * Inbox offline node information
 *
 * @property path
 */
data class InboxOfflineNodeInformation(
    override val path: String,
) : OfflineNodeInformation

/**
 * Incoming share offline node information
 *
 * @property path
 * @property incomingHandle
 */
data class IncomingShareOfflineNodeInformation(
    override val path: String,
    val incomingHandle: String,
) : OfflineNodeInformation