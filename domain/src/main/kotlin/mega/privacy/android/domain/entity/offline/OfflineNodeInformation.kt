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

    /**
     * last modified time
     */
    val lastModifiedTime: Long?
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
    override val lastModifiedTime: Long?
) : OfflineNodeInformation

/**
 * Backups offline node information
 *
 * @property path
 * @property name
 */
data class BackupsOfflineNodeInformation(
    override val path: String,
    override val name: String,
    override val handle: String,
    override val isFolder: Boolean,
    override val lastModifiedTime: Long?
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
    override val lastModifiedTime: Long?,
    val incomingHandle: String,
) : OfflineNodeInformation