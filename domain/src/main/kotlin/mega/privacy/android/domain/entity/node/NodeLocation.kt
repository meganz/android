package mega.privacy.android.domain.entity.node

/**
 * Data class storing relevant data for a node location.
 *
 * @property node The [Node] in question.
 * @property nodeSourceType Where the node is. It can be [NodeSourceType.CLOUD_DRIVE], [NodeSourceType.RUBBISH_BIN] or [NodeSourceType.INCOMING_SHARES]
 * @property ancestorIds List of [NodeId] containing each of the parent nodes in the tree.
 */
data class NodeLocation(
    val node: Node,
    val nodeSourceType: NodeSourceType,
    val ancestorIds: List<NodeId>,
)