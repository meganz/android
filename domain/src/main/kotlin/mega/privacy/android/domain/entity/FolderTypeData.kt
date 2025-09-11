package mega.privacy.android.domain.entity

import mega.privacy.android.domain.entity.node.NodeId

/**
 * Data class that holds all the information needed to determine folder types during mapping
 *
 * @property primarySyncHandle Primary camera uploads sync handle
 * @property secondarySyncHandle Secondary camera uploads sync handle
 * @property chatFilesFolderId Chat files folder ID
 * @property backupFolderId Root backup folder ID
 * @property syncedNodeIds Set of node IDs that are synced
 */
data class FolderTypeData(
    val primarySyncHandle: Long?,
    val secondarySyncHandle: Long?,
    val chatFilesFolderId: NodeId?,
    val backupFolderId: NodeId?,
    val syncedNodeIds: Set<NodeId>,
)
