package mega.privacy.android.domain.entity.node.publiclink

import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType

/**
 * Node name collision result
 *
 * @property noConflictNodes
 * @property conflictNodes
 * @property type
 */
data class PublicNodeNameCollisionResult(
    val noConflictNodes: List<Node>,
    val conflictNodes: List<NodeNameCollision>,
    val type: NodeNameCollisionType,
)