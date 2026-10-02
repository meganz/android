package mega.privacy.android.domain.entity.zipbrowser

import mega.privacy.android.domain.entity.node.NodeId

/**
 * A zip in the cloud that is browsed by reading only the parts of it that are needed.
 *
 * @property isFolderLinkNode the node belongs to a folder link, which is read through the folder
 * link's own api instance
 * @property modificationTime identifies the version, so a replaced file is not served from the
 * parts read from the one it replaced
 */
data class RemoteZipArchiveSource(
    val nodeId: NodeId,
    val isFolderLinkNode: Boolean,
    val name: String,
    val size: Long,
    val modificationTime: Long,
)
