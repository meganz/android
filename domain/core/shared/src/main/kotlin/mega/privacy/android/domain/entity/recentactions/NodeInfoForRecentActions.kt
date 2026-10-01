package mega.privacy.android.domain.entity.recentactions

import mega.privacy.android.domain.entity.node.NodeId

/**
 * Data class to hold the info of a node required for recent action
 * @property id NodeId of the node
 * @property name Name of the node
 * @property parentId NodeId of the parent node
 * @property isFolder Boolean to indicate if the node is a folder
 * @property isIncomingShare Boolean to indicate if the node is in incoming share
 * @property isOutgoingShare Boolean to indicate if the node is in outgoing share
 * @property isPendingShare Boolean to indicate if the node is in pending share
 */
data class NodeInfoForRecentActions(
    val id: NodeId,
    val name: String,
    val parentId: NodeId,
    val isFolder: Boolean,
    val isIncomingShare: Boolean,
    val isOutgoingShare: Boolean,
    val isPendingShare: Boolean,
)